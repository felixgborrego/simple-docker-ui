package util

import util.logger.log

import scala.scalajs.js

// Analytics using https://www.npmjs.com/package/universal-analytics
object ElectronAnalytics {

  import js.Dynamic.{global => g}

  val ua = g.require("universal-analytics")

  def sendEvent(category: String, action: String, label: String): Unit = {
    val visitor = ua(GoogleUA, userId())
    visitor.event(category, action, label).send()
    log.info(s"sendEvent: $category, $action, $label")
  }

  def sendPageView(name: String): Unit = {
    val visitor = ua(GoogleUA, userId())
    visitor.pageview(name).send()
    log.info(s"sendAppView: $name")
  }

  def sendException(ex: String): Unit = {
    val visitor = ua(GoogleUA, userId())
    log.info(s"sendException: $ex")
    visitor.exception(s"sendException: $ex").send()
  }

  val GoogleUA = "UA-61270183-1"

  // TODO Use custom user id
  // https://www.npmjs.com/package/electron-machine-id
  def userId(): String = "dev-1"

}
