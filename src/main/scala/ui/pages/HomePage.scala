package ui.pages

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model._
import ui.WorkbenchRef
import ui.widgets.{Alert, ContainersCard, InfoCard}
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global


case object HomePage extends Page {

  val id = "HomePage"

  case class State(info: Option[DockerMetadata] = None, error: Option[String] = None)

  case class Props(ref: WorkbenchRef)

  case class Backend(t: BackendScope[Props, State]) {
    def willStart(): Unit = {
      t.props.ref.client.map { client =>
        client.metadata().map { docker =>
          t.modState(s => State(Some(docker)))
        }.onFailure {
          case ex: Exception =>
            log.error("Unable to get Metadata", ex)
            t.modState(s => s.copy(error = Some("Unable to get data from " + t.props.ref.connection.fold("''")(_.url))))
        }
      }
    }
  }

  def component(ref: WorkbenchRef) = {
    val props = Props(ref)
    HomePageRender.component(props)
  }

}

object HomePageRender {

  import ui.pages.HomePage._

  val component = ReactComponentB[Props]("HomePage")
    .initialState(State())
    .backend(new Backend(_))
    .render((P, S, B) => {
    vdom(S, P)
  }).componentWillMount(_.backend.willStart())
    .build

  def vdom(S: State, P: Props) = <.div(
    S.error.map(Alert(_, Some(P.ref.link(SettingsPage)))),
    S.info.map(vdomInfo(_, P.ref))
  )

  def vdomInfo(docker: DockerMetadata, ref: WorkbenchRef) = {
    val info = Map(
      "Connected to" -> docker.connection.url,
      "Version" -> (docker.version.Version + "(api: " + docker.version.ApiVersion + ")")
    )
    <.div(
      ContainersCard(docker, ref),
      InfoCard(info, InfoCard.SMALL, Some("System"))
    )
  }
}

