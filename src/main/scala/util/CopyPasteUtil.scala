package util

import org.scalajs.dom.{document, window}
import util.logger.log

object CopyPasteUtil {
  def copyToClipboard(selector: String): Unit = {
    val element = document.querySelector(s".$selector")
    val range = document.createRange()
    range.selectNode(element)
    val selection = window.getSelection()
    selection.removeAllRanges()
    selection.addRange(range)
    val successful = document.execCommand("copy")
    log.debug(s"Copied to clipboard: $successful")
    window.setTimeout(() => selection.removeAllRanges(), 500)
  }
}
