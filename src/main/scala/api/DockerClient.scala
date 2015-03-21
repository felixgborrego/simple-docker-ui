package api

import model._
import org.scalajs.dom.ext.Ajax
import upickle._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DockerClient(connection: Connection) {

  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#ping-the-docker-server
  def ping(): Future[Unit] = {
    val url = connection.url + "/_ping"
    Ajax.get(url).map(_.responseText)
  }

  def metadata(): Future[DockerMetadata] = {
    for {
      info <- info()
      version <- version()
      containers <- containers(all = false)
    } yield DockerMetadata(connection, info, version, containers)
  }

  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#list-containers
  def containers(all: Boolean): Future[Seq[Container]] = {
    val url = connection.url + "/containers/json"
    Ajax.get(url).map { xhr =>
      println("[dockerClient.containers] return: " + xhr.responseText)
      read[Seq[Container]](xhr.responseText)
    }
  }

  private def info(): Future[Info] = {
    val url = connection.url + "/info"
    Ajax.get(url).map { xhr =>
      println("[dockerClient.info] return: " + xhr.responseText)
      read[Info](xhr.responseText)
    }
  }

  private def version(): Future[Version] = {
    val url = connection.url + "/version"
    Ajax.get(url).map { xhr =>
      println("[dockerClient.version] return: " + xhr.responseText)
      read[Version](xhr.responseText)
    }
  }
}
