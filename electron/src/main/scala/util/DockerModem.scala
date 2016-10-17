package util

import model.Connection
import org.scalajs.dom.ext.Ajax.InputData
import util.logger.log

import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined

@ScalaJSDefined
class ModemOptions(val socketPath: String,
                   val host: String,
                   val protocol: String,
                   val port: String
                  ) extends js.Object


@ScalaJSDefined
class DialOptions(val path: String, val method: String, val options: js.UndefOr[InputData], val statusCodes: js.Dictionary[Boolean], val isStream: Boolean = false, val hijack: Boolean = false, val openStdin: Boolean = false) extends js.Object

@ScalaJSDefined
class DockerMessage(val json: String, val reason: String, val statusCode: String) extends js.Object


@js.native
trait DockerModem extends js.Object {

  def dial(dialOptions: DialOptions,
           callback: js.Function2[js.Any, js.Dynamic, Unit])

  def runtime: Runtime = js.native
}

// Wrapper for https://github.com/apocas/docker-modem
object DockerModem {
  def build(connection: Connection): DockerModem = {
    import js.Dynamic.{global => g, newInstance => jsnew}
    val Modem = g.require("docker-modem")
    log.debug(s"Building Docker modem for Url: $connection")
    val modemOptions = {
      if (connection.url.toLowerCase().startsWith("unix://")) {
        new ModemOptions(socketPath = connection.url.drop("unix://".size), host = "localhost", protocol = "http", port = null)
      } else {
        val protocol = connection.url.takeWhile(_ != ':')
        val host = connection.url.drop(protocol.size).drop("://".size).takeWhile(_ != ':')
        log.debug(s"Url: protocol: $protocol, host: $host")
        val port = connection.url.drop(protocol.size).drop("://".size).dropWhile(_ != ':').drop(1)
        log.debug(s"Url: port: $port")
        new ModemOptions(socketPath = null, host = host, protocol = protocol, port = port)
      }
    }
    log.debug(s"modemOptions: protocol: ${modemOptions.protocol} path: ${modemOptions.socketPath}")

    val modem = jsnew(Modem)(modemOptions).asInstanceOf[DockerModem]
    log.debug(s"modem built: $modem")
    modem
  }
}