package api

import api.DockerClientConfig.APIRequired._
import model._
import model.stats.ContainerStats
import org.scalajs.dom.ext.Ajax.InputData
import org.scalajs.dom.ext.{Ajax, AjaxException}
import org.scalajs.dom.raw._
import upickle.default._
import util.CurrentDockerApiVersion.VersionBroadcaster
import util.EventsCustomParser.DockerEventStream
import util.PullEventsCustomParser.{EventStatus, EventStream}
import util._
import util.logger._

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

trait DockerConnection {
  val connection: Connection

  import DockerClientConfig._

  def info: String

  def get(path: String, timeout: Int = HttpTimeOut): Future[Response]

  def post(path: String, data: Option[InputData] = None, headers: Map[String, String] = Map.empty, timeout: Int = HttpTimeOut): Future[Response]

  def delete(path: String, timeout: Int = HttpTimeOut): Future[Response]

  def attachToContainer(containerId: String): Future[BasicWebSocket]

  def pullImage(term: String): EventStream

  def events(update: Seq[DockerEvent] => Unit): ConnectedStream

  def containerStats(containerId: String)(updateUI: (Option[ContainerStats], ConnectedStream) => Unit): Unit
}

case class Response(responseText: String, statusCode: Int)


case class DockerClient(con: DockerConnection) {

  import DockerClientConfig._


  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#ping-the-docker-server
  def ping(): Future[Unit] = con.get(path = "/_ping", timeout = PingTimeOut)
    .map { xhr =>
      log.info(s"[dockerClient.ping] return: ${xhr.statusCode} ${xhr.responseText}")
    }


  def metadata(): Future[DockerMetadata] = for {
    test <- ping()
    info <- info()
    version <- version()
  } yield DockerMetadata(con.info, info, version, containers = Seq.empty)

  def containersRunningWithExtraInfo(): Future[Seq[Container]] =
    containers(all = false, extraInfo = true)


  def containerRunning(): Future[Seq[Container]] =
    containers(all = false, extraInfo = false)

  def containersHistory(running: Seq[Container]): Future[Seq[Container]] = for {
    all <- containers(all = true, extraInfo = false)
  } yield all.diff(running)


  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#inspect-a-container
  def containerInfo(containerId: String): Future[ContainerInfo] = {
    con.get(path = s"/containers/$containerId/json", timeout = HttpTimeOut)
      .map { xhr =>
        log.debug("[dockerClient.containerInfo]")
        read[ContainerInfo](xhr.responseText)
      }
  }


  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#stop-a-container
  def stopContainer(containerId: String): Future[ContainerInfo] = {
    con.post(path = s"/containers/$containerId/stop?t=3")
      .flatMap(_ => containerInfo(containerId))
  }

  def startContainer(containerId: String): Future[ContainerInfo] = {
    con.post(path = s"/containers/$containerId/start").flatMap(_ => containerInfo(containerId))
  }.recover {
    case ex: AjaxException =>
      throw new Exception(ex.xhr.responseText)
  }

  def removeContainer(containerId: String): Future[Unit] = {
    con.delete(path = s"/containers/$containerId").map { xhr =>
      log.info("[dockerClient.removeContainer] return: " + xhr.statusCode)
    }.recover {
      case ex: AjaxException =>
        log.info(s"Unable to delete $containerId} ${ex.xhr.responseText}")
    }
  }

  private def removeImage(imageId: String): Future[Unit] = {
    con.delete(path = s"/images/$imageId").map { xhr =>
      log.info("[dockerClient.removeImage] return: " + xhr.statusCode)
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


      PlatformService.current.sendEvent(EventCategory.Image, EventAction.GC)

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
    PlatformService.current.sendEvent(EventCategory.Container, EventAction.GC)
    containers(all = true, extraInfo = false).flatMap { all =>
      def delete(containers: List[Container]): Future[Unit] = containers match {
        case head :: tail => removeContainer(head.Id).andThen { case _ => delete(tail) }
        case Nil => Future.successful(())
      }
      delete(all.drop(KeepInGarbageCollection).toList)
    }
  }

  def top(containerId: String): Future[ContainerTop] =
    con.get(path = s"/containers/$containerId/top").map { xhr =>
      log.info("[dockerClient.top] return: " + xhr.responseText)
      read[ContainerTop](xhr.responseText)
    }

  //https://docs.docker.com/reference/api/docker_remote_api_v1.17/#list-images
  def images(all: Boolean = false): Future[Seq[Image]] = {
    con.get(path = s"/images/json?all=$all").map { xhr =>
      log.info("[dockerClient.images] ")
      read[Seq[Image]](xhr.responseText)
    }.map {
      _.map(img => Option(img.RepoTags) match {
        case Some(_) => img
        case None => img.copy(RepoTags = Seq.empty)
      }).filter(all || !_.RepoTags.contains("<none>:<none>")).sortBy(-_.Created)
    }
  }

  def imageInfo(imageId: String): Future[ImageInfo] =
    con.get(path = s"/images/$imageId/json").map { xhr =>
      log.debug("[dockerClient.imageInfo] ")
      read[ImageInfo](xhr.responseText)
    }

  def imageHistory(imageId: String): Future[Seq[ImageHistory]] =
    con.get(path = s"/images/$imageId/history").map { xhr =>
      log.debug("[dockerClient.imageHistory]")
      read[Seq[ImageHistory]](xhr.responseText)
    }

  def imagesSearch(term: String): Future[Seq[ImageSearch]] = {
    con.get(path = s"/images/search?term=${term.toLowerCase}").map { xhr =>
      log.info("[dockerClient.imagesSearch]")
      read[Seq[ImageSearch]](xhr.responseText)
    }
  }

  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#list-containers
  // Note: gather extraInfo is slower (ex: SizeRootFs, SizeRw).
  private def containers(all: Boolean, extraInfo: Boolean): Future[Seq[Container]] = {
    con.get(path = s"/containers/json?all=$all&size=$extraInfo").map { xhr =>
      log.info(s"[dockerClient.containers]")
      read[Seq[Container]](xhr.responseText)
    }
  }

  //https://docs.docker.com/reference/api/docker_remote_api_v1.17/#display-system-wide-information
  def info(): Future[Info] = {
    con.get(path = s"/info").map { xhr =>
      read[Info](xhr.responseText)
    }
  }

  def checkVersion(): Future[Boolean] = version().map(_.apiVersion).map {
    case (mayor, _) if (mayor > Mayor) => true
    case (mayor, minor) if (mayor == Mayor && minor >= Minor) => true
    case _ => false
  }

  private def version(): Future[Version] = {
    con.get(path = s"/version", timeout = PingTimeOut).map { xhr =>
      log.info("[dockerClient.version] return: " + xhr.responseText)
      val version = read[Version](xhr.responseText)
      VersionBroadcaster.publishVersion(version)
      version
    }
  }

  def attachToContainer(containerId: String): Future[BasicWebSocket] = con.attachToContainer(containerId)

  def pullImage(term: String): EventStream = con.pullImage(term)

  def createContainer(name: String, request: CreateContainerRequest): Future[CreateContainerResponse] = {
    con.post(path = s"/containers/create?name=$name&",
      data = Some(write(request)),
      headers = Map("Content-Type" -> "application/json")
    ).map { xhr =>
      log.info("[dockerClient.version] return: " + xhr.responseText)
      read[CreateContainerResponse](xhr.responseText)
    }
  }

  def events(update: Seq[DockerEvent] => Unit): ConnectedStream = con.events(update)

  def containerChanges(containerId: String): Future[Seq[FileSystemChange]] = {
    con.get(path = s"/containers/$containerId/changes").map { xhr =>
      log.info("[dockerClient.containerChanges]")
      Option(read[Seq[FileSystemChange]](xhr.responseText)).getOrElse(Seq.empty)
    }
  }

  // https://docs.docker.com/engine/reference/api/docker_remote_api_v1.19/#get-container-stats-based-on-resource-usage
  def containerStats(containerId: String)(updateUI: (Option[ContainerStats], ConnectedStream) => Unit): Unit =
  con.containerStats(containerId)(updateUI)
}

trait ConnectedStream {
  def abort()
}

