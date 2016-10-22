package model

import scala.scalajs.js

// Minimal interface based on org.scalajs.dom.raw.WebSocket
// Shared code between Chrome app WebSocket and Node.js Socket
trait BasicWebSocket {

  var onmessage: js.Function1[ {def data: js.Any}, _]

  var onopen: js.Function1[ Unit, _]

  var onclose: js.Function1[ {def code: Int}, _]

  var onerror: js.Function1[ {def message: String}, _]

  def send(data: String): Unit

  def close(code: Int, reason: String): Unit
}
