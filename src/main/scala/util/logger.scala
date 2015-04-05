package util

object logger {
  def log = DefaultLogger
}


object DefaultLogger {

  def debug(msg: => String) = println(msg)

  def info(msg: => String) = println(msg)

  def error(msg: String, ex: Exception) = println(msg + " " + ex.getMessage + " " + ex)
}