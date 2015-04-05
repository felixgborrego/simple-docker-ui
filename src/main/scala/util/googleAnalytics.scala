package util

import scala.scalajs.js

object googleAnalytics {
  val Service = "DockerUI"
  val AnalyticsID = "UA-61270183-1"

  private lazy val tracker =
    interface.analytics.getService(Service).getTracker(AnalyticsID)


  def sendAppView(name: String) = tracker.sendAppView(name)

  def sendEvent(events: String*) = tracker.sendEvent(events)

  object interface extends js.GlobalScope {

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

}
