package api

import model.Connection
import util.chrome.Api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import util.logger._

object ConfigStorage {

  // TODO get connection from local storage
  // linux
  //def connection = new Connection("http://localhost:4243")

  //macos
  //def connection = new Connection("https://192.168.59.103:2376")

  val PARAM_URL_CONNECTION = "url"

  def saveConnection(url: String) = save(PARAM_URL_CONNECTION, url)

  def getUrlConnection():Future[Option[Connection]] = get(PARAM_URL_CONNECTION)
    .map(url => Some(Connection(url)))
    .recover {
      case _ => None
    }

  def save(key: String, value: String): Future[Unit] = {
    log.info("Saving " + key + " = " + value)
    val p = Promise[Unit]()
    val jsObject = scalajs.js.Dynamic.literal(key -> value)
    chrome.storage.local.set(jsObject, { () =>
      log.info("'" + key + "' Saved")
      p.success()
    })
    p.future
  }

  def get(key: String): Future[String] = {
    val p = Promise[String]()
    log.info("Getting " + key )
    chrome.storage.local.get(key, { result: js.Dynamic =>
      val value = result.selectDynamic(key).toString
      log.info("Value recover from local storage:" + key + " = " + value)
      if (value == "undefined")
        p.failure(new Exception("Key " + key + " not found"))
      else
        p.success(value)
    })
    p.future
  }

}
