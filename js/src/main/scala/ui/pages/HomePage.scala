package ui.pages

import api.{ConfigStore, DockerClient}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model._
import ui.Workbench
import ui.widgets.{InfoCard, ContainersCard}
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global


case object HomePage extends Page {

  val id = "HomePage"

  case class State(info: Option[DockerMetadata] = None)

  case class Props(connection: Connection)

  case class Backend(t: BackendScope[Props, State]) {

    def willStart(): Unit = {

      DockerClient(t.props.connection).metadata().map { docker =>
        t.modState(s => State(Some(docker)))
      }.onFailure {
        case ex: Exception =>
          log.error("Unable to get Metadata", ex)
          Workbench.error(ConnectionError(ex.getMessage))
      }
    }
  }


  def component() = {
    val props = Props(ConfigStore.connection)
    HomePageRender.component(props)
  }
}

object HomePageRender {

  import ui.pages.HomePage._

  val component = ReactComponentB[Props]("HomePage")
    .initialState(State())
    .backend(new Backend(_))
    .render((P, S, B) => {
    S.info match {
      case None =>
        <.div("Connecting... to " + P.connection.url)
      case Some(info) => vdom(info)
    }
  }).componentWillMount(_.backend.willStart())
    .build

  def vdom(docker: DockerMetadata) = {
    val info = Map(
      "Connected to" -> docker.connection.url,
      "Version" -> (docker.version.Version + "(api: " + docker.version.ApiVersion + ")")
    )
    <.div(
      ContainersCard(docker),
      InfoCard(info, InfoCard.SMALL, Some("System"))
    )
  }
}
