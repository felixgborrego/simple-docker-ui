import japgolly.scalajs.react.React
import org.scalajs.dom
import ui.Workbench
import util.logger._
import util.{CurrentDockerApiVersion, ElectronPlatformService, PlatformService}

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

@JSExport("ElectronMainApp")
object ElectronMainApp extends JSApp {

  override def main(): Unit = {
    PlatformService.register(ElectronPlatformService)
    CurrentDockerApiVersion.register()
    log.info(s"Staring app: ${PlatformService.current.appVersion}")
    val ui = Workbench()

    React.render(ui, dom.document.getElementById("container"))
  }
}
