package util

object logger {
  def log = DefaultLogger
}

object DefaultLogger {

  def debug(msg: => String) = println(msg)

  def info(msg: => String) = println(msg)

  def error(page: String, msg: => String, ex: => Exception) =  ex match {
    case ex: upickle.Invalid.Data => println(s"[$page] $msg  ${ex.getMessage} - Parser error: ${ex.data} ${ex.msg}")
    case ex:Exception => println(s"[$page] $msg  ${ex.getMessage} - $ex")
  }
}