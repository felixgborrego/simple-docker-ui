import japgolly.scalajs.react._
import org.scalajs.dom
import ui.Workbench
import util.chrome.ChromePlatformService
import util.{CurrentDockerApiVersion, PlatformService}
import util.chrome.api._
import util.logger._

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

@JSExport("ChromeMainApp")
object ChromeMainApp extends JSApp {

  @JSExport
  override def main(): Unit = {
    PlatformService.register(ChromePlatformService)

    log.info(s"Staring app ${PlatformService.current.appVersion}")
    val ui = Workbench()
    CurrentDockerApiVersion.register()
    React.render(ui, dom.document.getElementById("container"))
  }
}

