package ui.widgets.dialogs

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactEventI}
import model._
import org.scalajs.dom
import org.scalajs.dom.ext.AjaxException
import ui.WorkbenchRef
import ui.widgets.Alert
import util.CopyPasteUtil
import util.StringUtils._
import util.collections._
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global

object ContainerRequestForm {

  trait ActionsBackend {
    def newContainerCreated(containerId: String)
  }

  trait ConfigPanel
  case object PortConfigPanel extends ConfigPanel
  case object VolumesConfigPanel extends ConfigPanel
  case object EnvironmentConfigPanel extends ConfigPanel
  case object LinkingConfigPanel extends ConfigPanel

  case class Env(key: String, value: String, allowEdit: Boolean = true)
  case class Link(key: String, value: String)
  case class VolumeMapping(hostPath: String, containerPath: String, rw: Boolean = true, allowEdit: Boolean = true)

  case class State(request: CreateContainerRequest,
                   volumesMapping: Seq[VolumeMapping],
                   environment: Seq[Env],
                   links: Seq[Link],
                   portsRadioOption: PortsRadioOptions = AllPorts,
                   panelSelected: ConfigPanel = PortConfigPanel,
                   warnings: Seq[String] = Seq.empty,
                   cmdText: String = "",
                   message: Option[String] = None) {

  }

  case class Props(actionsBackend: ActionsBackend, image: Image, initialConfig: ContainerConfig, ref: WorkbenchRef) {
    def imageName = image.RepoTags.headOption.getOrElse(image.Id)

    val hasExposedPorts = !initialConfig.exposedPorts.isEmpty
  }

  case class Backend(t: BackendScope[Props, State]) {

    def didMount(): Unit = {
      // The Dialog is not a react component
      dom.document.getElementById("open-modal-dialog").asInstanceOf[dom.raw.HTMLButtonElement].click()
      t.modState(s => s.copy(cmdText = s.request.cmd.mkString(" ")))
    }


    def updateName(e: ReactEventI) =
      t.modState(s => s.copy(request = s.request.copy(name = e.target.value.replaceAll("\\s", "")), warnings = Seq.empty))


    def updateCmd(e: ReactEventI) = {
      val cmd = e.target.value.split("\\s+").toSeq
      t.modState(s => s.copy(cmdText = e.target.value, request = s.request.copy(Cmd = cmd)))
    }

    def onStCheckBox(e: ReactEventI) =
      t.modState(s => s.copy(request = s.request.copy(OpenStdin = !s.request.OpenStdin, Tty = !s.request.Tty)))

    def stCheckBoxValue = t.state.request.OpenStdin && t.state.request.Tty

    def portsMapping(option: PortsRadioOptions): Unit = {
      val hostConfig = t.state.request.HostConfig
      val request = option match {
        case AllPorts => t.state.request.copy(HostConfig = hostConfig.copy(PublishAllPorts = true, PortBindings = Map.empty))
        case NoPorts => t.state.request.copy(HostConfig = hostConfig.copy(PublishAllPorts = false, PortBindings = Map.empty))
        case CustomPorts => t.state.request.copy(
          HostConfig = hostConfig.copy(
            PublishAllPorts = false,
            PortBindings = t.props.initialConfig.portBindings.map { case (containerPort, host) =>
              (containerPort, if (host.isEmpty) Seq(NetworkSettingsPort(HostIp = "", HostPort = "")) else host)
            }
          )
        )
      }

      t.modState(s => s.copy(portsRadioOption = option, request = request))
    }

    def updateBinding(containerPort: String, binding: NetworkSettingsPort)(e: ReactEventI): Unit = {
      val oldBinding = t.state.request.HostConfig.PortBindings
      val bindings = oldBinding(containerPort)
      val updatedBindings = oldBinding.updated(containerPort,
        bindings.updated(bindings.indexOf(binding), binding.copy(HostPort = e.target.value)))
      t.modState(_.copy(request = t.state.request.copy(
        HostConfig = t.state.request.HostConfig.copy(PortBindings = updatedBindings)
      )))
    }

    def updateVolumeMappingHost(valueMapping: VolumeMapping)(e: ReactEventI): Unit = {
      val binding = addEmptyVolumeMapping(t.state.volumesMapping.replace(valueMapping, valueMapping.copy(hostPath = e.target.value)))
      updateRequest(binding)
    }

    def updateVolumeMappingContainer(valueMapping: VolumeMapping)(e: ReactEventI): Unit = {
      val binding = addEmptyVolumeMapping(t.state.volumesMapping.replace(valueMapping, valueMapping.copy(containerPath = e.target.value)))
      updateRequest(binding)
    }

    def onRWCheckBox(valueMapping: VolumeMapping)(e: ReactEventI) = {
      val binding = t.state.volumesMapping.replace(valueMapping, valueMapping.copy(rw = !valueMapping.rw))
      updateRequest(binding)
    }

    def addEmptyVolumeMapping(seq: Seq[VolumeMapping]) = {
      val withoutEmpty = seq.filterNot(v => v.hostPath == "" && v.containerPath == "")
      withoutEmpty :+ VolumeMapping("", "")
    }

    def updateRequest(newBinding: Seq[VolumeMapping]) = {
      def noAlreadyDefinedInImage(binding: VolumeMapping) = !t.props.initialConfig.volumes.contains(binding.containerPath) && !(binding.containerPath == "" && binding.hostPath == "")
      val volumesMapping = newBinding.filter(noAlreadyDefinedInImage).map(binding => (binding.containerPath, VolumeEmptyHolder())).toMap
      val volumesRW = newBinding.filter(noAlreadyDefinedInImage).map(binding => (binding.containerPath, true)).toMap
      val bindings = newBinding.filter(_.hostPath != "").map(binding => binding.hostPath + ":" + binding.containerPath)
      t.modState { state =>
        val hostConfig = state.request.HostConfig.copy(Binds = bindings)
        state.copy(volumesMapping = newBinding, request = state.request.copy(Volumes = volumesMapping, VolumesRW = volumesRW, HostConfig = hostConfig))
      }
    }

    def updateEnvironmentKey(env: Env)(e: ReactEventI) = {
      val newEnvironment = t.state.environment.replace(env, env.copy(key = e.target.value))
      updateEnvironment(newEnvironment)
    }

    def updateEnvironmentValue(env: Env)(e: ReactEventI) = {
      val newEnvironment = t.state.environment.replace(env, env.copy(value = e.target.value))
      updateEnvironment(newEnvironment)
    }

    def updateEnvironment(environment: Seq[Env]) = t.modState { state =>
      val envRequest = environment
        .filterNot(_.key == "").filterNot(_.value == "")
        .map(env => env.key + "=" + env.value)
        .filterNot(t.props.initialConfig.env.contains(_))

      val newEnvironment = environment.filterNot(env => env.key == "" && env.value == "") :+ Env("", "")
      state.copy(request = state.request.copy(Env = envRequest), environment = newEnvironment)
    }

    def updateLinkKey(link: Link)(e: ReactEventI) = {
      val newLinks = t.state.links.replace(link, link.copy(key = e.target.value))
      updateLinks(newLinks)
    }

    def updateLinkValue(link: Link)(e: ReactEventI) = {
      val newLinks = t.state.links.replace(link, link.copy(value = e.target.value))
      updateLinks(newLinks)
    }

    def updateLinks(links: Seq[Link]) = t.modState { state =>
      val linksNoEmpty = links.filterNot(link => link.key == "" && link.value == "")
      val linkRequest = linksNoEmpty.map(env => env.key + ":" + env.value)

      val hostConfig = state.request.HostConfig.copy(Links = linkRequest)
      state.copy(request = state.request.copy(HostConfig = hostConfig), links = linksNoEmpty :+ Link("", "") )
    }

    def selectPanel(selected: ConfigPanel): Unit = t.modState(_.copy(panelSelected = selected))

    def run(): Unit = t.props.ref.client.map { client =>
      val task = for {
        response <- client.createContainer(t.state.request.name, t.state.request)
        _ <- client.startContainer(response.Id)
      } yield {
          if (response.Id.isEmpty) {
            t.modState(s => s.copy(warnings = response.Warnings))
          } else {
            dom.document.getElementById("open-modal-dialog").asInstanceOf[dom.raw.HTMLButtonElement].click()
            dom.setTimeout(() => t.props.actionsBackend.newContainerCreated(response.Id), 1) // delay after animation
          }
        }

      task.onFailure {
        case ex: AjaxException =>
          log.error("ImagesPage", "Unable to Start", ex)
          t.modState(s => s.copy(warnings = Seq(ex.xhr.responseText)))
        case ex: Exception =>
          log.error("ImagesPage", "Unable to Start", ex)
          t.modState(s => s.copy(warnings = Seq(ex.getMessage)))
      }
    }

    def textCommand = {
      val request = t.state.request
      val cmd = request.cmd.mkString(" ")
      val imageName = t.props.imageName
      val nameCommand = if (request.name.isEmpty) "" else s" --name ${request.name}"
      val paramI = if (request.OpenStdin) " -i" else ""
      val paramT = if (request.Tty) " -t" else ""
      def ports = t.state.portsRadioOption match {
        case AllPorts => " -P"
        case NoPorts => ""
        case CustomPorts => request.HostConfig.PortBindings
          .flatMap { case (containerPort, hostPorts) =>
          hostPorts.map(h => (substringBefore(containerPort, "/"), h.HostPort))
        }.filter(_._2.nonEmpty).map { case (internal, external) => s"-p $external:$internal" }
          .mkString(" ", " ", "")
      }

      val binds = request.HostConfig.Binds
      val volumes = if (binds.isEmpty) "" else binds.mkString(" -v ", " -v ", "")
      val env = if (request.Env.isEmpty) "" else request.Env.mkString(" -e ", " -e ", "")
      val links = if (request.HostConfig.Links.isEmpty) "" else request.HostConfig.Links.mkString(" --link ", " --link ", "")

      s"docker run$paramI$paramT$ports$volumes$env$links$nameCommand $imageName $cmd"
    }

    def panelSelectedClass(panel: ConfigPanel) = if (panel == t.state.panelSelected) "active" else ""
  }

  def apply(actionsBackend: ActionsBackend, image: Image, initialConfig: ContainerConfig, ref: WorkbenchRef) = {
    val props = Props(actionsBackend, image, initialConfig, ref)
    val exports = initialConfig.exposedPorts
    val environment = initialConfig.env.map(_.split("=")).map { case Array(key, value) => Env(key, value, allowEdit = false) } :+ Env("", "")
    val volumesMapping: Seq[VolumeMapping] = initialConfig.volumes.keys.map(VolumeMapping("", _, allowEdit = false)).toSeq :+ VolumeMapping("", "")
    val links = Seq(Link("", ""))
    val request = CreateContainerRequest(
      AttachStdin = true,
      AttachStdout = true,
      AttachStderr = true,
      Tty = true,
      OpenStdin = true, // opens stdin
      Cmd = initialConfig.Cmd,
      Image = props.imageName,
      HostConfig = HostConfig(PublishAllPorts = true, PortBindings = Map.empty, Binds = Seq.empty, Links = Seq.empty),
      ExposedPorts = exports,
      name = "")
    val initialState = State(request, volumesMapping, environment, links)
    ContainerRequestFormRender.component(initialState)(props)
  }

}

object ContainerRequestFormRender {

  import ui.widgets.dialogs.ContainerRequestForm._

  def component(initialState: State) = ReactComponentB[Props]("ContainerConfigForm")
    .initialState(initialState)
    .backend(new Backend(_))
    .render((P, S, B) => vdom(P, S, B))
    .componentDidMount(_.backend.didMount)
    .build

  val data_toggle = "data-toggle".reactAttr
  val data_target = "data-target".reactAttr
  var data_dismiss = "data-dismiss".reactAttr
  var data_trigger = "data-trigger".reactAttr


  def vdom(P: Props, S: State, B: Backend) = {

    <.div(^.className := "modal fade", ^.id := "editModal", ^.role := "dialog",
      <.div(^.className := "modal-dialog",
        <.div(^.className := "modal-content",
          <.div(^.className := "modal-header",
            <.div(^.className := "btn-group pull-left",
              <.button(^.className := "btn btn-danger", data_dismiss := "modal", "Cancel")
            ),
            <.div(^.className := "btn-group pull-right",
              <.button(^.id := "open-modal-dialog", ^.display := "none",
                data_toggle := "modal", data_target := "#editModal", "Open"),
              <.button(^.className := "btn btn-success", ^.onClick --> B.run, <.span(^.className := "glyphicon glyphicon-play"), "Run")
            ),
            <.div(^.className := "modal-title",
              <.span("Create new Container"), <.br(),
              <.i("Using the image: "), <.strong(P.imageName)
            )
          ),
          <.div(^.className := "modal-body",
            S.message.map(<.i(_)),
            S.warnings.map(Alert(_)),
            <.form(^.className := "form-horizontal",
              <.div(^.className := "form-group",
                <.label(^.className := "col-xs-3 control-label", "Container Name"),
                <.div(^.className := "col-xs-9",
                  <.input(^.`type` := "text", ^.className := "form-control", ^.placeholder := "Container name (optional) ",
                    ^.value := S.request.name, ^.onChange ==> B.updateName)
                )
              ),
              <.div(^.className := "form-group",
                <.label(^.className := "col-xs-3 control-label", "Command"),
                <.div(^.className := "col-xs-9",
                  <.input(^.`type` := "text", ^.className := "form-control", ^.placeholder := "Command",
                    ^.value := S.cmdText, ^.onChange ==> B.updateCmd)
                )
              ),
              <.div(^.className := "form-group",
                <.label(^.className := "col-xs-3 control-label", "stdin/stout"),
                <.div(^.className := "col-xs-9",
                  <.input(^.`type` := "checkbox", ^.checked := B.stCheckBoxValue, ^.onChange ==> B.onStCheckBox,
                    <.span(" Keep STDIN open & Allocate a pseudo-TTY", <.br(), <.small("you can connect later in interactive mode."))
                  )
                )
              ),
              configPanels(S, B, P)
            )
          ),
          <.div(^.className := "modal-footer docker-cli",
            <.i(^.className := "glyphicon glyphicon-console pull-left", <.code(^.className:= "docker-cli-text")(B.textCommand)),
            <.a(^.onClick --> CopyPasteUtil.copyToClipboard("docker-cli-text"), " ", <.i(^.className := "fa fa-clipboard"))
          )
        )
      )
    )
  }

  def configPanels(S: State, B: Backend, P: Props) =
    <.div(^.className := "panel",
      <.ul(^.className := "nav nav-tabs",
        <.li(^.className := B.panelSelectedClass(PortConfigPanel), <.a(^.onClick --> B.selectPanel(PortConfigPanel), <.i(^.className := "fa fa-plug"), " Ports")),
        <.li(^.className := B.panelSelectedClass(VolumesConfigPanel), <.a(^.onClick --> B.selectPanel(VolumesConfigPanel), <.i(^.className := "fa fa-folder-open"), " Volumes")),
        <.li(^.className := B.panelSelectedClass(EnvironmentConfigPanel), <.a(^.onClick --> B.selectPanel(EnvironmentConfigPanel), <.i(^.className := "fa fa-laptop"), " Enviromments")),
        <.li(^.className := B.panelSelectedClass(LinkingConfigPanel), <.a(^.onClick --> B.selectPanel(LinkingConfigPanel), <.i(^.className := "fa fa-external-link"), " Links"))
      ),
      <.div(^.className := "panel-body panel-config",
        portsPanel(S, B),
        volumesPanel(S, B),
        environmentsPanel(S, B),
        linkingPanel(S, B)
      )
    )

  def portsPanel(S: State, B: Backend) = S.panelSelected == PortConfigPanel ?=
    <.div(
      <.div(^.className := "form-group",
        <.label(^.className := "col-xs-6 control-label", "Ports to expose: "),
        <.div(^.className := "col-xs-6 btn-group", data_toggle := "buttons",
          <.label(^.onClick --> B.portsMapping(AllPorts), ^.className := "btn btn-primary  active", <.input(^.`type` := "radio", ^.className := "publicPorts", ^.name := "ports"), "All"),
          <.label(^.onClick --> B.portsMapping(CustomPorts), ^.className := "btn btn-primary", <.input(^.`type` := "radio", ^.className := "publicPorts", ^.name := "ports"), "Custom"),
          <.label(^.onClick --> B.portsMapping(NoPorts), ^.className := "btn btn-primary", <.input(^.`type` := "radio", ^.className := "publicPorts", ^.name := "ports"), "None")
        )
      ),
      S.portsRadioOption == CustomPorts ?= tablePortBindings(S, B)
    )

  def volumesPanel(S: State, B: Backend) = S.panelSelected == VolumesConfigPanel ?=
    <.div(
      tableVolumesMapping(S, B),
      <.div(^.className := "alert alert-info", ^.role := "alert",
        <.p( """Note: If you are using Docker on Mac or Windows,
               | your Docker daemon only has limited access to your OS X/Windows filesystem.""".stripMargin),
        <.p( """Docker tries to auto-share your /Users (OS X) or C:\Users (Windows) directory.),"""),
        <.p( " you can mount files or directories using"),
        <.p( " docker run -v /Users/<path>:/<container path> ... (OS X)"),
        <.p( " or docker run -v /c/Users/<path>:/<container path ... (Windows)."),
        <.p(" All other paths come from your virtual machine's filesystem.")
      )
    )

  def environmentsPanel(S: State, B: Backend) = S.panelSelected == EnvironmentConfigPanel ?=
    <.div(
      tableEnvironment(S: State, B: Backend)
    )

  def linkingPanel(S: State, B: Backend) = S.panelSelected == LinkingConfigPanel ?=
    <.div(
      tableLinking(S: State, B: Backend)
    )



  def tablePortBindings(S: State, B: Backend) =
    <.div(^.className := "",
      <.table(^.className := "table table-hover table-striped",
        <.thead(
          <.tr(
            <.th("Host Ports"),
            <.th(<.i(^.className := "fa fa-plug")),
            <.th("Container Port")
          )
        ),
        <.tbody(
          S.request.HostConfig.PortBindings.map { case (containerPort, hostPorts) =>
            hostPorts.map { host =>
              <.tr(
                <.td(<.input(^.`type` := "text", ^.className := "form-control", ^.placeholder := "Exposed port in the host",
                  ^.value := host.HostPort, ^.onChange ==> B.updateBinding(containerPort, host))
                ),
                <.td(<.i(^.className := "fa fa-arrow-right")),
                <.td(containerPort)
              )
            }
          }
        )
      )
    )

  def tableVolumesMapping(S: State, B: Backend) =
    <.table(^.className := "table table-hover table-striped",
      <.thead(
        <.tr(
          <.th("Host path"),
          <.th(<.i(^.className := "fa fa-folder-open")),
          <.th("Container path"),
          <.th("RW")
        )
      ),
      <.tbody(
        S.volumesMapping.map { case binding =>
          <.tr(
            <.td(<.input(^.`type` := "text", ^.className := "form-control", ^.placeholder := "/host/path",
              ^.value := binding.hostPath, ^.onChange ==> B.updateVolumeMappingHost(binding))
            ),
            <.td(<.i(^.className := "fa fa-arrow-right")),
            <.td(<.input(^.`type` := "text", ^.className := "form-control", ^.placeholder := "/container/path",
              ^.value := binding.containerPath, ^.onChange ==> B.updateVolumeMappingContainer(binding),
              !binding.allowEdit ?= (^.disabled := "disabled"))
            )
            ,
            <.td(
              <.input(^.`type` := "checkbox", ^.checked := binding.rw, ^.onChange ==> B.onRWCheckBox(binding))
            )
          )
        }
      )
    )


  def tableEnvironment(S: State, B: Backend) =
    <.table(^.className := "table table-hover table-striped",
      <.thead(
        <.tr(
          <.th("Val"),
          <.th("="),
          <.th("Value")
        )
      ),
      <.tbody(
        S.environment.map { case env =>
          <.tr(
            <.td(<.input(^.`type` := "text", ^.className := "form-control", ^.placeholder := "VAR", !env.allowEdit ?= (^.disabled := "disabled"),
              ^.value := env.key, ^.onChange ==> B.updateEnvironmentKey(env))
            ),
            <.td("="),
            <.td(<.input(^.`type` := "text", ^.className := "form-control", ^.placeholder := "value",
              ^.value := env.value, ^.onChange ==> B.updateEnvironmentValue(env))
            )
          )
        }
      )
    )

  def tableLinking(S: State, B: Backend) =
    <.table(^.className := "table table-hover table-striped",
      <.thead(
        <.tr(
          <.th("Source Container Name"),
          <.th(":"),
          <.th("Container Alias Name")
        )
      ),
      <.tbody(
        S.links.map { case link =>
          <.tr(
            <.td(<.input(^.`type` := "text", ^.className := "form-control", ^.placeholder := "container_name",
              ^.value := link.key, ^.onChange ==> B.updateLinkKey(link))
            ),
            <.td("="),
            <.td(<.input(^.`type` := "text", ^.className := "form-control", ^.placeholder := "alias",
              ^.value := link.value, ^.onChange ==> B.updateLinkValue(link))
            )
          )
        }
      )
    )

}

sealed trait PortsRadioOptions

case object AllPorts extends PortsRadioOptions

case object CustomPorts extends PortsRadioOptions

case object NoPorts extends PortsRadioOptions