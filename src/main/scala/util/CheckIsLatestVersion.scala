package util

import org.scalajs.dom.ext.{Ajax, AjaxException}
import upickle.default._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import logger.log

object CheckIsLatestVersion {
  def installedVersion(): String = PlatformService.current.appVersion

  case class Release(name: String)

  val Url = "https://api.github.com/repos/felixgborrego/docker-ui-chrome-app/releases/latest"

  def latestVersion(): Future[String] = {
    Ajax.get(Url, timeout = 500).map { xhr =>
      read[Release](xhr.responseText)
    }.map(_.name)
  }

  var alreadyChecked = false

  def check(callback: (String => Unit)): Unit = if (!alreadyChecked) {
    alreadyChecked = true
    val installed = installedVersion()
    latestVersion().map {
      case `installed` => log.info(s"Installed version $installed is the latest version")
      case latest => callback(s"There is a new version available $latest")
    }.onFailure {
      case ex: AjaxException => log.info(s"Unable to fetch latest version - ${ex.xhr.responseText}")
    }
  }


}