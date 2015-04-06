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


  def bytesToSize(bytes: Int) = {
    val Sizes = Seq("Bytes", "KB", "MB", "GB", "TB")
    if (bytes == 0) {
      "0 Byte"
    } else {
      val i = Math.floor(Math.log(bytes) / Math.log(1024)).toInt
      Math.round(bytes / Math.pow(1024, i)) + " " + Sizes(i)
    }
  }


  def subId(id: String) = id.take(12)

}
