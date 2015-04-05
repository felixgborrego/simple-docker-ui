package api

import model.Connection
import util.logger._
import util.chrome.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.scalajs.js

object ConfigStorage {

  val DefaultMacUrl = "https://192.168.59.103:2376"
  val DefaultLinuxUrl = "http://localhost:4243"
  val DefaultWinUrl = ""
  val ParamUrlConnection = "url"

  def saveConnection(url: String) = save(ParamUrlConnection, url)

  def getUrlConnection(): Future[Option[Connection]] = get(ParamUrlConnection)
    .map(url => Some(Connection(url)))
    .recover { case _ => None }

  def save(key: String, value: String): Future[Unit] = {
    log.info(s"Saving $key = $value")
    val p = Promise[Unit]()
    val jsObject = scalajs.js.Dynamic.literal(key -> value)
    chrome.storage.local.set(jsObject, { () =>
      log.info(s"'$key' Saved")
      p.success()
    })
    p.future
  }

  def get(key: String): Future[String] = {
    val p = Promise[String]()
    chrome.storage.local.get(key, { result: js.Dynamic =>
      val value = result.selectDynamic(key).toString
      log.info(s"Value recover from local storage: $key = $value")
      if (value == "undefined")
        p.failure(new Exception(s"Key $key not found"))
      else
        p.success(value)
    })
    p.future
  }

  def getDefaultUrl(): Future[String] = getOs().map {
    case "mac" => DefaultMacUrl
    case "win" => ""
    case "linux" | "openbsd" => DefaultLinuxUrl
    case _ => DefaultWinUrl
  }


  private def getOs(): Future[String] = {
    val p = Promise[String]()
    chrome.runtime.getPlatformInfo { info: PlatformInfo =>
      p.success(info.os)
    }
    p.future
  }
}
