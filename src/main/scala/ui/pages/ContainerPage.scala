package ui.pages

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import model.{ContainerInfo, ContainerTop}
import org.scalajs.dom.raw.WebSocket
import ui.WorkbenchRef
import ui.widgets._
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ContainerPage {

  case class State(info: Option[ContainerInfo] = None,
                   top: Option[ContainerTop] = None,
                   error: Option[String] = None,
                   actionStopping: Boolean = false,
                   tabSelected: ContainerPageTab = TabNone)

  case class Props(ref: WorkbenchRef, containerId: String)

  case class Backend(t: BackendScope[Props, State]) {

    def willStart(): Unit = t.props.ref.client.map { client =>
      val result = for {
        info <- client.containerInfo(t.props.containerId)
        top <- if (info.State.Running) client.top(t.props.containerId).map(Some(_)) else Future(Option.empty)
      } yield t.modState(s => s.copy(Some(info), top))

      result.onFailure {
        case ex: Exception =>
          log.error("Unable to get Metadata", ex)
          t.modState(s => s.copy(None, None, Some("Unable to get data: " + ex.getMessage)))
      }

      result.onSuccess {
        case _ => t.modState(s => s.copy(tabSelected = TabTerminal))
      }
    }

    def stop() =
      t.props.ref.client.get.stopContainer(t.props.containerId).map { info =>
        willStart()
      }

    def start() =
      t.props.ref.client.get.startContainer(t.props.containerId).map { info =>
        t.modState(s => s.copy(tabSelected = TabNone))
        willStart()
      }

    def showTab(tab: ContainerPageTab) = {
      t.modState(s => s.copy(tabSelected = tab))
    }

    def attach(): WebSocket =
      t.props.ref.client.get.attachToContainer(t.props.containerId)

  }


  def apply(containerId: String, ref: WorkbenchRef) = new Page {
    val id = ContainersPage.id

    def component(ref: WorkbenchRef) = {
      val props = Props(ref, containerId)
      ContainerPageRender.component(props)
    }
  }
}

object ContainerPageRender {

  import ui.pages.ContainerPage._

  val component = ReactComponentB[Props]("ContainerPage")
    .initialState(State())
    .backend(new Backend(_))
    .render((P, S, B) => vdom(P, S, B))
    .componentWillMount(_.backend.willStart())
    .build


  def vdom(P: Props, S: State, B: Backend): ReactElement =
    <.div(
      S.error.map(Alert(_, Some(P.ref.link(SettingsPage)))),
      S.info.map(vdomInfo(_, S, P, B)),
      vDomTabs(S, B)
    )


  def vdomInfo(containerInfo: ContainerInfo, S: State, P: Props, B: Backend) = {
    val generalInfo = Map(
      "Id / Name" -> containerInfo.id,
      "Image" -> containerInfo.image,
      "Created" -> containerInfo.created,
      "Status" -> (if (containerInfo.State.Running) "Running" else "Stopped")
    )
    val executionInfo = Map(
      "Command" -> containerInfo.Config.cmd.mkString(" "),
      "Arguments" -> containerInfo.Args.mkString(" "),
      "Environment" -> containerInfo.Config.env.mkString(" "),
      "WorkingDir" -> containerInfo.Config.WorkingDir
    )
    val ports = containerInfo.NetworkSettings.ports.map {
      case (external, internal) => external + " -> " + internal
    }.mkString(", ")

    val networkInfo = Map(
      "Ip" -> containerInfo.NetworkSettings.IPAddress,
      "Port Mapping" -> ports,
      "Volumes" -> "---"
    )

    <.div(
      InfoCard(generalInfo, InfoCard.SMALL, None,
        vdomCommands(S, B)
      ),
      InfoCard(executionInfo),
      InfoCard(networkInfo, InfoCard.SMALL, None, vdomServiceUrl(containerInfo, P))
    )
  }

  def vdomServiceUrl(containerInfo: ContainerInfo, P: Props) = {
    val ip = P.ref.connection.map(_.ip).getOrElse("")
    containerInfo.NetworkSettings.ports.map {
      case (external, internal) => ip + ":" + external
    }
  }.map(url => <.div(^.className := "panel-footer", <.a(^.href := s"http://$url", ^.target := "_blank")(url))).headOption

  def vdomCommands(state: State, B: Backend) =
    Some(<.div(^.className := "panel-footer",
      <.div(^.className := "btn-group btn-group-justified",
        <.div(^.className := "btn-group",
          state.info.map {
            info =>
              if (info.State.Running)
                Button("Stop", "glyphicon-stop")(B.stop)
              else
                Button("Star", "glyphicon-play")(B.start)
          }
        )
      )
    ))


  def vDomTabs(S: State, B: Backend) = {
    val stdin = S.info.map(_.Config.AttachStdin).getOrElse(true)

    <.div(^.className := "container  col-sm-12",
      <.div(^.className := "panel panel-default",
        <.ul(^.className := "nav nav-tabs",
          <.li(^.role := "presentation", (S.tabSelected == TabTerminal) ?= (^.className := "active"),
            <.a(^.onClick --> B.showTab(TabTerminal), "Terminal")
          ),
          S.top.map(s =>
            <.li(^.role := "presentation", (S.tabSelected == TabTop) ?= (^.className := "active"),
              <.a(^.onClick --> B.showTab(TabTop), "Top")
            ))
        ),
        (S.tabSelected == TabTerminal) ?= TerminalCard(stdin)(B.attach),
        (S.tabSelected == TabTop && S.top.isDefined) ?= S.top.map(vdomTop).get

      )
    )
  }

  def vdomTop(top: ContainerTop): ReactElement = {
    val keys = top.Titles
    val values = top.Processes.map(data => keys.zip(data).toMap)
    TableCard(values)
  }

}

sealed trait ContainerPageTab

case object TabNone extends ContainerPageTab

case object TabTerminal extends ContainerPageTab

case object TabTop extends ContainerPageTab