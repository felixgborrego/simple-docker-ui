package ui.pages

import api.{ConfigStorage, ConnectedStream}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model._
import ui.WorkbenchRef
import ui.widgets.{Alert, ContainersCard, InfoCard, TableCard}
import util.chrome.api._
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global


case object HomePage extends Page {

  val id = "HomePage"

  case class State(info: Option[DockerMetadata] = None, events: Seq[DockerEvent] = Seq.empty, error: Option[String] = None, stream: Option[ConnectedStream] = None)

  case class Props(ref: WorkbenchRef) {
    def url = ref.connection.map(_.url).getOrElse("''")
  }

  case class Backend(t: BackendScope[Props, State]) {
    def willMount(): Unit = t.props.ref.client.map { client =>

      def loadInfo() =
        client.metadata().map { info =>
          t.modState(s => s.copy(info = Some(info), error = None))
          loadEvents()
          loadContainersSize(info)
        }.onFailure {
          case ex: Exception =>
            log.error("HomePage", "Unable to get Metadata", ex)
            ConfigStorage.isRunningBoot2Docker.map {
              case false => s"Unable to connect to ${t.props.url}, is Docker daemon running?"
              case true => s"Unable to connect to ${t.props.url}, is Boot2docker/docker-machine running?"
            }.map { msg =>
              t.modState(s => s.copy(error = Some(msg)))
            }
        }

      def loadContainersSize(info: DockerMetadata) =
        client.containersRunningWithExtraInfo.map { containers =>
          t.modState { s =>
            s.copy(info = Some(info.copy(containers = containers)))
          }
        }
      
      def loadEvents() = {
        val stream = client.events { events => //streaming
          t.modState(s => s.copy(events = events))
        }
        t.modState(s => s.copy(stream = Some(stream)))
      }

      loadInfo()
    }

    def willUnMount(): Unit = t.state.stream.map(_.abort())

    def refresh() = willMount()
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
    .render((P, S, B) => vdom(S, P, B))
    .componentWillMount(_.backend.willMount)
    .componentWillUnmount(_.backend.willUnMount)
    .build

  def vdom(S: State, P: Props, B: Backend) = <.div(
    S.error.map(Alert(_, Some(P.ref.link(SettingsPage)))),
    S.info.map(vdomInfo(_, P.ref, B)),
    vdomEvents(S.events)
  )

  def vdomInfo(docker: DockerMetadata, ref: WorkbenchRef, B: Backend) = {
    val info = Map(
      "Connected to" -> docker.connection.url,
      "Version" -> s"${docker.version.Version} (api: ${docker.version.ApiVersion})",
      "Docker UI" -> chrome.runtime.getManifest().version
    )
    <.div(
      ContainersCard(docker, ref)(() => B.refresh()),
      InfoCard(info, InfoCard.SMALL, Some("System")),
      !docker.info.swarmMasterInfo.isEmpty ?= InfoCard(docker.info.swarmMasterInfo, InfoCard.SMALL, Some("Swarm Info")),
      !docker.info.swarmNodesDescription.isEmpty ?= docker.info.swarmNodesDescription.map { nodeInfo =>
        InfoCard(nodeInfo, InfoCard.SMALL, nodeInfo.keys.headOption)
      }
    )
  }


  def vdomEvents(events: Seq[DockerEvent]) = {
    val values = events.map(e => Map("Status" -> e.status, "Id" -> e.shortId, "From" -> e.from, "Time" -> e.since))
    values.nonEmpty ?= <.div(^.className := "container  col-sm-12",
      <.div(^.className := "panel panel-default  bootcards-summary",
        <.div(^.className := "panel-heading clearfix",
          <.h3(^.className := "panel-title pull-left",
            <.span(^.className := "glyphicon glyphicon-list"), " Events History"
          )
        ),
        TableCard(values)
      )
    )
  }
}

