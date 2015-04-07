package ui.widgets


import japgolly.scalajs.react.vdom.prefix_<^._
import model._
import ui.pages.ImagesPage._
import util.stringUtils._

object PullModalDialog {

  case class Event(id: String, var progressValue: Int, var status: String, var progressText: String)

  case class ProgressState(remoteImageSelected: ImageSearch,
                           running: Boolean = false,
                           finished: Boolean = false,
                           events: Seq[PullProgressEvent] = Seq.empty) {


    val done = !running && events.nonEmpty

    // transform events and grouping duplicates which are together
    def progressEvents: Seq[Event] = events.foldLeft((Seq.empty[Event])) { case (eventsGrouped, rawEvent) =>
      if (eventsGrouped.map(_.id).contains(rawEvent.id)) {
        // reuse previous
        val last = eventsGrouped.last
        last.progressValue = progressValue(rawEvent)
        last.progressText = progressText(rawEvent)
        last.status = rawEvent.status
        eventsGrouped
      } else {
        eventsGrouped :+ Event(rawEvent.id, progressValue(rawEvent), rawEvent.status, progressText(rawEvent))
      }
    }.reverse

    def description = remoteImageSelected.description

    def progressValue(event: PullProgressEvent): Int = {
      if (event.progressDetail.total == 0) 0
      else ((event.progressDetail.current.toDouble / event.progressDetail.total.toDouble) * 100).toInt
    }

    def progressText(event: PullProgressEvent) =
      substringAfter(event.progress, "]")

    def message = events.lastOption.map(_.status)
  }

}

object PullModalDialogRender {

  var data_dismiss = "data-dismiss".reactAttr
  val aria_valuenow = "aria-valuenow".reactAttr
  val aria_valuemin = "aria-valuemin".reactAttr
  val aria_valuemax = "aria-valuemax".reactAttr
  val custom_style = "style".reactAttr


  def vdom(S: State, B: Backend) = {
    val progress = S.progressState
    val title = progress.map(_.remoteImageSelected.name).getOrElse("")
    val finished = progress.map(_.finished).getOrElse(false)
    val running = progress.map(_.running).getOrElse(false)

    <.div(^.className := "modal fade", ^.id := "editModal", ^.role := "dialog",
      <.div(^.className := "modal-dialog",
        <.div(^.className := "modal-content",
          <.div(^.className := "modal-header",
            <.div(^.className := "btn-group pull-left",
              (!running) ?= <.button(^.className := "btn btn-danger", data_dismiss := "modal", "Close")
            ),
            <.div(^.className := "btn-group pull-right",
              (!finished && !running) ?= <.button(^.className := "btn btn-primary", ^.onClick --> B.pullImage, "Pull Image")
            ),
            <.h3(^.className := "modal-title")(title)
          ),
          progress.map { p =>
            <.div(^.className := "modal-body",
              p.message.map(<.i(_)),
              <.div(^.className := "list-group",
                <.div(^.className := "list-group-item noborder",
                  <.i(^.className := "list-group-item-text")("Description"),
                  <.p(^.className := "list-group-item-heading", ^.wordWrap := "break-word", p.description)
                ),
                p.running ?= table(p.progressEvents)
              )
            )
          },
          <.div(^.className := "modal-footer",
            progress.map { p => Seq(
              p.remoteImageSelected.is_official ?= <.i(^.className := "glyphicon glyphicon-bookmark pull-left"),
              (p.remoteImageSelected.star_count == 0) ?= <.i(^.className := "glyphicon glyphicon-star-empty pull-right")(p.remoteImageSelected.star_count),
              (p.remoteImageSelected.star_count > 0) ?= <.i(^.className := "glyphicon glyphicon-star pull-right")(p.remoteImageSelected.star_count)
            )
            },
            <.a(^.className := "btn btn-link btn-xs pull-right", ^.target := "_blank", ^.href := s"https://registry.hub.docker.com/search?q:=${progress.map(_.remoteImageSelected.name).getOrElse("")}&searchfield:=")("View in Docker.com")
          )
        )
      )
    )
  }

  def table(events: Seq[PullModalDialog.Event]) =
    <.div(
      <.br(),
      <.div(^.className := "panel panel-default  bootcards-summary",
        <.table(^.className := "table table-hover table-striped",
          <.thead(
            <.tr(
              <.th("Id"),
              <.th("Status"),
              <.th("Progress")
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
