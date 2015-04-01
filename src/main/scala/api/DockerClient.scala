package api

import model._
import org.scalajs.dom.ext.Ajax
import upickle._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Promise, Future}
import util.logger._

case class DockerClient(connection: Connection) {

  val HttpTimeOut = 3 * 1000 // 3 seconds

  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#ping-the-docker-server
  def ping(): Future[Unit] = {
    val url = connection.url + "/_ping"
    Ajax.get(url = url, timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.ping] return: " + xhr.responseText)
    }
  }

  def metadata(): Future[DockerMetadata] = {
    for {
      info <- info()
      version <- version()
      containers <- containers(all = false)
    } yield DockerMetadata(connection, info, version, containers)
  }

  def containersInfo(): Future[ContainersInfo] = for {
    r <- containers(all = false)
    all <- containers(all = true)
  } yield ContainersInfo(r, all)


  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#inspect-a-container
  def containerInfo(containerId: String): Future[ContainerInfo] = {
    val url = connection.url + "/containers/" + containerId + "/json"
    Ajax.get(url = url, timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.containerInfo] return: " + xhr.responseText)
      read[ContainerInfo](xhr.responseText)
    }
  }

    }
  }

  def top(containerId: String): Future[ContainerTop] = {
    val url = connection.url + "/containers/" + containerId + "/top"
    Ajax.get(url = url, timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.top] return: " + xhr.responseText)
      read[ContainerTop](xhr.responseText)
    }
  }

  //https://docs.docker.com/reference/api/docker_remote_api_v1.17/#list-images
  def images(): Future[Seq[Image]] = {
    val url = connection.url + "/images/json"
    Ajax.get(url = url, timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.images] return: " + xhr.responseText)
      read[Seq[Image]](xhr.responseText)
    }
  }

  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#list-containers
  private def containers(all: Boolean): Future[Seq[Container]] = {
    val url = connection.url + "/containers/json?all=" + all
    Ajax.get(url = url, timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.containers] return: " + xhr.responseText)
      read[Seq[Container]](xhr.responseText)
    }
  }

  private def info(): Future[Info] = {
    val url = connection.url + "/info"
    Ajax.get(url = url, timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.info] return: " + xhr.responseText)
      read[Info](xhr.responseText)
    }
  }

  private def version(): Future[Version] = {
    val url = connection.url + "/version"
    Ajax.get(url = url, timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.version] return: " + xhr.responseText)
      read[Version](xhr.responseText)
    }
  }
}
