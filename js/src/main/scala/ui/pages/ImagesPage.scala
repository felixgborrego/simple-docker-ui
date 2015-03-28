package ui.pages

import api.{ConfigStore, DockerClient}
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model.{Connection, ConnectionError, Image}
import ui.Workbench
import ui.widgets.TableCard
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global

object ImagesPage extends Page {

  val id = "Images"

  case class State(localImages: Seq[Image] = Seq.empty)

  case class Props(connection: Connection)

  case class Backend(t: BackendScope[Props, State]) {
    def willStart(): Unit = {
      DockerClient(t.props.connection).images().map { images =>
        t.modState(s => State(images))
      }.onFailure {
        case ex: Exception =>
          log.error("Unable to get images", ex)
          Workbench.error(ConnectionError(ex.getMessage))
      }
    }
  }

  def component() = {
    val props = Props(ConfigStore.connection)
    ImagesPageRender.component(props)
  }
}


object ImagesPageRender {

  import ui.pages.ImagesPage._

  val component = ReactComponentB[Props]("ImagesPage")
    .initialState(State())
    .backend(new Backend(_))
    .render((P, S, B) => {
      vdom(S.localImages)
    }).componentWillMount(_.backend.willStart())
    .build


  def vdom(localImages: Seq[Image]) = {
    val data = localImages.map { image =>
      Map(
        "Id" -> image.id,
        "Tags" -> image.RepoTags.mkString(", "),
        "Created" -> image.created,
        "Size" -> image.virtualSize
      )
    }
    TableCard(data, Some("Local images"))
  }

}