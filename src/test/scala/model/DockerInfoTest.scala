package model


import resources.{ApiV17, ApiV21}
import upickle.default._
import utest._

object DockerInfoTest extends TestSuite {

  def tests = TestSuite {

    'V19 {
      'DockerInfo_V19 {
        val json = ApiV17.Info
        val info = read[Info](json)
        val swarmInfo = info.swarmMasterInfo.toSeq

        assert (swarmInfo == Seq.empty)// no swarm for a normal docker
        assert ( info.Images == 33)

      }
    }
    'Swarm {
      'DockerInfo {
        val json = ApiV21.SwarmMaster.Info
        val info = read[Info](json)
        val swarmInfo = info.swarmMasterInfo.toSeq


        assertMatch(swarmInfo) {
          case Seq(
          ("Role", "primary"),
          ("Strategy", "spread"),
          ("Filters", "health, port, dependency, affinity, constraint"),
          ("Nodes", "2")
          )
          =>
        }

        val nodesDescription = info.swarmNodesDescription

        val nodes = nodesDescription.map(_.head)
        assertMatch(nodes) {
          case Seq(
          ("swarm-agent-00", "192.168.99.102:2376"),
          ("swarm-node-01", "192.168.99.103:2376")
          )
          =>
        }


      }
    }

  }
}