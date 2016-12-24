package util

import model.BasicWebSocket
import org.scalajs.dom.raw.Event
import util.logger.log
import scala.language.reflectiveCalls
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportAll}

import js.Dynamic.{ global => g, newInstance => jsnew }

@js.native
trait HijackSocket extends js.Object {
  def addListener(eventName: String, listener: js.Function1[_, _]): this.type = js.native

  def removeListener(eventName: String, listener: js.Function1[_, _]): this.type = js.native
  def write(data: js.Any): Unit = js.native
  def destroy(): Unit = js.native
}

@JSExportAll
case class CustomTextEvent(data: String)

@JSExportAll
class HijackWebSocket(socket: HijackSocket) extends BasicWebSocket {

  socket.addListener("data", { socketData: js.Any =>
    log.debug(s"HijackWebSocket data: $socketData")
  })

  socket.addListener("connect", { socketData: js.Any =>
    log.debug(s"HijackWebSocket connect: $socketData")
  })

  @JSExport
  override def send(data: js.Any): Unit =  {
    socket.write(data)
  }

  @JSExport
  override def close(code: Int, reason: String): Unit = {
    socket.destroy()
  }

  @JSExport
  def addEventListener(`type`: String, listener: js.Function1[Any, _]): Unit = {
    if (`type` == "message") {
      socket.addListener("data", { socketData: js.Any =>
        log.debug(s"HijackWebSocket data: $socketData")
        val decoder = jsnew(g.TextDecoder)("utf-8")
        val data = decoder.decode(socketData).replace("\r$/g", "\r\n")
        listener(new CustomTextEvent(data.toString.replace("\n", "\r\n")))
        ()
      })
    } else {
      socket.addListener(`type`, listener)
    }

  }

  @JSExport
  def removeEventListener(`type`: String, listener: js.Function1[Any, _]): Unit = {
    if (`type` == "message") {
      socket.removeListener("data", listener)
    }
    socket.removeListener(`type`, listener)
  }
}