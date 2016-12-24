package util

import api._
import model.stats.ContainerStats
import model.{BasicWebSocket, Connection, DockerEvent}
import nodejs.raw.EventEmitter
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax.InputData
import util.EventsCustomParser.DockerEventStream
import util.PullEventsCustomParser.{EventStatus, EventStream}
import util.logger.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.{Dynamic, URIUtils}

object ElectronPlatformService extends PlatformService {

  override def appVersion: String = {
    import js.Dynamic.{global => g}
    val electron = g.require("electron")
    val version = electron.remote.app.getVersion().asInstanceOf[String]
    s"$version"
  }

  val keyStoragePrefix = "v2_"

  def os(): String = {
    import js.Dynamic.{global => g}
    val os = g.require("os")
    // https://nodejs.org/api/os.html#os_os_release
    // 'darwin', 'freebsd', 'linux', 'sunos' or 'win32'
    os.platform().asInstanceOf[String]
  }

  override def osName: Future[String] = Future.successful(os())

  override def save(key: String, value: String): Future[Unit] = Future.successful {
    dom.window.localStorage.setItem(keyStoragePrefix + key, value)
  }

  override def get(key: String): Future[String] = Future {
    val value = Option(dom.window.localStorage.getItem(keyStoragePrefix + key))
    value.getOrElse(throw new Exception(s"Key $key not found"))
  }

  override def remove(key: String): Future[Unit] = Future.successful {
    dom.window.localStorage.removeItem(keyStoragePrefix + key)
  }

  override def sendAppView(name: String): Unit = {
    ElectronAnalytics.sendPageView(name)
  }

  override def sendEvent(category: String, action: String, label: String): Unit = {
    ElectronAnalytics.sendEvent(category, action, label)
  }

  override def sendException(ex: String): Unit = {
    ElectronAnalytics.sendException(ex)
  }

  override def buildDockerClient(con: Connection): DockerClient = try {
    val dockerConnection = new ElectronDockerConnection(con)
    new DockerClient(dockerConnection)
  } catch {
    case ex: Throwable =>
      log.debug(s"ex:  ElectronPlatformService.buildDockerClient $ex")
      throw ex
  }

  val DefaultMacLinuxDockerURL = "unix:///var/run/docker.sock"
  val DefaultWindows = "http://localhost:2375"

  def defaultUrl: Future[String] =  {
    osName.map {
      case "win32" =>
        log.debug(s"Default docker for Windows url $DefaultWindows")
        DefaultWindows
      case "darwin" =>
        log.debug(s"Default docker for Mac url $DefaultMacLinuxDockerURL")
        DefaultMacLinuxDockerURL
      case other =>
        log.debug(s"Default docker for '$other' url $DefaultMacLinuxDockerURL")
        DefaultMacLinuxDockerURL
    }

  }

  override def checkIsLatestVersion(callback: (String) => Unit): Unit = CheckIsLatestVersion.check(callback)
}

class ElectronDockerConnection(val connection: Connection) extends DockerConnection {
  import DockerClientConfig._

  import js.JSConverters._

  val info = connection.url
  val versionPath = "/" + DockerVersion
  val modem = DockerModem.build(connection)

  private def dial(dialOptions: DialOptions,
                   onStreamingData: String => Unit = _ => Unit,
                   shouldAbort: Unit => Boolean = _ => false,
                   onWebSocketCreated: BasicWebSocket => Unit = _ => Unit
                  ): Future[Response] = {
    val p = Promise[Response]
    log.debug(s"Dailing to Docker api ${dialOptions.method} ${dialOptions.path}, data: ${dialOptions.options.map(js.JSON.stringify(_))}")
    val callback: js.Function2[js.Any, js.Dynamic, Unit] =
      (msg: js.Any, response: js.Dynamic) => {
        if (msg == null) {
          if (dialOptions.hijack) {
            processHijackResponse(onWebSocketCreated, response)

          } else if (dialOptions.isStream) {
            def onComplete { p.success(Response("", 200)) }
            processStreamingResponse(onStreamingData, shouldAbort, onComplete, response)

          } else {
            val responseText = js.Dynamic.global.JSON.stringify(response).asInstanceOf[String]
            p.success(Response(responseText, 200))

          }
        } else {
          log.debug(s"dial fail: $msg")
          p.failure(new ConnectionException(msg.toString))
        }
        ()
      }

    try {
      modem.dial(dialOptions, callback)
    } catch {
      case ex: Throwable =>
        log.debug(s"dial error: $ex")
        p.failure(ConnectionException(ex.getMessage))
    }
    p.future
  }

  def processHijackResponse(onWebSocketCreated: BasicWebSocket => Unit, response: Dynamic): Unit = {
    log.debug(s"creating websocket $response")
    val socket = response.asInstanceOf[HijackSocket]
    val ws = new HijackWebSocket(socket)
    onWebSocketCreated(ws)
  }

  def processStreamingResponse(onStreamingData: (String) => Unit, shouldAbort: Unit => Boolean, onComplete: => Unit,response: Dynamic): Unit = {
    log.debug(s"processing stream $response")
    val eventEmiter = response.asInstanceOf[EventEmitter]
    eventEmiter.on("data") { chuck: Dynamic =>
      val data = chuck.toString
      onStreamingData(data)
      if (shouldAbort()) {
        log.info("Stream aborted")
        response.req.abort()
      }
    }
    eventEmiter.on("end") { _: Dynamic =>
      onComplete
    }
  }

  def get(path: String, timeout: Int = HttpTimeOut): Future[Response] = {
    val options = new DialOptions(path = path, method = "GET", options = js.undefined, Map(("200", true)).toJSDictionary)
    dial(options)
  }

  def post(path: String, json: Option[InputData] = None, headers: Map[String, String] = Map.empty, timeout: Int = HttpTimeOut) = {
    dial(new DialOptions(
      path = path,
      method = "POST",
      options = json.map(data => js.JSON.parse(data.toString).asInstanceOf[InputData]).orUndefined,
      Map(("200", true), ("201", true), ("204", true)).toJSDictionary
    ))
  }

  def delete(path: String, timeout: Int = HttpTimeOut): Future[Response] = {
    dial(new DialOptions(path = path, method = "DELETE", options = js.undefined, Map(("200", true), ("204", true)).toJSDictionary))
  }

  // TODO refactor
  def attachToContainer(containerId: String): Future[BasicWebSocket] = {
    val p = Promise[BasicWebSocket]
    def onWebSocketCreated(ws: BasicWebSocket): Unit = {
      log.debug(s"onWebSocketCreated!")
      p.success(ws)
    }

    val path = s"$versionPath/containers/$containerId/attach?logs=1&stderr=1&stdout=1&stream=1&stdin=1&"

    log.info(s"[dockerClient.attach] path:  $path")
    val options = new DialOptions(
      path = path,
      method = "POST",
      options = Some("{}".asInstanceOf[InputData]).orUndefined,
      statusCodes = Map(("200", true)).toJSDictionary,
      isStream = true,
      hijack = true,
      openStdin = true
    )

    dial(options, onWebSocketCreated = onWebSocketCreated)
      .onFailure { case ex: Exception =>
        log.debug(s"Unable to connect WS - ${ex.toString}")
        p.failure(ex)
      }

    p.future
  }

  // TODO refactor
  def pullImage(term: String): EventStream = {
    val p = Promise[Seq[EventStatus]]

    val currentStream = EventStream()

    def onStreamingData(data: String): Unit = {
      currentStream.data = data
    }
    val sanitizedTerm = URIUtils.encodeURIComponent(term)
    val pullUrl = s"$versionPath/images/create?fromImage=$sanitizedTerm&"
    val options = new DialOptions(path = pullUrl, method = "POST", options = js.undefined, Map(("200", true)).toJSDictionary, isStream = true)
    log.info(s"[dockerClient.pullImage] start")

    dial(options, onStreamingData).onComplete { result =>
      currentStream.done = true
    }

    currentStream
  }

  // TODO refactor
  def events(update: Seq[DockerEvent] => Unit): ConnectedStream = {
    val since = {
      val t = new js.Date()
      t.setDate(t.getDate() - 3) // pull 3 days
      t.getTime() / 1000
    }.toLong

    var isAborted = false
    val stream = new ConnectedStream {
      override def abort(): Unit = {
        isAborted = true
      }
    }

    val eventsUrl = s"$versionPath/events?since=$since"
    val currentStream = DockerEventStream()

    def onStreamingData(data: String): Unit = {
      EventsCustomParser.parse(currentStream, data)
      update(currentStream.events)
    }

    val options = new DialOptions(path = eventsUrl, method = "GET", options = js.undefined, Map(("200", true)).toJSDictionary, isStream = true)
    dial(options, onStreamingData, (Unit) => isAborted)

    stream
  }

  // TODO refactor impl
  def containerStats(containerId: String)(updateUI: (Option[ContainerStats], ConnectedStream) => Unit): Unit = {
    var isAborted = false
    val stream = new ConnectedStream {
      override def abort(): Unit = {
        isAborted = true
      }
    }

    def onStreamingData(data: String): Unit = {
      val stats = StatsCustomParser.parse(data)
      updateUI(stats, stream)
    }

    val statsUrl = s"$versionPath/containers/$containerId/stats"

    log.info(s"[dockerClient.containerStats] $statsUrl")
    val options = new DialOptions(path = statsUrl, method = "GET", options = js.undefined, Map(("200", true)).toJSDictionary, isStream = true)
    dial(options, onStreamingData, (Unit) => isAborted)
  }
}