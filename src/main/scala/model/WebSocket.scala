package model

import scala.scalajs.js

// Minimal interface based on org.scalajs.dom.raw.WebSocket
// Shared code between Chrome app WebSocket and Node.js Socket
trait BasicWebSocket {

  def send(data: js.Any): Unit

  def close(code: Int, reason: String): Unit

  def addEventListener(`type`: String, listener: js.Function1[Any, _]): Unit
  def removeEventListener(`type`: String, listener: js.Function1[Any, _]): Unit
}
