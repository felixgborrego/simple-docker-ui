package util

import api.DockerClient
import model.Connection
import util.logger._

import scala.concurrent.Future

trait PlatformService {
  def appVersion: String

  def osName: Future[String]

  def save(key: String, value: String): Future[Unit]

  def get(key: String): Future[String]

  def remove(key: String): Future[Unit]

  def sendAppView(name: String): Unit

  def sendEvent(category: String, action: String, label: String = ""): Unit

  def sendException(ex: String): Unit

  def buildDockerClient(con: Connection): DockerClient

  def defaultUrl: Future[String]

  def checkIsLatestVersion(callback: (String => Unit)): Unit

}

object PlatformService {

  private var service: Option[PlatformService] = None

  def current: PlatformService = service.getOrElse(sys.error("No Platform registered"))

  def register(registerService: PlatformService) = {
    log.info(s"Registering ${registerService.getClass.getName}")
    service = Some(registerService)
  }

}

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
