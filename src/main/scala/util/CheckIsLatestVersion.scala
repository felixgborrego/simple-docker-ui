package util

import org.scalajs.dom.ext.{Ajax, AjaxException}
import upickle.default._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import logger.log

object CheckIsLatestVersion {
  def installedVersion(): String = s"v${PlatformService.current.appVersion}"

  case class Release(tag_name: String)

  val Url = "https://api.github.com/repos/felixgborrego/simple-docker-ui/releases/latest"

  def latestVersion(): Future[String] = {
    Ajax.get(Url, timeout = 5000).map { xhr =>
      val version = read[Release](xhr.responseText)
      version
    }.map(_.tag_name)
  }

  var alreadyChecked = false
  var checkMessage: Option[String] = None

  def check(callback: (String => Unit)): Unit = if (!alreadyChecked) {
    alreadyChecked = true
    val installed = installedVersion()
    latestVersion().map {
      case `installed` =>
        log.info(s"Installed version $installed is the latest version")
      case latest =>
        log.info(s"Latest Version: $latest, installed: $installed")
        val msg = s"There is a new version available $latest"
        checkMessage = Some(msg)
        callback(msg)
    }.onFailure {
      case ex: AjaxException =>
        log.info(s"Unable to fetch latest version - ${ex} - ${ex.xhr.responseText}")
    }
  } else {
    checkMessage.map(callback)
  }


}