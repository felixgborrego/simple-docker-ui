package ui.pages

import api.{ConfigStore, DockerClient}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model.{ContainersInfo, Connection, Container}
import ui.Links
import ui.widgets.Alert
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global

object ContainersPage {

  case class State(info: ContainersInfo = ContainersInfo(), error: Option[String] = None)

  case class Props(connection: Connection)

  case class Backend(t: BackendScope[Props, State]) {

    def willStart(): Unit = {
      DockerClient(t.props.connection).containersInfo().map { info =>
        t.modState(s => State(info))
      }.onFailure {
        case ex: Exception =>
          log.error("Unable to get containers", ex)
          t.modState(s => State(ContainersInfo(), Some(ex.getMessage)))
      }
    }
  }

  def apply() = {
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
    if (S.error.isDefined) {
      Alert("Internal error " + P.connection.url, S.error.get, Some(Links.settingsLink))
    } else {
      vdom(S)
    }
  }).componentWillMount(_.backend.willStart())
    .build

  def vdom(state: State) = <.div(
    table("Container Running", state.info.running),
    table("History", state.info.history)
  )

  def table(title: String, containers: Seq[Container]) = <.div(^.className := "container  col-sm-11",
    <.div(^.className := "panel panel-default  bootcards-summary",
      <.div(^.className := "panel-heading clearfix",
        <.h3(^.className := "panel-title pull-left")(title),
        <.a(^.className := "btn pull-right glyphicon glyphicon-refresh", ^.href := "#")
      ),
      <.table(^.className := "table",
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
            <.tr(^.className := "info",
              <.td(Links.containerLink(c.Id)(c.id)),
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