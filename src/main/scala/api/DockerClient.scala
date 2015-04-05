package api

import model._
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw._
import upickle._
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DockerClient(connection: Connection) {

  val HttpTimeOut = 10 * 1000
  // 10 seconds
  val PingTimeOut = 2 * 1000

  val DockerVersion = "v1.17"
  val url = connection.url + "/" + DockerVersion

  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#ping-the-docker-server
  def ping(): Future[Unit] =
    Ajax.get(url = s"$url/_ping", timeout = PingTimeOut).map { xhr =>
      log.info("[dockerClient.ping] return: " + xhr.responseText)
    }


  def metadata(): Future[DockerMetadata] = for {
    info <- info()
    version <- version()
    containers <- containers(all = false)
  } yield DockerMetadata(connection, info, version, containers)


  def containersInfo(): Future[ContainersInfo] = for {
    r <- containers(all = false)
    all <- containers(all = true)
  } yield ContainersInfo(r, all)


  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#inspect-a-container
  def containerInfo(containerId: String): Future[ContainerInfo] =
    Ajax.get(s"$url/containers/$containerId/json", timeout = HttpTimeOut).map { xhr =>
      log.debug("[dockerClient.containerInfo] return: " + xhr.responseText)
      read[ContainerInfo](xhr.responseText)
    }


  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#stop-a-container
  def stopContainer(containerId: String): Future[ContainerInfo] =
    Ajax.post(s"$url/containers/$containerId/stop?t=3", timeout = HttpTimeOut)
      .flatMap(_ => containerInfo(containerId))


  def startContainer(containerId: String): Future[ContainerInfo] = {
    Ajax.post(s"$url/containers/$containerId/start", timeout = HttpTimeOut).flatMap(_ => containerInfo(containerId))
  }

  def top(containerId: String): Future[ContainerTop] =
    Ajax.get(s"$url/containers/$containerId/top", timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.top] return: " + xhr.responseText)
      read[ContainerTop](xhr.responseText)
    }

  //https://docs.docker.com/reference/api/docker_remote_api_v1.17/#list-images
  def images(): Future[Seq[Image]] =
    Ajax.get(s"$url/images/json", timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.images] return: " + xhr.responseText)
      read[Seq[Image]](xhr.responseText)
    }


  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#list-containers
  private def containers(all: Boolean): Future[Seq[Container]] =
    Ajax.get(s"$url/containers/json?all=$all", timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.containers] return: " + xhr.responseText)
      read[Seq[Container]](xhr.responseText)
    }


  private def info(): Future[Info] =
    Ajax.get(s"$url/info", timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.info] return: " + xhr.responseText)
      read[Info](xhr.responseText)
    }


  private def version(): Future[Version] =
    Ajax.get(s"$url/version", timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.version] return: " + xhr.responseText)
      read[Version](xhr.responseText)
    }

  def attachToContainer(containerId: String): WebSocket = {
    import util.stringUtils._
    val schema = if (connection.url.startsWith("http://")) "ws://" else "wss://"
    val ws = schema + substringAfter(s"$url/containers/$containerId/attach/ws?logs=1&stderr=1&stdout=1&stream=1&stdin=1", "://")
    log.info(s"[dockerClient.attach] url: $ws")
    new WebSocket(ws)
  }

}
