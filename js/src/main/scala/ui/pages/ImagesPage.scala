package ui.pages

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model.Image
import ui.WorkbenchRef
import ui.widgets.{Alert, TableCard}
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global

object ImagesPage extends Page {

  val id = "Images"

  case class State(localImages: Seq[Image] = Seq.empty, error: Option[String] = None)

  case class Props(ref: WorkbenchRef)

  case class Backend(t: BackendScope[Props, State]) {
    def willStart(): Unit = {
      t.props.ref.client.map { client =>
        client.images().map { images =>
          t.modState(s => State(images))
        }.onFailure {
          case ex: Exception =>
            log.error("Unable to get Metadata", ex)
            t.modState(s => s.copy(error = Some("Unable to get data: " + ex.getMessage)))
        }
      }
    }
  }

  def component(ref: WorkbenchRef) = {
    val props = Props(ref)
    ImagesPageRender.component(props)
  }
}


object ImagesPageRender {

  import ui.pages.ImagesPage._

  val component = ReactComponentB[Props]("ImagesPage")
    .initialState(State())
    .backend(new Backend(_))
    .render((P, S, B) => {
    vdom(S)
  }).componentWillMount(_.backend.willStart())
    .build


  def vdom(S: State) = <.div(
    S.error.map(Alert(_, None)),
    TableCard(S.localImages.map { image =>
      Map(
        "Id" -> image.id,
        "Tags" -> image.RepoTags.mkString(", "),
        "Created" -> image.created,
        "Size" -> image.virtualSize
      )
    }, Some("Local images"))
  )


}