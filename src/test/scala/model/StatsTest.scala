package model

import model.stats.ContainerStats
import resources.ApiV19
import upickle.default._
import utest._

object StatsTest extends TestSuite {

  def tests = TestSuite {

    'V19 {
      'ParseStats {
        val json = ApiV19.ContainerStats
        val info = read[ContainerStats](json)


        assert(info.network.rx_bytes == 648)
        assert(info.precpu_stats.cpu_usage.total_usage == 10570644357d)

      }
    }

  }
}