package model

import resources.ApiV21
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
  }
}
