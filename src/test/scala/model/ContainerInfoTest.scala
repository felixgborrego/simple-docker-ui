package model

import resources.{ApiV19, ApiV21}
import upickle.default._
import utest._
import utest.framework.TestSuite

object ContainerInfoTest extends TestSuite {


  def tests = TestSuite {
    'ContainerInfo {
      val json = ApiV21.ContainerInfo
      val info = read[ContainerInfo](json)
      assert(info.id == "ffef6e062e11")
    }


    'V21 {
      'Containers_All {
        val json = ApiV21.Containers
        val containers = read[Seq[Container]](json)
        val temp = containers.map(_.SizeRootFs)

        assert (temp == Seq(0,0))
      }

    }

    'V19 {
      'Containers_All {
        val json = ApiV19.Containers
          val containers = read[Seq[Container]](json)
          val defaultSize = containers.headOption.map(_.SizeRootFs)

          assert(defaultSize == Some(0))
      }

    }
  }
}
