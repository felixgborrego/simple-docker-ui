package util

import java.util.UUID

import util.logger.log

import scala.scalajs.js

// Analytics using https://www.npmjs.com/package/universal-analytics
object ElectronAnalytics {

  import js.Dynamic.{global => g}

  val ua = g.require("universal-analytics")

  def sendEvent(category: String, action: String, label: String): Unit = {
    val event = toEventParams(category, action, label)
    visitor.event(event).send()
    log.info(s"sendEvent: $category, $action, $label")
  }

  def sendPageView(name: String): Unit = {
    val event = toPageViewParams(name)
    visitor.pageview(event).send()
    log.info(s"sendAppView: $name")
  }

  def sendException(ex: String): Unit = {
    log.info(s"sendException: $ex")
    visitor.exception(s"sendException: $ex").send()
  }

  val GoogleUA = "UA-61270183-1"

  // TODO Use custom user id
  // https://www.npmjs.com/package/electron-machine-id
  lazy val userId = UUID.randomUUID().toString
  lazy val visitor = ua(GoogleUA, userId)
  val appName = "Docker ui"
  val applicationId = "org.fgb.dockerui"

  lazy val appVersion = ElectronPlatformService.appVersion

  def toEventParams(eventCategory: String, eventAction: String, eventLabel: String) = {
    js.Dynamic.literal(ec = eventCategory, ea = eventAction, el = eventLabel, cd = eventCategory, an = appName, aid = applicationId, av = appVersion)
  }

  def toPageViewParams(page: String) = {
    js.Dynamic.literal(dp = page, cd = page, an = appName, aid = applicationId, av = appVersion)
  }

  def toExceptionParams(description: String) = {
    js.Dynamic.literal(exd = description, an = appName, aid = applicationId, av = appVersion)
  }


}
