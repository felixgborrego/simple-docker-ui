package ui.pages

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model.Image
import ui.WorkbenchRef
import ui.widgets.Alert
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global

object ImagesPage extends Page {

  val id = "Images"

  case class State(localImages: Seq[Image] = Seq.empty, error: Option[String] = None)

  case class Props(ref: WorkbenchRef)

  case class Backend(t: BackendScope[Props, State]) {
    def willStart(): Unit = t.props.ref.client.map { client =>
      client.images().map { images =>
        t.modState(s => State(images))
      }.onFailure {
        case ex: Exception =>
          log.error("ImagesPage", "Unable to get Metadata", ex)
          t.modState(s => s.copy(error = Some(s"Unable to connect")))
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
    .render((P, S, B) => vdom(S, P))
    .componentWillMount(_.backend.willStart())
    .build


  def vdom(S: State, P: Props) = <.div(
    S.error.map(Alert(_, None)),
    table("Local images", S.localImages, P)
  )

  def table(title: String, images: Seq[Image], P: Props) =
    <.div(^.className := "container  col-sm-12",
      <.div(^.className := "panel panel-default  bootcards-summary",
        <.div(^.className := "panel-heading clearfix",
          <.h3(^.className := "panel-title pull-left")(title)
        ),
        <.table(^.className := "table table-hover",
          <.thead(
            <.tr(
              <.th("Id"),
              <.th("Tags"),
              <.th("Created"),
              <.th("Size")
            )
          ),
          <.tbody(
            images.map { img =>
              <.tr(
                <.td(P.ref.link(ImagePage(img, P.ref))(img.id)),
                <.td(img.RepoTags.mkString(", ")),
                <.td(img.created),
                <.td(img.virtualSize)
              )
            }
          )
        )
      )
    )

}