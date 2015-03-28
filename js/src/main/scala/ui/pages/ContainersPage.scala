package ui.pages

import api.{ConfigStore, DockerClient}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model.{Connection, ConnectionError, Container, ContainersInfo}
import ui.Workbench
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global

object ContainersPage extends Page {

  val id = "Containers"

  case class State(info: ContainersInfo = ContainersInfo())

  case class Props(connection: Connection)

  case class Backend(t: BackendScope[Props, State]) {
    def willStart(): Unit = {
      DockerClient(t.props.connection).containersInfo().map { info =>
        t.modState(s => State(info))
      }.onFailure {
        case ex: Exception =>
          log.error("Unable to get containers", ex)
          Workbench.error(ConnectionError(ex.getMessage))
      }
    }
  }

  def component() = {
    val props = Props(ConfigStore.connection)
    ContainersPageRender.component(props)
  }
}

object ContainersPageRender {

  import ui.pages.ContainersPage._

  val component = ReactComponentB[Props]("ContainerPage")
    .initialState(State())
    .backend(new Backend(_))
    .render((P, S, B) => {
    vdom(S)
  }).componentWillMount(_.backend.willStart())
    .build

  def vdom(state: State) = <.div(
    table(true,"Container Running", state.info.running),
    table(false,"History", state.info.history)
  )

  def table(showLinks: Boolean, title: String, containers: Seq[Container]) = <.div(^.className := "container  col-sm-11",
    <.div(^.className := "panel panel-default  bootcards-summary",
      <.div(^.className := "panel-heading clearfix",
        <.h3(^.className := "panel-title pull-left")(title),
        <.a(^.className := "btn pull-right glyphicon glyphicon-refresh", ^.href := "#")
      ),
      <.table(^.className := "table table-hover",
        <.thead(
          <.tr(
            <.th("Id"),
            <.th("Image"),
            <.th("Command"),
            <.th("Ports"),
            <.th("Created"),
            <.th("Status")
          )
        ),
        <.tbody(
          containers.map { c =>
            <.tr(
              <.td(if(showLinks) Workbench.link(ContainerPage(c.Id))(c.id) else None),
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