package util

object stringUtils {

  def substringAfter(s: String, k: String) = {
    s.indexOf(k) match {
      case -1 => ""
      case i => s.substring(i + k.length)
    }
  }

  def substringBefore(s: String, k: String) = {
    s.indexOf(k) match {
      case -1 => s
      case i => s.substring(0, i)
    }
  }
}
