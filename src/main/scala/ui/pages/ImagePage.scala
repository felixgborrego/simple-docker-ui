package ui.pages

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import model._
import ui.WorkbenchRef
import ui.widgets._
import ui.widgets.dialogs.ContainerRequestForm
import util.googleAnalytics._
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ImagePage {

  case class State(info: Option[ImageInfo] = None,
                   history: Seq[ImageHistory] = Seq.empty,
                   error: Option[String] = None,
                   showCreateDialog: Boolean = false)

  case class Props(ref: WorkbenchRef, image: Image)

  case class Backend(t: BackendScope[Props, State]) extends ContainerRequestForm.ActionsBackend {

    def willMount(): Unit = t.props.ref.client.map { client =>
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

    def showCreateDialog(): Future[Unit] = Future {
      sendEvent(EventCategory.Image, EventAction.Show, "CreateDialog")
      t.modState(_.copy(showCreateDialog = true))
    }

    def removeImage(): Future[Unit] = {
      sendEvent(EventCategory.Image, EventAction.Remove)
      t.props.ref.client.get.removeImage(t.props.image.Id).map { info =>
        t.props.ref.show(ImagesPage)
      }.recoverWith {
        case ex: Exception =>
          val msg = s"${ex.getMessage}. You can also try to garbage collect unused containers and images."
          Future.successful(t.modState(_.copy(error = Some(msg))))
      }
    }


    def containerConfig: ContainerConfig = t.state.info match {
      case Some(info) => info.Config
      case None => ContainerConfig()
    }

    override def newContainerCreated(containerId: String) = {
      log.info(s"Container created $containerId")
      sendEvent(EventCategory.Image, EventAction.Start)
      t.props.ref.show(ContainerPage(containerId, t.props.ref))
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
    .componentWillMount(_.backend.willMount)
    .build


  def vdom(P: Props, S: State, B: Backend): ReactElement =
    <.div(
      S.error.map(Alert(_)),
      S.info.map(vdomInfo(_, S, P, B))
    )


  def vdomInfo(imageInfo: ImageInfo, S: State, P: Props, B: Backend) = {
    import util.StringUtils._
    val imageName = substringBefore(P.image.RepoTags.headOption.getOrElse(""), ":")
    val generalInfo = Map(
      "Id" -> P.image.id,
      "Name" -> imageName,
      "Tags" -> P.image.RepoTags.map(substringAfter(_, ":")).mkString(", ")
    )
    val executionInfo = Map(
      "Command" -> imageInfo.Config.cmd.mkString(" "),
      "Environment" -> imageInfo.Config.env.mkString(" "),
      "WorkingDir" -> imageInfo.Config.WorkingDir
    )
    val extraInfo = Map(
      "Contaner exposed ports" -> imageInfo.Config.exposedPorts.keySet.mkString(", "),
      "Author" -> imageInfo.Author,
      "Os" -> imageInfo.Os,
      "Created" -> P.image.created
    )

    <.div(
      InfoCard(generalInfo, InfoCard.SMALL, None, Seq.empty, vdomCommands(S, B)),
      InfoCard(executionInfo),
      InfoCard(extraInfo),
      vdomHistory(S.history),
      S.showCreateDialog ?= ContainerRequestForm(B, P.image, B.containerConfig, P.ref)
    )
  }

  def vdomCommands(state: State, B: Backend) =
    Some(<.div(^.className := "panel-footer",
      <.div(^.className := "btn-group btn-group-justified",
        <.div(^.className := "btn-group", Button("Deploy container", "glyphicon-play", "Create container using this image")(B.showCreateDialog)),
        <.div(^.className := "btn-group", Button("Remove", "glyphicon-trash")(B.removeImage))
        )
      )
    )


  def vdomHistory(history: Seq[ImageHistory]): ReactElement = {
    val values = history.map(row => Map( "Created By" -> row.CreatedBy, "Id" -> row.id, "Size" -> row.size, "Created" -> row.created))
    <.div(^.className := "container  col-sm-12",
      <.div(^.className := "panel panel-default  bootcards-summary",
        <.div(^.className := "panel-heading clearfix",
          <.h3(^.className := "panel-title pull-left",
            <.i(^.className := "fa fa-history"), " Creation History"
          )
        ),
        TableCard(values, "Created" -> "col-sm-2", "Size" -> "col-sm-2")
      )
    )

  }

}
