package api

import model.Connection
import util.chrome.api._
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.scalajs.js

object ConfigStorage {

  val DefaultMacUrl = "https://192.168.59.103:2376"
  val DefaultLinuxUrl = "http://localhost:2375"
  val DefaultWinUrl = "http://192.168.59.103:2375"
  val ParamUrlConnection = "url"
  val ParamSavedUrlConnection ="saved_urls"

  def saveConnection(url: String) = save(ParamUrlConnection, url)

  def urlConnection: Future[Option[Connection]] = get(ParamUrlConnection)
    .map(url => Some(Connection(url)))
    .recover { case _ => None }

  private def save(key: String, value: String): Future[Unit] = {
    log.info(s"Saving $key = $value")
    val p = Promise[Unit]()
    val jsObject = scalajs.js.Dynamic.literal(key -> value)
    chrome.storage.local.set(jsObject, { () =>
      p.success(log.info(s"'$key' Saved"))
    })
    p.future
  }

  private def get(key: String): Future[String] = {
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

  def defaultUrl: Future[String] = os.map {
    case "mac" => DefaultMacUrl
    case "win" => DefaultWinUrl
    case "linux" | "openbsd" => DefaultLinuxUrl
    case _ => ""
  }

  def isRunningBoot2Docker:Future[Boolean] =  os.map {
    case "mac" | "win" => true
    case _ => false
  }

  private def os: Future[String] = {
    val p = Promise[String]()
    chrome.runtime.getPlatformInfo { info: PlatformInfo =>
      p.success(info.os)
    }
    p.future
  }

  // Return the list of url already used
  val Separator = "!;!"
  def savedUrls(): Future[Seq[String]] = get(ParamSavedUrlConnection)
    .map(_.split(Separator).toSeq)
    .recover {
      case ex:Exception => Seq.empty
    }

  def saveUrls(urls:Seq[String]) = save(ParamSavedUrlConnection,urls.mkString(Separator))
}
