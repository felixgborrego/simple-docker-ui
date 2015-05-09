package ui.pages

import api.{ConfigStorage, DockerClient, DockerClientConfig}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactEventI}
import model._
import ui.WorkbenchRef
import ui.widgets.Alert
import util.googleAnalytics._
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global

object SettingsPage extends Page {

  val id = "Settings"

  case class Props(ref: WorkbenchRef)

  case class State(url: String = "", error: Option[String] = None)

  case class Backend(t: BackendScope[Props, State]) {
    def onChange(e: ReactEventI) =
      t.modState(_.copy(url = e.target.value))

    def willMount(): Unit = {
      t.props.ref.connection match {
        case None =>
          ConfigStorage.defaultUrl.map { url =>
            t.modState(_.copy(url = url, Some("There is no connection configuration")))
          }
        case Some(c) =>
          t.modState(s => s.copy(c.url, None))
          check(c.url, reconnect = false)
      }
    }

    def check(url: String, reconnect: Boolean): Unit = {

      DockerClient(Connection(url)).checkVersion().map {
        case true =>
          sendEvent(EventCategory.Connection, EventAction.Saved, "Settings")
          t.modState(s => State(url, None))
          if (reconnect) ConfigStorage.saveConnection(url).map(_ => t.props.ref.reconnect())
        case false =>
          sendEvent(EventCategory.Connection, EventAction.InvalidVersion, "Settings")
          t.modState(s => s.copy(url, Some(
            s"""There is connection but Docker UI requires a newer version.
               |Minimum Remote API supported is ${DockerClientConfig.DockerVersion}""".stripMargin)))
      }.onFailure {
        case ex: Exception =>
          log.info(s"Unable to connected to $url")
          t.modState(s => s.copy
            (url, Some(s"Unable to connected to $url")))
      }
    }

    def save(): Unit = {
      val url = t.state.url
      if (url.startsWith("http")) {
        check(url, reconnect = true)
      } else {
        sendEvent(EventCategory.Connection, EventAction.Unable, "Settings")
        log.info(s"Invalid $url")
      }
    }
  }

  def component(ref: WorkbenchRef) = {
    val props = Props(ref)
    SettingsPageRender.component(props)
  }
}


object SettingsPageRender {

  import SettingsPage._

  def component = ReactComponentB[Props]("SettingsPage")
    .initialState(State())
    .backend(new Backend(_))
    .render((P, S, B) => {
    vdom(S, B)
  }).componentWillMount(_.backend.willMount)
    .build


  def vdom(S: State, B: Backend) = <.div(
    S.error.map(Alert(_, None)),
    <.div(^.className := "container  col-sm-12",
      <.div(^.className := "panel panel-default",
        <.div(^.className := "panel-heading clearfix",
          <.h3(^.className := "panel-title pull-left", <.i(^.className := "fa fa-plug"), " Connection to Docker Remote Api"),
          <.div(^.className := "btn-group pull-right",
            <.button(^.className := "btn btn-success", ^.onClick --> B.save,
              <.i(^.className := "fa fa-check", "Save")
            )
          )
        ),
        <.div(^.className := "modal-body",
          <.form(^.className := "form-horizontal",
            <.div(^.className := "form-group",
              <.label(^.className := "col-xs-3 control-label", "Url", <.br(), <.small()),
              <.div(^.className := "col-xs-9",
                <.input(^.`type` := "text", ^.className := "form-control", ^.value := S.url, ^.onChange ==> B.onChange
                )
              )
            )
          ),
          <.div(^.className := "panel panel-default",
            <.div(^.className := "panel-heading",
              <.h3(^.className := "panel-title", <.i(^.className := "fa fa-apple"), " Mac OS X config")
            ),
            <.div(^.className := "list-group",
              <.div(^.className := "list-group-item",
                <.p(^.className := "list-group-item-text",
                  "By default Boot2Docker runs Docker with TLS enabled. It auto-generates certificates and copies them to ~/.boot2docker/certs. To allow Docker UI to connect to Docker Remote API, first we need to install and allow Chrome to use those credentials.",
                  <.ul(
                    <.li("First, to make Chrome trust in the auto-generated CA. Execute this command to add the new CA to your Certificate Trust Settings in your Keychain:", <.br(),
                      <.code("security add-trusted-cert -k ~/Library/Keychains/login.keychain  ~/.boot2docker/certs/boot2docker-vm/ca.pem")
                    ),
                    <.li("Also, you need to add the auto-generated certificate to your Keychain:", <.br(),
                      <.code("security import ~/.boot2docker/certs/boot2docker-vm/key.pem -k ~/Library/Keychains/login.keychain"), <.br(),
                      <.code("security import ~/.boot2docker/certs/boot2docker-vm/cert.pem  -k ~/Library/Keychains/login.keychain")
                    ),
                    <.li("Figure out the boot2docker ip using ", <.code("boot2docker ip")),
                    <.li("Open your browser and verify you can connect to https://192.168.59.103:2376/_ping (this will ask for your certificate the first time)"),
                    <.li("Try to reconnect! using https://192.168.59.103:2376")
                  ),
                  <.a(^.href := "https://github.com/felixgborrego/docker-ui-chrome-app/wiki", ^.target := "_blank", "Wiki for more info.")
                )
              )
            )
          ),
          <.div(^.className := "panel panel-default",
            <.div(^.className := "panel-heading",
              <.h3(^.className := "panel-title", <.i(^.className := "fa fa-linux"), " Linux config")
            ),
            <.div(^.className := "list-group",
              <.div(^.className := "list-group-item",
                <.p(^.className := "list-group-item-text",
                  "We'll need to enable the Docker Remote API, but first make sure Docker daemon is up an running using ", <.code("docker info."),
                  <.h3("Linux with systemd (Ubuntu 15.04, Debian 8,...)"),
                  "Using systemd, we'll need to enable a systemd socket to access the Docker remote API:",
                  <.ul(
                    <.li("Create a new systemd config file called ", <.code("/etc/systemd/system/docker-tcp.socket"), " to make docker available on a TCP socket on port 2375.",
                      <.pre( """[Unit]
                               |Description=Docker HTTP Socket for the API
                               |
                               |[Socket]
                               |ListenStream=2375
                               |BindIPv6Only=both
                               |Service=docker.service
                               |
                               |[Install]
                               |WantedBy=sockets.target""".stripMargin)
                    ),
                    <.li("Register the new systemd http socket and restart docker", <.br(),
                      <.code( """systemctl enable docker-tcp.socket
                                |systemctl stop docker
                                |systemctl start docker-tcp.socket""".stripMargin),
                      <.li("Open your browser and verify you can connect to http://localhost:2375/_ping")
                    )
                  ),
                  <.h3("Linux without systemd:"),
                  <.ul(
                    <.li("Edit ", <.code("/etc/default/docker"), " to allow connections adding:", <.br(),
                      <.code("DOCKER_OPTS='-H tcp://0.0.0.0:4243 -H unix:///var/run/docker.sock'")
                    ),
                    <.li("Restart the Docker service using:", <.br(),
                      <.code("sudo service docker restart")
                    ),
                    <.li("Open your browser and verify you can connect to http://localhost:2375/_ping"),
                    <.li("Try to reconnect! using http://localhost:2375")
                  ),
                  <.a(^.href := "https://github.com/felixgborrego/docker-ui-chrome-app/wiki", ^.target := "_blank", "Wiki for more info.")
                )
              )
            )
          ),
          <.div(^.className := "panel panel-default",
            <.div(^.className := "panel-heading",
              <.h3(^.className := "panel-title", <.i(^.className := "fa fa-windows"), " Windows config")
            ),
            <.div(^.className := "list-group",
              <.div(^.className := "list-group-item",
                <.p(^.className := "list-group-item-text",
                  "The easiest way to connect to Docker Remote API is by disabling TLS.", "To do so, you need to:",
                  <.ul(
                    <.li("Log into the boot2docker virtual machine: ", <.code("boot2docker ssh")),
                    <.li("Add ", <.code("DOCKER_TLS=no"), " to the file ", <.code("/var/lib/boot2docker/profile"), " (may not exist)", <.br(),
                      <.code("echo \"DOCKER_TLS=no\" | sudo tee /var/lib/boot2docker/profile")
                    ),
                    <.li("Restart boot2docker", <.br(), <.code("boot2docker down"),<.br(), <.code("boot2docker up")),
                    <.li("Open your browser and verify you can connect to http://192.168.59.103:2375/_ping"),
                    <.li("Try to reconnect! using http://192.168.59.103:2375")
                  )
                ),
                <.a(^.href := "https://github.com/felixgborrego/docker-ui-chrome-app/wiki", ^.target := "_blank", "Wiki for more info.")
              )
            )
          )
        )
      )

    )
  )


}
