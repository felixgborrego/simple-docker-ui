package ui.pages

import api.DockerClientConfig
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model.Container
import ui.WorkbenchRef
import ui.widgets.{Alert, Button}
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ContainersPage extends Page {

  val id = "Containers"

  case class State(running: Seq[Container] = Seq.empty, history: Seq[Container] = Seq.empty, error: Option[String] = None)

  case class Props(ref: WorkbenchRef)

  case class Backend(t: BackendScope[Props, State]) {
    def willMount(): Unit = refresh()

    def refresh():Future[Unit] = t.props.ref.client.map { client =>
      client.containerRunning().map { running =>
        t.modState(s => s.copy(running = running, error = None))
        client.containersHistory(running).map { history =>
          t.modState(s => s.copy(history = history))
        }: Unit
      }.recover{
        case ex: Exception =>
          log.error("ContainersPage", "Unable to get Metadata", ex)
          t.modState(s => s.copy(error = Some(s"Unable to connect")))
      }
    }.getOrElse(Future.successful{})

    def garbageCollection() = t.props.ref.client.get.garbageCollectionContainers().flatMap { _ =>
      refresh()
    }

  }

  def component(ref: WorkbenchRef) = {
    val props = Props(ref)
    ContainersPageRender.component(props)
  }
}

object ContainersPageRender {

  import ui.pages.ContainersPage._

  val component = ReactComponentB[Props]("ContainerPage")
    .initialState(State())
    .backend(new Backend(_))
    .render((P, S, B) => vdom(S, P, B))
    .componentWillMount(_.backend.willMount)
    .build

  def vdom(S: State, P: Props, B: Backend) = <.div(
    S.error.map(Alert(_, Some(P.ref.link(SettingsPage)))),
    table("glyphicon glyphicon-transfer", "Container Running", S.running, showGC = false, showRefresh=true, P, B),
    table("glyphicon glyphicon-equalizer", "History", S.history, showGC = true, showRefresh = false, P, B)
  )

  def table(iconClassName: String, title: String, containers: Seq[Container], showGC: Boolean, showRefresh: Boolean, props: Props, B: Backend) =
    <.div(^.className := "container  col-sm-12",
      <.div(^.className := "panel panel-default  bootcards-summary",
        <.div(^.className := "panel-heading clearfix",
          <.h3(^.className := "panel-title pull-left")(<.span(^.className := iconClassName), " " + title),
          (showGC && containers.size > DockerClientConfig.KeepInGarbageCollection) ?= <.span(^.className := "pull-right",
            Button("Garbage Collection", "glyphicon-trash",
              "Removes all unused containers, keeping the 10 recent once")(B.garbageCollection)
          ),
          showRefresh?= <.span(^.className := "pull-right",
            Button("Refresh", "glyphicon-refresh",
              "Refresh containers")(B.refresh)
          )
        ),
        <.table(^.className := "table table-hover table-striped break-text",
          <.thead(
            <.tr(
              <.th("Id", ^.className:="col-xs-2 col-sm-2 col-md-2"),
              <.th("Image",^.className:="col-xs-4 col-sm-3 col-md-3"),
              <.th("Command",^.className:="hidden-xs hidden-sm col-md-2"),
              <.th("Ports",^.className:="hidden-xs col-sm-2 col-md-2"),
              <.th("Created",^.className:="col-xs-2 col-sm-2 col-md-2"),
              <.th("Status",^.className:="col-xs-4 col-sm-3 col-md-2")
            )
          ),
          <.tbody(
            containers.map { c =>
              <.tr(
                <.td(props.ref.link(ContainerPage(c.Id, props.ref))(c.id),
                  <.div(<.small(c.Names.headOption.map(_.replaceFirst("/",""))))
                ),
                <.td(c.Image),
                <.td(^.className:="hidden-xs hidden-sm",c.Command),
                <.td(^.className:="hidden-xs", c.ports.map(<.div(_))),
                <.td(^.className:="break-by-word", c.created),
                <.td(^.className:="break-by-word", c.Status)
              )
            }
          )
        )
      )
    )
}