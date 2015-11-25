package ui.widgets


import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model._
import org.scalajs.dom
import ui.WorkbenchRef
import util.FutureUtils
import util.PullEventsCustomParser.EventStatus
import util.googleAnalytics._

import scala.concurrent.{Promise, Future}

object PullModalDialog {

  trait ActionsBackend {
    def imagePulled()
  }

  case class ProgressState(running: Boolean = false,
                           finished: Boolean = false,
                           events: Seq[EventStatus] = Seq.empty) {

    val done = !running && events.nonEmpty

    def message = events.headOption.map(_.status)
  }


  case class State(progress: ProgressState = ProgressState())

  case class Props(actionsBackend: ActionsBackend, image: ImageSearch, ref: WorkbenchRef)

  case class Backend(t: BackendScope[Props, State]) {

    def didMount(): Unit = {
      // The Dialog is not a react component
      dom.document.getElementById("open-modal-dialog").asInstanceOf[dom.raw.HTMLButtonElement].click()
    }

    def willReceiveProps(newProps: Props): Unit = {
      if (newProps.image.name != t.props.image.name) {
        t.modState(s => s.copy(progress = s.progress.copy(events = Seq.empty, running = false, finished = false)))
      }
    }

    val RefreshMilliseconds = 1000
    def pullImage(): Future[Unit] = {
      val p = Promise[Unit]()

      t.props.ref.client.map { client =>
        sendEvent(EventCategory.Image, EventAction.Pull, "StartPull")
        val stream = client.pullImage(t.props.image.name + ":latest")
        t.modState(s => s.copy(progress = s.progress.copy(running = true)))
        def refresh(): Unit = FutureUtils.delay(RefreshMilliseconds){ () =>
          val (activeEvents, finishedEvents) = stream.refreshEvents()
          t.modState(s => s.copy(progress = s.progress.copy(events = activeEvents, running = true)))
          if (!stream.done && t.isMounted()) refresh()
          else {
            t.modState(s => s.copy(progress = s.progress.copy(running = false, finished = true)))
            t.props.actionsBackend.imagePulled()
            sendEvent(EventCategory.Image, EventAction.Pull, "FinishPulled")
            p.success({})
          }
        }
        refresh()
      }
      p.future
    }
  }

  def apply(actionsBackend: ActionsBackend, image: ImageSearch, ref: WorkbenchRef) = {
    val props = Props(actionsBackend, image, ref)
    PullModalDialogRender.component(props)
  }
}


object PullModalDialogRender {
  import PullModalDialog._

  val component = ReactComponentB[Props]("PullModalDialog")
    .initialState(State())
    .backend(new Backend(_))
    .render((P, S, B) => vdom(P, S, B))
    .componentDidMount(_.backend.didMount)
    .componentWillReceiveProps((scope, newProps) => scope.backend.willReceiveProps(newProps))
    .build


  var data_dismiss = "data-dismiss".reactAttr
  val aria_valuenow = "aria-valuenow".reactAttr
  val aria_valuemin = "aria-valuemin".reactAttr
  val aria_valuemax = "aria-valuemax".reactAttr

  val data_toggle = "data-toggle".reactAttr
  val data_target = "data-target".reactAttr


  def vdom(P: Props, S: State, B: Backend) = {
    val title = P.image.name
    val finished = S.progress.finished
    val running = S.progress.running

    <.div(^.className := "modal fade", ^.id := "editModal", ^.role := "dialog",
      <.div(^.className := "modal-dialog",
        <.div(^.className := "modal-content",
          <.div(^.className := "modal-header",
            <.div(^.className := "btn-group pull-left",
              (!running) ?= <.button(^.className := "btn btn-danger", data_dismiss := "modal", "Close")
            ),
            <.div(^.className := "btn-group pull-right",
              <.button(^.id := "open-modal-dialog", ^.display := "none", data_toggle := "modal", data_target := "#editModal", "Open"),
              !finished ?= Button( "Pull Image","glyphicon-cloud-download")(B.pullImage)
            ),
            <.h3(^.className := "modal-title")(title)
          ),
          <.div(^.className := "modal-body",
            S.progress.message.map(<.i(_)),
            <.div(^.className := "list-group",
              <.div(^.className := "list-group-item noborder",
                <.i(^.className := "list-group-item-text")("Description"),
                <.p(^.className := "list-group-item-heading", ^.wordWrap := "break-word", P.image.description)
              ),
              (running || finished) ?= table(S.progress.events)
            )
          )
          ,
          <.div(^.className := "modal-footer",
            P.image.is_official ?= <.i(^.className := "glyphicon glyphicon-bookmark pull-left"),
            (P.image.star_count == 0) ?= <.div(^.className := "pull-right", <.i(^.className := "glyphicon glyphicon-star-empty"), P.image.star_count),
            (P.image.star_count > 0) ?= <.div(^.className := "pull-right", <.i(^.className := "glyphicon glyphicon-star"), P.image.star_count),
            <.a(^.className := "btn btn-link btn-xs pull-right", ^.target := "_blank", ^.href := s"https://hub.docker.com/search/?q=${P.image.name}&searchfield:=")("View in Docker.com")

          )
        )
      )
    )
  }

  def table(events: Seq[EventStatus]) =
    <.div(
      <.br(),
      <.div(^.className := "panel panel-default  bootcards-summary",
        <.table(^.className := "table table-condensed table-hover table-striped fixed-table",
          <.thead(
            <.tr(
              <.th(^.className := "col-sm-2", "Id"),
              <.th(^.className := "col-sm-7", "Status"),
              <.th(^.className := "col-sm-3", "Progress")
            )
          ),
          <.tbody(
            events.map { e =>
              <.tr(
                <.td(e.id),
                <.td(e.status, <.i(e.progressText)),
                <.td(
                  (e.progressValue > 0) ?=
                    <.div(^.className := "progress",
                      <.div(^.id := "pullProgressBar", ^.className := "progress-bar progress-bar-striped active",
                        ^.role := "progressbar", ^.width := s"${e.progressValue}%",
                        aria_valuenow := "0", aria_valuemin := "0", aria_valuemax := "100",
                        s"${e.progressValue}%"
                      )
                    )

                )
              )
            }
          )
        )
      )
    )

}
