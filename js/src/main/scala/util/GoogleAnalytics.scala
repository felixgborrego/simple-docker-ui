package util

import util.GlobalGoogleAnalytics.Tracker

import scala.scalajs.js

object GoogleAnalytics {
  val Service = "DockerUI"
  val AnalyticsID = "UA-61270183-1"

  def apply(): Tracker =
    GlobalGoogleAnalytics.analytics.getService(Service).getTracker(AnalyticsID)
}

object GlobalGoogleAnalytics extends js.GlobalScope {

  def analytics: Analytics = js.native

  trait Analytics extends js.Object {
    def getService(name: String): Service = js.native
  }

  trait Service extends js.Object {
    def getTracker(id: String): Tracker = js.native
  }

  trait Tracker extends js.Object {
    def sendAppView(name: String): Unit = js.native

    def sendEvent(events: Seq[String]): Unit = js.native
  }

}
