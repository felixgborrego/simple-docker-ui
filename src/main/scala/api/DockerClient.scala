package api

import model._
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw._
import upickle._
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DockerClient(connection: Connection) {

  val HttpTimeOut = 10 * 1000 // 3 seconds

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

  // https://docs.docker.com/reference/api/docker_remote_api_v1.17/#stop-a-container
  def stopContainer(containerId: String): Future[ContainerInfo] = {
    val TimeoutSeg = 3
    val url = connection.url + "/containers/" + containerId + "/stop?t=" + TimeoutSeg
    log.info("[dockerClient.stopContainer] url: " + url)
    Ajax.post(url = url, timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.stopContainer] return: " + xhr.responseText)
      //  p.completeWith(containerInfo(containerId))
    }.flatMap(_ => containerInfo(containerId))
  }


  def startContainer(containerId: String): Future[ContainerInfo] = {
    val url = connection.url + "/containers/" + containerId + "/start"
    log.info("[dockerClient.startContainer] url: " + url)
    Ajax.post(url = url, timeout = HttpTimeOut).map { xhr =>
      log.info("[dockerClient.startContainer] return: " + xhr.responseText)
    }.flatMap(_ => containerInfo(containerId))
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

  def attachToContainer(containerId: String): WebSocket = {
    import util.stringUtils._
    val schema = if (connection.url.startsWith("http://")) "ws://" else "wss://"

    //val url = "ws://"+ substringAfter(connection.url + "/containers/" + containerId + "/attach/ws?logs=0&stream=1&stdin=1&stdout=1&stderr=1","://")
    val url = schema + substringAfter(connection.url + "/containers/" + containerId + "/attach/ws?logs=1&stderr=1&stdout=1&stream=1&stdin=1", "://")
    // val url = "ws://echo.websocket.org/"
    log.info("[dockerClient.attach] url: " + url)
    var ws = new WebSocket(url)

    ws
  }

  /*
  def attachToContainer(containerId: String): WebSocket = {
    import util.stringUtils._

    def execCreate(containerId: String): Future[ExecCreated] = {
      val url = connection.url + "/containers/" + containerId + "/exec"
      val data = write(ExecStart(false, true))

      Ajax.post(url = url, data = data, timeout = HttpTimeOut).map { xhr =>
        log.info("[dockerClient.execCreate] return: " + xhr.responseText)
        read[ExecCreated](xhr.responseText)
      }
    }



    def execStart(execCreated: ExecCreated) = {

      val schema = if (connection.url.startsWith("http://")) "ws://" else "wss://"

      //val url = "ws://"+ substringAfter(connection.url + "/containers/" + containerId + "/attach/ws?logs=0&stream=1&stdin=1&stdout=1&stderr=1","://")
      val url = schema + substringAfter(connection.url + "/containers/" + containerId + "/attach/ws?logs=0&stream=1&stdout=1", "://")
      // val url = "ws://echo.websocket.org/"
      log.info("[dockerClient.attach] url: " + url)
      var ws = new WebSocket(url)
    }
    //ws.onmessage = (x: MessageEvent) => Console.println(x.data.toString)
    //ws.onopen = (x: Event) => {}
    //ws.onerror = (x: ErrorEvent) => Console.println("some error has   occured " + x.message)
    //ws.onclose = (x: CloseEvent) => {}
    //ws
  }
*/
}
