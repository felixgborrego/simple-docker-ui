package ui.pages

import api.{ConfigStorage, ConnectedStream}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import model.stats.ContainerStats
import model.{BasicWebSocket, ContainerInfo, ContainerTop, FileSystemChange}
import org.scalajs.dom.raw.WebSocket
import ui.WorkbenchRef
import ui.widgets.TerminalCard.TerminalInfo
import ui.widgets._
import util.logger._
import util._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js

object ContainerPage {

  case class State(info: Option[ContainerInfo] = None,
                   top: Option[ContainerTop] = None,
                   changes: Seq[FileSystemChange] = Seq.empty,
                   error: Option[String] = None,
                   actionStopping: Boolean = false,
                   stats: Option[ContainerStats] = None,
                   statsConnectedStream: Option[ConnectedStream] = None,
                   tabSelected: ContainerPageTab = TabMain,
                   runCommand: Option[String] = None)

  case class Props(ref: WorkbenchRef, containerId: String)

  case class Backend(t: BackendScope[Props, State]) {

    def willMount(): Unit = t.props.ref.client.map { client =>
      val result = for {
        info <- client.containerInfo(t.props.containerId)
        top <- if (info.State.Running) client.top(t.props.containerId).map(Some(_)) else Future(Option.empty)
        changes <- if (info.State.Running) client.containerChanges(t.props.containerId) else Future.successful(Seq.empty)
        runCommand <- ConfigStorage.getRunCommand(t.props.containerId)
      } yield t.modState(s => s.copy(Some(info), top, changes, error = None, runCommand = runCommand))


      result.onFailure {
        case ex: Exception =>
          log.error("ContainerPage", s"Unable to get containerInfo for ${t.props.containerId}", ex)
          t.modState(s => s.copy(error = Some(s"Unable to connect")))
      }

    }

    def willUnMount(): Unit = stopStats()

    def stop() =
      t.props.ref.client.get.stopContainer(t.props.containerId).map { info =>
        PlatformService.current.sendEvent(EventCategory.Container, EventAction.Stop)
        refresh()
      }

    def start() =
      t.props.ref.client.get.startContainer(t.props.containerId).map { info =>
        PlatformService.current.sendEvent(EventCategory.Container, EventAction.Start)
        t.modState(s => s.copy(tabSelected = TabMain))
        refresh()
      }.recover {
        case ex: Exception => t.modState(s => s.copy(error = Some(ex.getMessage)))
      }

    def refresh() = willMount()

    def remove() =
      t.props.ref.client.get.removeContainer(t.props.containerId).map { info =>
        ConfigStorage.removeRunCommand(t.props.containerId)
        t.props.ref.show(ContainersPage)
      }

    def showTab(tab: ContainerPageTab) = {
      tab match {
        case TabInfo => startStats()
        case _ => stopStats()
      }
      t.modState(s => s.copy(tabSelected = tab))
    }

    def attach(): Future[BasicWebSocket] =
      t.props.ref.client.get.attachToContainer(t.props.containerId)

    def resize(g: Geometry): Unit = {
      // TODO t.props.ref.client.get.resizeTTY(t.props.containerId, g.rows, w)
    }

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
        case TabMain => Seq(
          (s"docker inspect $id", "Inspect container")
        )
        case TabTerminal => Seq(
          (s"docker attach $id", "Attach to the current container stdin"),
          (s"docker exec -i -t $id bash", "Open a new bash session to the container")
        )
        case TabChanges => Seq(
          (s"docker diff $id", "List the changed files and directories in a container᾿s filesystem")
        )
        case TabInfo => Seq(
          (s"docker top $id", "Display the running processes of the container"),
          (s"docker stats $id", "Display the stats of the container")
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
      vDomTabs(P, S, B)
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
      "Links" -> containerInfo.HostConfig.links
    ) ++
      containerInfo.Volumes.map { case (k, v) => ("volume: " + k, v) }


    <.div(
      InfoCard(generalInfo, InfoCard.SMALL, None,
        Seq(imageInfo(containerInfo, B)) ++ S.runCommand.map(runCommand(_)),
        vdomCommands(S, B)
      ),
      InfoCard(executionInfo),
      InfoCard(networkInfo, InfoCard.SMALL, None, Seq.empty, vdomServiceUrl(containerInfo, P))
    )
  }

  def imageInfo(containerInfo: ContainerInfo, B: Backend) = {
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
  }

  def runCommand(cmd: String) = {
    val cmdName = cmd.split(" ").head
    <.div(^.className := "list-group",
      <.div(^.className := "list-group-item",
        <.i(^.className := "list-group-item-text")("Run command"),
        <.p(^.className := s"${cmdName} list-group-item-heading", ^.wordWrap := "break-word", cmd,
          <.a(^.onClick --> CopyPasteUtil.copyToClipboard(cmdName), " ", <.i(^.className := "fa fa-clipboard", ^.style := js.Dictionary("opacity" -> "1").asInstanceOf[js.Object]))
        )
      )
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


  def vDomTabs(P: Props, S: State, B: Backend): ReactElement = {
    val terminalInfo = S.info.map { info =>
      TerminalInfo(stdinOpened = info.Config.OpenStdin && info.State.Running,
        stdinAttached = info.Config.AttachStdin,
        stOutAttached = info.Config.AttachStdout
      )
    }.getOrElse(TerminalInfo(false, false, false))

    <.div(^.className := "container  col-sm-12",
      <.div(^.className := "panel panel-default",
        <.ul(^.className := "nav nav-tabs",
          <.li(^.role := "presentation", (S.tabSelected == TabMain) ?= (^.className := "active"),
            <.a(^.onClick --> B.showTab(TabMain), <.i(^.className := "fa fa-info"), " Info")
          ),
          S.info.exists(_.State.Running) ?= <.li(^.role := "presentation", (S.tabSelected == TabInfo) ?= (^.className := "active"),
            <.a(^.onClick --> B.showTab(TabInfo), <.i(^.className := "fa fa-bar-chart"), " Stats")
          ),
          <.li(^.role := "presentation", (S.tabSelected == TabChanges) ?= (^.className := "active"),
            <.a(^.onClick --> B.showTab(TabChanges), <.i(^.className := "fa fa-history"), " File system changes")
          ),
          <.li(^.role := "presentation", (S.tabSelected == TabTerminal) ?= (^.className := "active"),
            <.a(^.onClick --> B.showTab(TabTerminal), ^.className := "glyphicon glyphicon-console",
              " Terminal ",
              <.i(^.className := (if (terminalInfo.stdinOpened) "fa fa-chain" else "fa fa-chain-broken"))
            )
          )
        ),
        <.div(^.className := "panel-body panel-config",
          (S.tabSelected == TabMain) ?= S.info.map(vdomInfo(_, S, P, B)),
          (S.tabSelected == TabTerminal) ?= TerminalCard(terminalInfo)(B.attach, B.resize),
          (S.tabSelected == TabInfo) ?= vdomInfo(S, B),
          (S.tabSelected == TabChanges) ?= S.info.map(vdomChanges(S.changes, _))
        )
      ),
      <.div(^.className := "panel-footer docker-cli",
        B.textCommands.map { case (cmd, info) =>
          val cmdName = info.split(" ").head
          <.div(
            <.span(^.className := "glyphicon glyphicon-console pull-left"),
            <.i(<.code(^.className := cmdName)(cmd), <.span(^.className := "small text-muted", info)),
            <.a(^.onClick --> CopyPasteUtil.copyToClipboard(cmdName), " ", <.i(^.className := "fa fa-clipboard"))
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

  def vdomInfo(S: State, B: Backend) =
    <.div(
      vdomStats(S, B),
      S.top.map(vdomTop)
    )
}

sealed trait ContainerPageTab

case object TabMain extends ContainerPageTab

case object TabInfo extends ContainerPageTab
case object TabTerminal extends ContainerPageTab
case object TabChanges extends ContainerPageTab
