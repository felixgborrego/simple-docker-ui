package ui

import japgolly.scalajs.react._
import util.logger._
import org.scalajs.dom

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

@JSExport("MainApp")
object MainApp extends JSApp {

  @JSExport
  override def main(): Unit = {
    log.info("Staring app")
    val ui = Workbench()
    React.render(ui, dom.document.getElementById("container"))
  }
}