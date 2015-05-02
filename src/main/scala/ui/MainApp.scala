package ui

import japgolly.scalajs.react._
import org.scalajs.dom
import util.chrome.api._
import util.logger._

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

@JSExport("MainApp")
object MainApp extends JSApp {

  @JSExport
  override def main(): Unit = {
    log.info(s"Staring app ${chrome.runtime.getManifest().version}")
    val ui = Workbench()
    React.render(ui, dom.document.getElementById("container"))
  }
}