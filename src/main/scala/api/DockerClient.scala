package api

import api.DockerClientConfig.APIRequired._
import model._
import model.stats.ContainerStats
import org.scalajs.dom.ext.{Ajax, AjaxException}
import org.scalajs.dom.raw._
import upickle.default._
import util.CurrentDockerApiVersion.VersionBroadcaster
import util.EventsCustomParser.DockerEventStream
import util.PullEventsCustomParser.{EventStatus, EventStream}
import util.googleAnalytics._
import util.logger._
import util.{CurrentDockerApiVersion, EventsCustomParser, FutureUtils, StatsCustomParser}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.scalajs.js

object DockerClientConfig {
  val HttpTimeOut = 10 * 1000
  val HttpExternalTimeOut = 20 * 1000
  val PingTimeOut = 4 * 1000
  val DockerVersion = s"v$Mayor.$Minor"
  val DockerOptionalVersion = s"v${APIOptional.Mayor}.${APIOptional.Minor}"

  object APIRequired {
    val Mayor = 1
    val Minor = 17
  }

  object APIOptional {
    val Mayor = 1
    val Minor = 19
  }

  val KeepInGarbageCollection = 10
}

case class DockerClient(connection: Connection) {

  import DockerClientConfig._

  val url = connection.url + "/" + DockerVersion
  val urlOptional = connection.url + "/" + DockerOptionalVersion

  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#ping-the-docker-server
  def ping(): Future[Unit] =
    Ajax.get(url = s"$url/_ping", timeout = PingTimeOut).map { xhr =>
      log.info("[dockerClient.ping] return: " + xhr.responseText)
    }


  def metadata(): Future[DockerMetadata] = for {
    test <- ping()
    info <- info()
    version <- version()
  } yield DockerMetadata(connection, info, version, containers = Seq.empty)

  def containersRunningWithExtraInfo(): Future[Seq[Container]] =
    containers(all = false, extraInfo = true)


  def containerRunning(): Future[Seq[Container]] =
    containers(all = false, extraInfo = false)

  def containersHistory(running: Seq[Container]): Future[Seq[Container]] = for {
    all <- containers(all = true, extraInfo = false)
  } yield all.diff(running)


  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#inspect-a-container
  def containerInfo(containerId: String): Future[ContainerInfo] =
    Ajax.get(s"$url/containers/$containerId/json", timeout = HttpTimeOut).map { xhr =>
      log.debug("[dockerClient.containerInfo]")
      read[ContainerInfo](xhr.responseText)
    }


  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#stop-a-container
  def stopContainer(containerId: String): Future[ContainerInfo] =
    Ajax.post(s"$url/containers/$containerId/stop?t=3", timeout = HttpTimeOut)
      .flatMap(_ => containerInfo(containerId))


  def startContainer(containerId: String): Future[ContainerInfo] = {
    Ajax.post(s"$url/containers/$containerId/start", timeout = HttpTimeOut).flatMap(_ => containerInfo(containerId))
  }.recover {
    case ex: AjaxException =>
      throw new Exception(ex.xhr.responseText)
  }

  def removeContainer(containerId: String): Future[Unit] =
    Ajax.delete(s"$url/containers/$containerId", timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.removeContainer] return: " + xhr.readyState)
    }.recover {
      case ex: AjaxException =>
        log.info(s"Unable to delete $containerId} ${ex.xhr.responseText}")
    }

  private def removeImage(imageId: String): Future[Unit] = {
    Ajax.delete(s"$url/images/$imageId", timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.removeImage] return: " + xhr.readyState)
    }.transform(identity, ex => ex match {
      case ex: AjaxException =>
        log.info(s"Unable to delete $imageId} ${ex.xhr.responseText}")
        new Exception(ex.xhr.responseText)
    })
  }

  def removeImage(image: Image): Future[Unit] = {
    val imageId = image.Id
    val imagesToDelete = image.RepoTags.drop(1) :+ imageId
    val tasks = imagesToDelete.map(imageId => () => removeImage(imageId)).toList
    FutureUtils.sequenceWithDelay(tasks, FutureUtils.LongDelay, ignoreErrors = false).map(_ => ())
  }

  def garbageCollectionImages(status: String => Unit): Future[Seq[Image]] = {
    if (!CurrentDockerApiVersion.checkSupportGC()) {
      Future.failed(new Exception(s"Unsupported operation in API ${CurrentDockerApiVersion.currentVersion}. API version 1.23+ required"))
    } else {
      log.info("Staring garbageCollectionImages")


      sendEvent(EventCategory.Image, EventAction.GC)

      status(s"Fetching active containers")
      val usedImagesId: Future[Seq[String]] = for {
        containers <- containers(all = true, extraInfo = false)
      } yield containers.map(_.ImageID)


      // process the tree removing any used Image and its parents
      def filterUsed(images: Seq[Image], imageId: String): Seq[Image] = {
        val imageFound = images.filter(_.Id == imageId)
        val remainingImages = images.diff(imageFound)
        imageFound.headOption match {
          case None => remainingImages
          case Some(image) =>
            // recursively remove parentImages
            filterUsed(remainingImages, image.ParentId)
        }
      }


      val imagesToGC = for {
        all <- images(all = true)
        usedIds <- usedImagesId
      } yield {
        status(s"Searching for images to GC")
        log.info(s"Calculating imagesToGC all: ${all.size}, usedIds: ${usedIds.size} ")
        usedIds.foldLeft(all) {
          case (remaining, imageId) =>
            filterUsed(remaining, imageId)
        }
      }

      // fetch the parents for every image to GC
      val imagesWithParents = imagesToGC.flatMap { unorderedImages =>
        status(s"Images to GC: ${unorderedImages.size}")
        log.info(s"imagesToGC: ${unorderedImages.size} ")
        val imagesWithParents = unorderedImages.map(image => () => Future.successful(ImageWithParents(image, getImageParents(unorderedImages, image)))).toList
        FutureUtils.sequenceWithDelay(imagesWithParents, FutureUtils.SmallDelay, ignoreErrors = false)
      }

      // order based on parents
      val orderedImages: Future[List[Image]] = imagesWithParents.map { unorderedImages =>
        //  status(s"Images to GC: ${unorderedImages.size}")
        //log.info(s"imagesToGC: ${unorderedImages.size} ")
        log.info(s"Ordering images before GC...")
        // First need to order from top to button

        val imagesOrdered = unorderedImages.sortWith(compareImages).map(_.image)

        status(s"Images Ordered and ready to GC: ${unorderedImages.size}")
        log.info("images ordered:")
        imagesOrdered
      }

      val tasksFut = orderedImages.map { imagesToGC =>
        val tasks = imagesToGC.map { image => () => {
          status(s"Removing ${image.id}/ ${image.RepoTags.mkString(" ")}")
          removeImage(image)
        }

        }
        FutureUtils.sequenceWithDelay(tasks, FutureUtils.LongDelay, ignoreErrors = false)
      }.flatMap(identity)

      tasksFut.flatMap(_ => images())

    }
  }

  case class ImageWithParents(image: Image, parents: List[String])
  def getImageParents(all: Seq[Image], image: Image): List[String] = {
    all.find(_.Id == image.ParentId) match {
      case None => image.Id :: Nil
      case Some(parentImage) => image.Id :: getImageParents(all, parentImage)
    }
  }

  // an image is before another if there is no relation ship between them
  def compareImages(image1: ImageWithParents, image2: ImageWithParents) = {
    !image2.parents.exists(_ == image1.image.Id)
  }


  def garbageCollectionContainers(): Future[Unit] = {
    sendEvent(EventCategory.Container, EventAction.GC)
    containers(all = true, extraInfo = false).flatMap { all =>
      def delete(containers: List[Container]): Future[Unit] = containers match {
        case head :: tail => removeContainer(head.Id).andThen { case _ => delete(tail) }
        case Nil => Future.successful(())
      }
      delete(all.drop(KeepInGarbageCollection).toList)
    }
  }

  def top(containerId: String): Future[ContainerTop] =
    Ajax.get(s"$url/containers/$containerId/top", timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.top] return: " + xhr.responseText)
      read[ContainerTop](xhr.responseText)
    }

  //https://docs.docker.com/reference/api/docker_remote_api_v1.17/#list-images
  def images(all: Boolean = false): Future[Seq[Image]] =
    Ajax.get(s"$url/images/json?all=$all", timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.images] ")
      read[Seq[Image]](xhr.responseText)
    }.map {
      _.map(img => Option(img.RepoTags) match {
        case Some(_) => img
        case None => img.copy(RepoTags = Seq.empty)
      }).filter(all || !_.RepoTags.contains("<none>:<none>")).sortBy(-_.Created)
    }


  def imageInfo(imageId: String): Future[ImageInfo] =
    Ajax.get(s"$url/images/$imageId/json", timeout = HttpTimeOut).map { xhr =>
      log.debug("[dockerClient.imageInfo] ")
      read[ImageInfo](xhr.responseText)
    }

  def imageHistory(imageId: String): Future[Seq[ImageHistory]] =
    Ajax.get(s"$url/images/$imageId/history", timeout = HttpTimeOut).map { xhr =>
      log.debug("[dockerClient.imageHistory]")
      read[Seq[ImageHistory]](xhr.responseText)
    }

  def imagesSearch(term: String): Future[Seq[ImageSearch]] =
    Ajax.get(s"$url/images/search?term=${term.toLowerCase}", timeout = HttpExternalTimeOut).map { xhr =>
      log.info("[dockerClient.imagesSearch]")
      read[Seq[ImageSearch]](xhr.responseText)
    }

  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#list-containers
  // Note: gather extraInfo is slower (ex: SizeRootFs, SizeRw).
  private def containers(all: Boolean, extraInfo: Boolean): Future[Seq[Container]] =
    Ajax.get(s"$url/containers/json?all=$all&size=$extraInfo", timeout = HttpTimeOut).map { xhr =>
      log.info(s"[dockerClient.containers]")
      read[Seq[Container]](xhr.responseText)
    }

  //https://docs.docker.com/reference/api/docker_remote_api_v1.17/#display-system-wide-information
  def info(): Future[Info] =
    Ajax.get(s"$url/info").map { xhr =>
      read[Info](xhr.responseText)
    }

  def checkVersion(): Future[Boolean] = version().map(_.apiVersion).map {
    case (mayor, _) if (mayor > Mayor) => true
    case (mayor, minor) if (mayor == Mayor && minor >= Minor) => true
    case _ => false
  }

  private def version(): Future[Version] =
    Ajax.get(s"${connection.url}/version", timeout = PingTimeOut).map { xhr =>
      log.info("[dockerClient.version] return: " + xhr.responseText)
      val version = read[Version](xhr.responseText)
      VersionBroadcaster.publishVersion(version)
      version
    }

  def attachToContainer(containerId: String): WebSocket = {
    import util.StringUtils._
    val schema = if (connection.url.startsWith("http://")) "ws://" else "wss://"
    val ws = schema + substringAfter(s"$url/containers/$containerId/attach/ws?logs=1&stderr=1&stdout=1&stream=1&stdin=1", "://")
    log.info(s"[dockerClient.attach] url: $ws")
    new WebSocket(ws)
  }

  def pullImage(term: String): EventStream = {
    val Loading = 3
    val Done = 4
    val p = Promise[Seq[EventStatus]]
    val xhr = new XMLHttpRequest()

    val currentStream = EventStream()
    xhr.onreadystatechange = { event: Event =>
      currentStream.data = xhr.responseText
      if (xhr.readyState == Done) {
        currentStream.done = true
      }
    }

    xhr.open("POST", s"$url/images/create?fromImage=$term", async = true)
    log.info(s"[dockerClient.pullImage] start")
    xhr.send()
    currentStream
  }

  def createContainer(name: String, request: CreateContainerRequest): Future[CreateContainerResponse] =
    Ajax.post(s"$url/containers/create?name=$name",
      write(request),
      headers = Map("Content-Type" -> "application/json"),
      timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.version] return: " + xhr.responseText)
      read[CreateContainerResponse](xhr.responseText)
    }

  def events(update: Seq[DockerEvent] => Unit): ConnectedStream = {
    log.info("[dockerClient.events] start")
    val since = {
      val t = new js.Date()
      t.setDate(t.getDate() - 3) // pull 3 days
      t.getTime() / 1000
    }.toLong

    val Loading = 3
    val Done = 4
    val xhr = new XMLHttpRequest()

    val currentStream = DockerEventStream()
    xhr.onreadystatechange = { event: Event =>
      EventsCustomParser.parse(currentStream, xhr.responseText)
      if (xhr.readyState == Loading) {
        update(currentStream.events)
      } else if (xhr.readyState == Done) {
        update(currentStream.events)
      }
    }

    val eventsUrl = s"$url/events?since=$since"
    xhr.open("GET", eventsUrl, async = true)
    log.info(s"[dockerClient.events] start:  $eventsUrl")
    xhr.send()

    new ConnectedStream {
      override def abort(): Unit = xhr.abort()
    }
  }

  def containerChanges(containerId: String): Future[Seq[FileSystemChange]] = {
    Ajax.get(s"$url/containers/$containerId/changes", timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.containerChanges]")
      read[Seq[FileSystemChange]](xhr.responseText)
    }
  }

  // https://docs.docker.com/engine/reference/api/docker_remote_api_v1.19/#get-container-stats-based-on-resource-usage
  def containerStats(containerId: String)(updateUI: (Option[ContainerStats], ConnectedStream) => Unit): Unit = {
    val xhr = new XMLHttpRequest()
    val stream = new ConnectedStream {
      override def abort(): Unit = {
        xhr.abort()
      }
    }
    xhr.onreadystatechange = { _: Event =>
      log.debug("container stats update")
      val stats = StatsCustomParser.parse(xhr.responseText)
      updateUI(stats, stream)
    }

    val statsUrl = s"$url/containers/$containerId/stats"
    xhr.open("GET", statsUrl, async = true)
    log.info(s"[dockerClient.containerStats] $statsUrl")
    xhr.send()
  }

}

trait ConnectedStream {
  def abort()
}

