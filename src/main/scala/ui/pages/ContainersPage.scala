package ui.pages

import api.DockerClientConfig
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model.{Container, ContainersInfo}
import ui.WorkbenchRef
import ui.widgets.{Alert, Button}
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global

object ContainersPage extends Page {

  val id = "Containers"

  case class State(info: ContainersInfo = ContainersInfo(), error: Option[String] = None)

  case class Props(ref: WorkbenchRef)

  case class Backend(t: BackendScope[Props, State]) {
    def willMount(): Unit = t.props.ref.client.map { client =>
      client.containersInfo().map { info =>
        t.modState(s => State(info))
      }.onFailure {
        case ex: Exception =>
          log.error("ContainersPage", "Unable to get Metadata", ex)
          t.modState(s => s.copy(error = Some(s"Unable to connect")))
      }
    }

    def refresh() = willMount()

    def garbageCollection() = t.props.ref.client.get.garbageCollectionContainers().map { info =>
      t.modState(s => State(info))
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
    table("glyphicon glyphicon-transfer", "Container Running", S.info.running, showGC = false, P, B),
    table("glyphicon glyphicon-equalizer", "History", S.info.history, showGC = true, P, B)
  )

  def table(iconClassName: String, title: String, containers: Seq[Container], showGC: Boolean, props: Props, B: Backend) =
    <.div(^.className := "container  col-sm-12",
      <.div(^.className := "panel panel-default  bootcards-summary",
        <.div(^.className := "panel-heading clearfix",
          <.h3(^.className := "panel-title pull-left")(<.span(^.className := iconClassName), " " + title),
          (showGC && containers.size > DockerClientConfig.KeepInGarbageCollection) ?= <.span(^.className := "pull-right",
            Button("Garbage Collection", "glyphicon-trash",
              "Removes all unused containers, keeping the 10 recent once")(B.garbageCollection)
          )
        ),
        <.table(^.className := "table table-hover table-striped break-text",
          <.thead(
            <.tr(
              <.th("Id", ^.className:="col-xs-1"),
              <.th("Image",^.className:="col-xs-2"),
              <.th("Command",^.className:="col-xs-4"),
              <.th("Ports",^.className:="col-xs-2"),
              <.th("Created",^.className:="col-xs-2"),
              <.th("Status",^.className:="col-xs-1")
            )
          ),
          <.tbody(
            containers.map { c =>
              <.tr(
                <.td(props.ref.link(ContainerPage(c.Id, props.ref))(c.id)),
                <.td(c.Image),
                <.td(c.Command),
                <.td(c.ports.map(<.div(_))),
                <.td(c.created),
                <.td(c.Status)
              )
            }
          )
        )
      )
    )
}