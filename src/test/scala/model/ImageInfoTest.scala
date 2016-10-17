package model

import resources.ApiV21
import upickle.default._
import utest._
import utest.framework.TestSuite


object ImageInfoTest extends TestSuite  {

  def tests = TestSuite {

    'V21 {
      "RepoTags is null"- {
        val json = ApiV21.ImageInfoWithoutRepoTags
        val info = read[Image](json)


        assert(info.RepoTags == null)
      }
    }

  }
}
