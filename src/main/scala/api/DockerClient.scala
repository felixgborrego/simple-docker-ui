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

  def containersInfo(): Future[ContainersInfo] = for {
    r <- containers(all = false)
    all  <-containers(all = true)
  } yield ContainersInfo(r, all)



  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#inspect-a-container
  def containerInfo(containerId:String): Future[ContainerInfo] ={
    val url = connection.url + "/containers/"+containerId+"/json"
    Ajax.get(url).map { xhr =>
      println("[dockerClient.containerInfo] return: " + xhr.responseText)
      read[ContainerInfo](xhr.responseText)
    }
  }

  def top(containerId:String): Future[ContainerTop] ={
    val url = connection.url + "/containers/"+containerId+"/top"
    Ajax.get(url).map { xhr =>
      println("[dockerClient.top] return: " + xhr.responseText)
      read[ContainerTop](xhr.responseText)
    }
  }

  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#list-containers
  private def containers(all: Boolean): Future[Seq[Container]] = {
    val url = connection.url + "/containers/json?all=" + all
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
