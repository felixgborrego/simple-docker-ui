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
    val router = MainRouter.routerComponent()
    React.render(router, dom.document.getElementById("container"))
  }
}