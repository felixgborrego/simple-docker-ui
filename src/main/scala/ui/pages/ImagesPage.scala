package ui.pages

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactEventI}
import model._
import ui.WorkbenchRef
import ui.widgets.PullModalDialog.ActionsBackend
import ui.widgets.{Alert, PullModalDialog}
import util.logger._
import util.StringUtils._

import scala.concurrent.ExecutionContext.Implicits.global

object ImagesPage extends Page {

  val id = "Images"

  case class State(localImages: Seq[Image] = Seq.empty,
                   remoteImages: Seq[ImageSearch] = Seq.empty,
                   searching: Boolean = false,
                   imageToPull: Option[ImageSearch] = None,
                   error: Option[String] = None) {
    def searchIcon =
      if (searching)
        "glyphicon glyphicon-refresh glyphicon-spin"
      else
        "glyphicon glyphicon-search"

    def totalSize = bytesToSize(localImages.map(_.VirtualSize).sum)
  }

  case class Props(ref: WorkbenchRef)

  case class Backend(t: BackendScope[Props, State]) extends ActionsBackend {
    def willMount(): Unit = t.props.ref.client.map { client =>
      client.images().map { images =>
        t.modState(s => s.copy(localImages = images))
      }.onFailure {
        case ex: Exception =>
          log.error("ImagesPage", "Unable to get Metadata", ex)
          t.modState(s => s.copy(error = Some(s"Unable to connect")))
      }
    }

    val MinTextSize = 3

    def onTextChange(e: ReactEventI): Unit = t.props.ref.client.map { client =>
      val text = e.target.value
      if (text.isEmpty) {
        t.modState(s => s.copy(remoteImages = Seq.empty, searching = false))
      } else if (text.length > MinTextSize || t.state.remoteImages.nonEmpty) {
        t.modState(s => s.copy(searching = true))
        client.imagesSearch(text).map { images =>
          log.info(s"images ${images.size}")
          t.modState(s => s.copy(remoteImages = images, searching = false))
        }.onFailure {
          case ex: Exception =>
            log.error("ImagesPage", "Unable to get Metadata", ex)
            t.modState(s => s.copy(error = Some(s"Unable to connect")))
        }
      }
    }

    def showDetail(image: ImageSearch) = {
      t.modState(s => s.copy(imageToPull = Some(image)))
    }

    def refresh() = willMount()

    override def imagePulled(): Unit = refresh()

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
    .render((P, S, B) => vdom(S, P, B))
    .componentWillMount(_.backend.willMount)
    .build


  def vdom(S: State, P: Props, B: Backend) = <.div(
    S.error.map(Alert(_)),
    remoteSearch(S, P, B),
    table("Local images", S, P),
    S.imageToPull.map(image => PullModalDialog(B, image, P.ref))
  )

  def table(title: String, S:State, P: Props) =
    <.div(^.className := "container  col-sm-12",
      <.div(^.className := "panel panel-default  bootcards-summary",
        <.div(^.className := "panel-heading clearfix",
          <.h3(^.className := "panel-title pull-left")(<.span(^.className := "glyphicon glyphicon-hdd"), " " + title),
          <.span(^.className := "panel-title pull-right", "Total: ", S.totalSize)
        ),
        <.table(^.className := "table table-hover table-striped",
          <.thead(
            <.tr(
              <.th("Id"),
              <.th("Tags"),
              <.th("Created"),
              <.th("Size")
            )
          ),
          <.tbody(
            S.localImages.map { img =>
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

  def remoteSearch(S: State, P: Props, B: Backend) =
    <.div(
      <.div(^.className := "container  col-sm-2"),
      <.div(^.className := "container  col-sm-8",
        <.div(^.className := "bootcards-list ",
          <.div(^.className := "panel panel-default",
            <.div(^.className := "panel-body",
              <.form(^.className := "form-horizontal",
                <.div(^.className := "form-group",
                  <.label(^.className := "col-sm-3 control-label")(<.span(^.className := S.searchIcon), " Registry Hub"),
                  <.div(^.className := "col-sm-9",
                    <.input(^.`type` := "text", ^.className := "form-control", ^.placeholder := "Search Images...", ^.onChange ==> B.onTextChange)
                  )
                )
              )
            ),
            remoteList(S, P, B)
          )
        )
      ),
      <.div(^.className := "container  col-sm-2")
    )


  var data_toggle = "data-toggle".reactAttr
  val data_target = "data-target".reactAttr


  def remoteList(S: State, P: Props, B: Backend) =
    <.div(^.className := "list-group",
      S.remoteImages.map { image =>
        <.a(^.className := "list-group-item", ^.onClick --> B.showDetail(image), data_toggle := "modal", data_target := "#editModal",
          image.is_official ?= <.i(^.className := "glyphicon glyphicon-bookmark pull-left"),
          (image.star_count == 0) ?= <.i(^.className := "glyphicon glyphicon-star-empty pull-right")(image.star_count),
          (image.star_count > 0) ?= <.i(^.className := "glyphicon glyphicon-star pull-right")(image.star_count),
          <.h4(^.className := "list-group-item-heading", image.name),
          <.p(^.className := "list-group-item-text", image.description)
        )

      }
    )


}