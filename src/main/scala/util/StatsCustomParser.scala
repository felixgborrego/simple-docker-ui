package util

import model.stats.ContainerStats
import upickle.default._

import scala.util.Try

// Hack to parse the stats json
object StatsCustomParser {

  def parse(data: String): Option[ContainerStats] = {
    data.split( """\{"read":""").lastOption
      .map(element => """{"read":""" + element)
      .map { text =>
        Try {
          Some(read[ContainerStats](text))
        }.getOrElse {
          None
        }
      }.flatten.lastOption
  }

}
