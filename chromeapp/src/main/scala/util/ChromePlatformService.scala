package util.chrome

import _root_.api._
import model.stats.ContainerStats
import model.{BasicWebSocket, Connection, DockerEvent}
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.ext.Ajax.InputData
import org.scalajs.dom.raw._
import util.EventsCustomParser.DockerEventStream
import util.PullEventsCustomParser.{EventStatus, EventStream}
import util.chrome.api._
import util.logger._
import util.{EventsCustomParser, GoogleAnalytics, PlatformService, StatsCustomParser}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.Function1

object ChromePlatformService extends PlatformService {
  override def appVersion: String = chrome.runtime.getManifest().version

  override def osName: Future[String] = {
    val p = Promise[String]()
    chrome.runtime.getPlatformInfo { info: PlatformInfo =>
      p.success(info.os)
    }
    p.future
  }

  override def save(key: String, value: String): Future[Unit] = {
    log.info(s"Saving $key = $value")
    val p = Promise[Unit]()
    val jsObject = scalajs.js.Dynamic.literal(key -> value)
    chrome.storage.local.set(jsObject, { () =>
      p.success(log.info(s"'$key' Saved"))
    })
    p.future
  }


  override def get(key: String): Future[String] = {
    val p = Promise[String]()
    chrome.storage.local.get(key, { result: js.Dynamic =>
      val value = result.selectDynamic(key).toString
      log.info(s"Value recover from local storage: $key = $value")
      if (value == "undefined")
        p.failure(new Exception(s"Key $key not found"))
      else
        p.success(value)
    })
    p.future
  }


  override def remove(key: String): Future[Unit] = {
    log.info(s"Removing $key")
    val p = Promise[Unit]()
    chrome.storage.local.remove(key, { () =>
      p.success(log.info(s"'$key' Removed"))
    })
    p.future
  }

  override def sendAppView(name: String): Unit = {
    GoogleAnalytics.sendAppView(name)
  }

  override def sendEvent(category: String, action: String, label: String): Unit = {
    GoogleAnalytics.sendEvent(category, action, label)
  }

  override def sendException(ex: String): Unit = {
    GoogleAnalytics.sendException(ex)
  }

  override def buildDockerClient(con: Connection): DockerClient = {
    val chromeCon = new ChromeDockerConnection(con)
    DockerClient(chromeCon)
  }

  override def defaultUrl: Future[String] = osName.map {
    case "mac" => ConfigStorage.DefaultMacUrl
    case "win" => ConfigStorage.DefaultWinUrl
    case "linux" | "openbsd" => ConfigStorage.DefaultLinuxUrl
    case _ => ""
  }
}


class ChromeDockerConnection(val connection: Connection) extends DockerConnection {

  import DockerClientConfig._

  import scala.concurrent.ExecutionContext.Implicits.global

  val info = connection.url

  val url = connection.url + "/" + DockerVersion
  val urlOptional = connection.url + "/" + DockerOptionalVersion

  def get(path: String, timeout: Int = HttpTimeOut): Future[Response] = Ajax.get(url = s"$url$path", timeout = timeout)
    .map(xhr => Response(xhr.responseText, xhr.status))

  def post(path: String, data: Option[InputData] = None, headers: Map[String, String] = Map.empty, timeout: Int = HttpTimeOut) =
    Ajax.post(url = s"$url$path", data = data.getOrElse(""), headers = headers, timeout = timeout)
      .map(xhr => Response(xhr.responseText, xhr.status))

  def delete(path: String, timeout: Int = HttpTimeOut): Future[Response] = Ajax.delete(url = s"$url$path", timeout = timeout)
    .map(xhr => Response(xhr.responseText, xhr.status))

  // TODO refactor
  def attachToContainer(containerId: String): Future[BasicWebSocket] = Future {
    import util.StringUtils._
    val schema = if (connection.url.startsWith("http://")) "ws://" else "wss://"
    val ws = schema + substringAfter(s"$url/containers/$containerId/attach/ws?logs=1&stderr=1&stdout=1&stream=1&stdin=1", "://")
    log.info(s"[dockerClient.attach] url: $ws")
    new WebSocketAdapter(new WebSocket(ws))
  }

  // TODO refactor
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

  // TODO refactor
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

// Bridge between common class and Chrome WebSocket
class WebSocketAdapter(socket: WebSocket) extends BasicWebSocket {

  override def send(data: String): Unit = socket.send(data)

  override def close(code: Int, reason: String): Unit = socket.close(code, reason)

  socket.onopen = { event: Event => this.onopen() }
  socket.onmessage = { event: MessageEvent =>
    this.onmessage(new {
      def data = event.data.asInstanceOf[js.Any]
    })
  }
  socket.onclose = { event: CloseEvent =>
    this.onclose(new {
      def code = event.code
    })
  }

  socket.onerror = { event: ErrorEvent =>
    this.onerror(new {
      def message = event.message
    })
  }

  override var onopen: Function1[Unit, _] = { x: Unit => () }
  override var onmessage: js.Function1[ {def data: js.Any}, _] = { x: {def data: js.Any} => () }
  override var onclose: js.Function1[ {def code: Int}, _] = { x: {def code: Int} => () }
  override var onerror: js.Function1[ {def message: String}, _] = { x: {def message: String} => () }


}
