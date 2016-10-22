package util

import model.BasicWebSocket
import util.logger.log

import scala.language.reflectiveCalls
import scala.scalajs.js
import scala.scalajs.js.Function1

@js.native
trait HijackSocket extends js.Object {
  def addListener(eventName: String, listener: js.Function1[js.Dynamic, _]): this.type = js.native
  def write(data: String): Unit = js.native
  def destroy(): Unit = js.native
}


class HijackWebSocket(socket: HijackSocket) extends BasicWebSocket {

  socket.addListener("data", { socketData: js.Any =>
    log.debug(s"HijackWebSocket data: $socketData")
    onmessage(new {
      def data = socketData
    })
  })

  socket.addListener("connect", { socketData: js.Any =>
    log.debug(s"HijackWebSocket connect: $socketData")
    onopen()
  })

  socket.addListener("close", { socketData: js.Any =>
    log.debug(s"HijackWebSocket close: $socketData")
    onclose(new {
      def code = 1
    })
  })

  socket.addListener("error", { socketData: js.Dynamic =>
    log.debug(s"HijackWebSocket error: $socketData")
    onerror(new {
      def message = socketData.message.toString
    })
  })

  override def send(data: String): Unit =  {
    socket.write(data)
  }

  override def close(code: Int, reason: String): Unit = {
    socket.destroy()
  }

  // Default impl, will be replaced by the attached Terminal
  override var onopen: Function1[Unit, _] = { x: Unit => () }
  override var onmessage: js.Function1[ {def data: js.Any}, _] = { x: {def data: js.Any} => () }
  override var onclose: js.Function1[ {def code: Int}, _] = { x: {def code: Int} => () }
  override var onerror: js.Function1[ {def message: String}, _] = { x: {def message: String} => () }
}