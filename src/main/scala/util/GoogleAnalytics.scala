package util

import scala.scalajs.js

object googleAnalytics {
  val Service = "DockerUI"
  val AnalyticsID = "UA-61270183-1"

  private lazy val tracker =
    interface.analytics.getService(Service).getTracker(AnalyticsID)


  def sendAppView(name: String) = tracker.sendAppView(name)

  def sendEvent(category: String, action: String, label: String = "") = tracker.sendEvent(category, action, label)


  object EventCategory {
    val Connection = "Connection"
    val Save = "Save"
    val Image = "Image"
    val Container = "Container"
  }

  object EventAction {
    val Saved = "Saved"
    val Connected = "Connected"
    val Unable = "Unable"
    val InvalidVersion = "InvalidVersion"
    val UnsupportedOptionalVersion = "UnsupportedOptionalVersion"
    val Try = "Try"
    val Pull = "Pull"
    val GC = "GC"
    val Show = "Show"
    val Start = "Start"
    val Remove = "Remove"
    val Stop = "Stop"
  }

}

@js.native
object interface extends js.GlobalScope {

    def analytics: Analytics = js.native

  @js.native
    trait Analytics extends js.Object {
      def getService(name: String): Service = js.native
    }

  @js.native
    trait Service extends js.Object {
      def getTracker(id: String): Tracker = js.native
    }

  @js.native
    trait Tracker extends js.Object {
      def sendAppView(name: String): Unit = js.native

      def sendEvent(category: String, action: String, label: String): Unit = js.native
    }

}
