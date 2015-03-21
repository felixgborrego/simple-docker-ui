package ui.pages

import api.{ConfigStore, DockerClient}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model.{Connection, Container}
import ui.Links
import ui.widgets.Alert
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global

object ContainersPage {

  case class State(containers: Seq[Container] = Seq.empty, error: Option[String] = None)

  case class Props(connection: Connection)

  case class Backend(t: BackendScope[Props, State]) {

    def willStart(): Unit = {
      // get running containers
      DockerClient(t.props.connection).containers(all = false).map { containers =>
        t.modState(s => State(containers))
      }.onFailure {
        case ex: Exception =>
          log.error("Unable to get containers", ex)
          t.modState(s => State(Seq.empty, Some(ex.getMessage)))
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

  def dom(containers: Seq[Container]) = <.div("Containers TODO")

  val component = ReactComponentB[Props]("ContainerPage")
    .initialState(State())
    .backend(new Backend(_))
    .render((P, S, B) => {
    if (S.error.isDefined) {
      Alert("Unable to connect to " + P.connection.url, S.error.get, Some(Links.settingsLink))
    } else {
      dom(S.containers)
    }

  }).componentWillMount(_.backend.willStart())
    .build


}