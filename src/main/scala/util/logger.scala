package util

import googleAnalytics._
import org.scalajs.dom.ext.AjaxException

object logger {
  def log = DefaultLogger
}

object DefaultLogger {

  def debug(msg: => String) = println(msg)

  def info(msg: => String) = println(msg)

  def error(page: String, msg: => String, ex: => Exception) = {
    val (report, message) = ex match {
      case ex: upickle.Invalid.Data => (true, s"[$page] $msg  ${ex.msg} - Parser error: ${ex.data}")
      case ex: AjaxException if (ex.isTimeout) => (false, s"[$page] $msg  ${ex.getMessage} - $ex - isTimeout: ${ex.isTimeout}, status: ${ex.xhr.status}, response: ${ex.xhr.responseText}, url: ${ex.xhr}")
      case ex: AjaxException => (true, s"[$page] $msg  ${ex.getMessage} - $ex - isTimeout: ${ex.isTimeout}, status: ${ex.xhr.status}, response: ${ex.xhr.responseText}, url: ${ex.xhr}")
      case ex: Exception => (true, s"[$page] $msg  ${ex.getMessage} - $ex")
    }
    println(s"error: $message")
    if (report) {
      sendException(message)
    }

  }
}