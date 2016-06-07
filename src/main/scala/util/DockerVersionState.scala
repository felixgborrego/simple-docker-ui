package util

import japgolly.scalajs.react.extra.{Broadcaster, Listenable}
import model.Version
import util.logger.log

import scala.concurrent.Future

object CurrentDockerApiVersion {
  private var ApiVersionMayor = 0
  private var APiVersionMinor = 0

  def register() =
    VersionBroadcaster.register { version: Version =>
      ApiVersionMayor = version.mayorVersion
      APiVersionMinor = version.minorVersion
      log.info(s"Using api version: $ApiVersionMayor.$APiVersionMinor")
    }

  // container.ImageId is required in /containers/id/json - Added in API version 1.23
  def checkSupportGC(): Boolean = checkVersion(
    mayorRequired = 1,
    minorRequired = 23)

  def checkVersion(mayorRequired: Int, minorRequired: Int): Boolean = (ApiVersionMayor, APiVersionMinor) match {
    case (mayor, _) if (mayor > mayorRequired) => true
    case (mayor, minor) if (mayor == mayorRequired && minor >= minorRequired) => true
    case _ => false
  }

  object VersionBroadcaster extends Broadcaster[Version] {
    def publishVersion(version: Version): Unit = broadcast(version)
  }

}


