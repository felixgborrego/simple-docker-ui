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
        case Some(c) => t.modState(s => s.copy(c.url, None))
      }
    }

    def save(): Unit = {
      val url = t.state.url
      if (url.startsWith("http")) {
        DockerClient(Connection(url)).checkVersion().map {
          case true =>
            sendEvent(EventCategory.Connection, EventAction.Saved, "Settings")
            t.modState(s => State(url, None))
            ConfigStorage.saveConnection(url).map(_ => t.props.ref.reconnect())
          case false =>
            sendEvent(EventCategory.Connection, EventAction.InvalidVersion, "Settings")
            t.modState(s => s.copy(url, Some(
              s"""Docker UI requires a newer version.
                 |Minimum Remote API supported is ${DockerClientConfig.DockerVersion}""".stripMargin)))
        }.onFailure {
          case ex: Exception =>
            log.info(s"Unable to connected to $url")
            t.modState(s => s.copy(url, Some(s"Unable to connected to $url")))
        }
      } else {
        sendEvent(EventCategory.Connection, EventAction.Unable, "Settings")
        log.info("Invalid $url")
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
          <.h3(^.className := "panel-title pull-left", <.i(^.className := "fa fa-plug")," Connection to Docker Remote Api"),
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
              <.h3(^.className := "panel-title", <.i(^.className := "fa fa-apple"), " Mac OS config")
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
                    <.li("Try to reconnect!")
                  )
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
                  "You need to enable Docker Remote API. To do so you need to:",
                  <.ul(
                    <.li("Edit ", <.code("/etc/default/docker"), " to allow connections adding:", <.br(),
                      <.code("DOCKER_OPTS='-H tcp://0.0.0.0:4243 -H unix:///var/run/docker.sock'")
                    ),
                    <.li("Restart the Docker service using:", <.br(),
                      <.code("sudo service docker restart"),
                      <.li("Open your browser and verify you can connect to http://localhost:4243/_ping")
                    )
                  )
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
                  "No tested yet, but you should be able to connect by disabling the TLS."
                )
              )
            )
          )
        )
      )

    )
  )

}
