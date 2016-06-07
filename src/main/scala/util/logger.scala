package util
import googleAnalytics._
import org.scalajs.dom.ext.AjaxException
object logger {
  def log = DefaultLogger
}

object DefaultLogger {

  def debug(msg: => String) = println(msg)

  def info(msg: => String) = println(msg)

  def error(page: String, msg: => String, ex: => Exception) =  {
    val message = ex match {
      case ex: upickle.Invalid.Data => s"[$page] $msg  ${ex.getMessage} - Parser error: ${ex.data} ${ex.msg}"
      case ex: AjaxException => s"[$page] $msg  ${ex.getMessage} - $ex - isTimeout: ${ex.isTimeout}, status: ${ex.xhr.status}, response: ${ex.xhr.responseText}, url: ${ex.xhr}"
      case ex:Exception => s"[$page] $msg  ${ex.getMessage} - $ex"
    }
    println(s"error: $message")
    sendException(message)
  }
}