package ui.pages

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import model._
import ui.WorkbenchRef
import ui.widgets._
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global

object ImagePage {

  case class State(info: Option[ImageInfo] = None,
                   history: Seq[ImageHistory] = Seq.empty,
                   error: Option[String] = None)

  case class Props(ref: WorkbenchRef, image: Image)

  case class Backend(t: BackendScope[Props, State]) {

    def willStart(): Unit = t.props.ref.client.map { client =>
      val result = for {
        info <- client.imageInfo(t.props.image.Id)
        history <- client.imageHistory(t.props.image.Id)
      } yield t.modState(s => s.copy(info = Some(info), history = history))

      result.onFailure {
        case ex: Exception =>
          log.error("ImagePage", s"Unable to get imageInfo for ${t.props.image.id}", ex)
          t.modState(s => s.copy(error = Some(s"Unable to connect")))
      }

    }
  }

  def apply(image: Image, ref: WorkbenchRef) = new Page {
    val id = ImagesPage.id

    def component(ref: WorkbenchRef) = {
      val props = Props(ref, image)
      ImagePageRender.component(props)
    }
  }

}

object ImagePageRender {

  import ui.pages.ImagePage._

  val component = ReactComponentB[Props]("ImagePage")
    .initialState(State())
    .backend(new Backend(_))
    .render((P, S, B) => vdom(P, S, B))
    .componentWillMount(_.backend.willStart())
    .build


  def vdom(P: Props, S: State, B: Backend): ReactElement =
    <.div(
      S.error.map(Alert(_, Some(P.ref.link(SettingsPage)))),
      S.info.map(vdomInfo(_, S, P, B))
    )


  def vdomInfo(imageInfo: ImageInfo, S: State, P: Props, B: Backend) = {
    import util.stringUtils._
    val generalInfo = Map(
      "Id" -> P.image.id,
      "Name" -> substringBefore(P.image.RepoTags.headOption.getOrElse(""), ":"),
      "Tags" -> P.image.RepoTags.map(substringAfter(_, ":")).mkString(", "),
      "Created" -> P.image.created
    )
    val executionInfo = Map(
      "Author" -> imageInfo.Author,
      "Os" -> imageInfo.Os,
      "Command" -> imageInfo.Config.cmd.mkString(" "),
      "Environment" -> imageInfo.Config.env.mkString(" "),
      "WorkingDir" -> imageInfo.Config.WorkingDir
    )

    <.div(
      InfoCard(generalInfo, InfoCard.SMALL, None),
      InfoCard(executionInfo),
      vdomHistory(S.history)
    )
  }

  def vdomHistory(history: Seq[ImageHistory]): ReactElement = {
    val values = history.map(row => Map("Created" -> row.created, "Id" -> row.id, "Size" -> row.size, "Created By" -> row.CreatedBy))
    <.div(^.className := "container  col-sm-12",
      <.div(^.className := "panel panel-default  bootcards-summary",
        <.div(^.className := "panel-heading clearfix",
          <.h3(^.className := "panel-title pull-left",
            <.span(^.className := "glyphicon glyphicon-list"), " Creation History"
          )
        ),
        TableCard(values)
      )
    )
  }

}
