package ui.pages

import api.ConnectedStream
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import model.stats.ContainerStats
import model.{ContainerInfo, ContainerTop, FileSystemChange}
import org.scalajs.dom.raw.WebSocket
import ui.WorkbenchRef
import ui.widgets.TerminalCard.TerminalInfo
import ui.widgets._
import util.googleAnalytics._
import util.logger._
import util.{CopyPasteUtil, StringUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ContainerPage {

  case class State(info: Option[ContainerInfo] = None,
                   top: Option[ContainerTop] = None,
                   changes: Seq[FileSystemChange] = Seq.empty,
                   error: Option[String] = None,
                   actionStopping: Boolean = false,
                   stats: Option[ContainerStats] = None,
                   statsConnectedStream: Option[ConnectedStream] = None,
                   tabSelected: ContainerPageTab = TabNone)

  case class Props(ref: WorkbenchRef, containerId: String)

  case class Backend(t: BackendScope[Props, State]) {

    def willMount(): Unit = t.props.ref.client.map { client =>
      val result = for {
        info <- client.containerInfo(t.props.containerId)
        top <- if (info.State.Running) client.top(t.props.containerId).map(Some(_)) else Future(Option.empty)
        changes <- client.containerChanges(t.props.containerId)
      } yield t.modState(s => s.copy(Some(info), top, changes, error = None))


      result.onFailure {
        case ex: Exception =>
          log.error("ContainerPage", s"Unable to get containerInfo for ${t.props.containerId}", ex)
          t.modState(s => s.copy(error = Some(s"Unable to connect")))
      }

      result.onSuccess { case _ =>
        t.modState(s => s.copy(tabSelected = TabTerminal))
      }
    }

    def willUnMount(): Unit = stopStats()

    def stop() =
      t.props.ref.client.get.stopContainer(t.props.containerId).map { info =>
        sendEvent(EventCategory.Container, EventAction.Stop)
        refresh()
      }

    def start() =
      t.props.ref.client.get.startContainer(t.props.containerId).map { info =>
        sendEvent(EventCategory.Container, EventAction.Start)
        t.modState(s => s.copy(tabSelected = TabNone))
        refresh()
      }.recover {
        case ex: Exception => t.modState(s => s.copy(error = Some(ex.getMessage)))
      }

    def refresh() = willMount()

    def remove() =
      t.props.ref.client.get.removeContainer(t.props.containerId).map { info =>
        t.props.ref.show(ContainersPage)
      }

    def showTab(tab: ContainerPageTab) = {
      tab match {
        case TabStats => startStats()
        case _ => stopStats()
      }
      t.modState(s => s.copy(tabSelected = tab))
    }

    def attach(): WebSocket =
      t.props.ref.client.get.attachToContainer(t.props.containerId)

    def showImage(): Unit = t.props.ref.client.map { client =>
      client.images().map { images =>
        images.filter(_.Id == t.state.info.get.Image).map { image =>
          t.props.ref.show(ImagePage(image, t.props.ref))
        }
      }
    }

    def textCommands: Seq[(String, String)] = {
      val id = StringUtils.subId(t.props.containerId)
      t.state.tabSelected match {
        case TabTerminal => Seq(
          (s"docker attach $id", "Attach to the current container stdin"),
          (s"docker exec -i -t $id bash", "Open a new bash session to the container")
        )
        case TabChanges => Seq(
          (s"docker diff $id", "List the changed files and directories in a container᾿s filesystem")
        )
        case TabTop => Seq(
          (s"docker top $id", "Display the running processes of a container")
        )
        case _ => Seq.empty
      }
    }

    def startStats(): Unit = t.props.ref.client.map { client =>
      client.containerStats(t.props.containerId) { (stats, stream) =>
        t.modState(s => s.copy(stats = stats, statsConnectedStream = Some(stream)))
      }
    }

    def stopStats(): Unit = t.state.statsConnectedStream.map { stream =>
      stream.abort()
      t.modState(s => s.copy(statsConnectedStream = None))
    }
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
    .componentWillMount(_.backend.willMount)
    .componentWillUnmount(_.backend.willUnMount)
    .build


  def vdom(P: Props, S: State, B: Backend): ReactElement =
    <.div(
      S.error.map(Alert(_)),
      S.info.map(vdomInfo(_, S, P, B)),
      vDomTabs(S, B)
    )


  def vdomInfo(containerInfo: ContainerInfo, S: State, P: Props, B: Backend) = {
    val generalInfo = Map(
      "Id / Name" -> (containerInfo.id + " " + containerInfo.Name),
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
      "Links" ->  containerInfo.HostConfig.links
    ) ++
      containerInfo.Volumes.map { case (k, v) => ("volume: " + k, v) }


    <.div(
      InfoCard(generalInfo, InfoCard.SMALL, None,
        imageInfo(containerInfo, B),
        vdomCommands(S, B)
      ),
      InfoCard(executionInfo),
      InfoCard(networkInfo, InfoCard.SMALL, None, Seq.empty, vdomServiceUrl(containerInfo, P))
    )
  }

  def imageInfo(containerInfo: ContainerInfo, B: Backend) = Seq(
    <.div(^.className := "list-group",
      <.div(^.className := "list-group-item",
        <.i(^.className := "list-group-item-text")("Image"),
        <.p(^.className := "list-group-item-heading", ^.wordWrap := "break-word",
          <.a(^.onClick --> B.showImage)(containerInfo.image),
          " (",
          <.strong(^.className := "list-group-item-heading", ^.wordWrap := "break-word", containerInfo.Config.Image),
          ") "
        )
      )
    )
  )


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
                Button("Start", "glyphicon-play")(B.start)
          }
        ),
        <.div(^.className := "btn-group",
          if (state.info.exists(_.State.Running))
            Button("Remove", "glyphicon-trash", disabled = true)(B.remove)
          else
            Button("Remove", "glyphicon-trash")(B.remove)
        )
      )
    ))


  def vDomTabs(S: State, B: Backend) = {
    val terminalInfo = S.info.map { info =>
      TerminalInfo(stdinOpened = info.Config.OpenStdin && info.State.Running,
        stdinAttached = info.Config.AttachStdin,
        stOutAttached = info.Config.AttachStdout
      )
    }.getOrElse(TerminalInfo(false, false, false))

    <.div(^.className := "container  col-sm-12",
      <.div(^.className := "panel panel-default",
        <.ul(^.className := "nav nav-tabs",
          <.li(^.role := "presentation", (S.tabSelected == TabTerminal) ?= (^.className := "active"),
            <.a(^.onClick --> B.showTab(TabTerminal), ^.className := "glyphicon glyphicon-console",
              " Terminal ",
              <.i(^.className := (if (terminalInfo.stdinOpened) "fa fa-chain" else "fa fa-chain-broken"))
            )
          ),
          S.info.exists(_.State.Running) ?= <.li(^.role := "presentation", (S.tabSelected == TabStats) ?= (^.className := "active"),
            <.a(^.onClick --> B.showTab(TabStats), <.i(^.className := "fa fa-bar-chart"), " Stats")
          ),
          S.top.map(s =>
            <.li(^.role := "presentation", (S.tabSelected == TabTop) ?= (^.className := "active"),
              <.a(^.onClick --> B.showTab(TabTop), ^.className := "glyphicon glyphicon-transfer", " Top")
            )),
          <.li(^.role := "presentation", (S.tabSelected == TabChanges) ?= (^.className := "active"),
            <.a(^.onClick --> B.showTab(TabChanges), <.i(^.className := "fa fa-history"), " File system changes")
          )
        ),
        <.div(^.className := "panel-body panel-config",
          (S.tabSelected == TabTerminal) ?= TerminalCard(terminalInfo)(B.attach),
          (S.tabSelected == TabTop && S.top.isDefined) ?= S.top.map(vdomTop).get,
          (S.tabSelected == TabChanges) ?= S.info.map(vdomChanges(S.changes, _)),
          (S.tabSelected == TabStats) ?= vdomStats(S, B)
        )
      ),
      <.div(^.className := "panel-footer docker-cli",
        B.textCommands.map { case (cmd, info) =>
          val cmdName = info.split(" ").head
          <.div(
            <.span(^.className := "glyphicon glyphicon-console pull-left"),
            <.i(<.code(^.className := cmdName)(cmd), <.span(^.className := "small text-muted", info)),
            <.a(^.onClick --> CopyPasteUtil.copyToClipboard(cmdName)," ", <.i(^.className := "fa fa-clipboard"))
          )
        }
      )
    )
  }

  def vdomTop(top: ContainerTop): ReactElement = {
    val keys = top.Titles
    val values = top.Processes.map(data => keys.zip(data).toMap)
    TableCard(values)
  }

  def vdomChanges(changes: Seq[FileSystemChange], containerInfo: ContainerInfo): ReactElement = if (changes.isEmpty) {
    <.div(^.className := "panel",
      <.div(^.className := s"alert",
        s"There is no changes since this container was created from '${containerInfo.image}' (${containerInfo.Config.Image})"
      )
    )
  } else {
    val values = changes.map(c => Map("Kind" -> c.kind, "Path" -> c.Path))
    TableCard(values)
  }

  def vdomStats(S: State, B: Backend) = {
    S.stats.map { stats =>
      val values = Seq(Map[String, String](
        "CPU %" -> StringUtils.toPercent(stats.cpuPercent),
        "MEM USAGE/LIMIT" -> (StringUtils.bytesToSize(stats.memory.toLong) + " / " + StringUtils.bytesToSize(stats.memoryLimit.toLong)),
        "MEM %" -> StringUtils.toPercent(stats.memPercent)
      ))
      TableCard(values)
    }
  }

}

sealed trait ContainerPageTab

case object TabNone extends ContainerPageTab

case object TabTerminal extends ContainerPageTab

case object TabTop extends ContainerPageTab

case object TabChanges extends ContainerPageTab

case object TabStats extends ContainerPageTab