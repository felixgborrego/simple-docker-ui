package ui.pages

import api.{ConfigStorage, DockerClient}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactEventI}
import model._
import ui.WorkbenchRef
import ui.widgets.Alert
import util.googleAnalytics._
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object SettingsPage extends Page {

  val id = "Settings"

  case class Props(ref: WorkbenchRef)

  case class State(url: String = "", error: Option[String] = None)

  case class Backend(t: BackendScope[Props, State]) {
    def onChange(e: ReactEventI) =
      t.modState(_.copy(url = e.target.value))

    def willStart(): Unit = {
      t.props.ref.connection match {
        case None =>
          ConfigStorage.getDefaultUrl().map { url =>
            t.modState(_.copy(url = url, Some("There is no connection configuration")))
          }
        case Some(c) => t.modState(s => s.copy(c.url, None))
      }
    }

    def save(): Unit = {
      val url = t.state.url
      if (url.startsWith("http")) {
        DockerClient(Connection(url)).ping().onComplete {
          case Success(_) =>
            sendEvent("SettingsSavedConnection")
            t.modState(s => State(url, None))
            ConfigStorage.saveConnection(url).map(_ => t.props.ref.reconnect())
          case Failure(e) => {
            log.info(s"Unable to connected to $url")
            t.modState(s => s.copy(url, Some(s"Unable to connected to $url")))
          }
        }
      } else {
        sendEvent("SettingsUnableToConnect")
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
  }).componentWillMount(_.backend.willStart())
    .build


  def vdom(S: State, B: Backend) = <.div(
    S.error.map(Alert(_, None)),
    <.div(^.className := "container  col-sm-12",
      <.div(^.className := "panel panel-default",
        <.div(^.className := "panel-heading clearfix",
          <.h3(^.className := "panel-title pull-left", "Connection"),
          <.div(^.className := "btn-group pull-right",
            <.button(^.className := "btn btn-success", ^.onClick --> B.save,
              <.i(^.className := "fa fa-check", "Save")
            )
          )
        ),
        <.div(^.className := "modal-body",
          <.form(^.className := "form-horizontal",
            <.div(^.className := "form-group",
              <.label(^.className := "col-xs-3 control-label", "Url", <.br(), <.small("to Docker Remote Api")),
              <.div(^.className := "col-xs-9",
                <.input(^.`type` := "text", ^.className := "form-control", ^.value := S.url, ^.onChange ==> B.onChange
                )
              )
            )
          ),
          <.div(^.className := "panel panel-default",
            <.div(^.className := "panel-heading",
              <.h3(^.className := "panel-title",<.i(^.className := "fa fa-apple"), " Mac OS config")
            ),
            <.div(^.className := "list-group",
              <.div(^.className := "list-group-item",
                <.p(^.className := "list-group-item-text",
                  "By default Boot2Docker runs docker with TLS enabled. It auto-generates certificates and copies them to ~/.boot2docker/certs. To allow Docker UI to connect to Docker Remote API, we need first to install those credentials.",
                  <.ul(
                    <.li("First, you need to make Chrome trust in the auto-generated CA. Execute this command to add the new CA to your Certificate Trust Settings in your Keychain:", <.br(),
                      <.code("security add-trusted-cert -k ~/Library/Keychains/login.keychain  ~/.boot2docker/certs/boot2docker-vm/ca.pem")
                    ),
                    <.li("Also, you need to add the auto-generated certificate to your Keychain:", <.br(),
                      <.code("security import ~/.boot2docker/certs/boot2docker-vm/key.pem -k ~/Library/Keychains/login.keychain"), <.br(),
                      <.code("security import ~/.boot2docker/certs/boot2docker-vm/cert.pem  -k ~/Library/Keychains/login.keychain")
                    ),
                    <.li("Figure out the boot2docker ip using ", <.code("boot2docker ip")),
                    <.li("Try to reconnect!")
                  )
                )
              )
            )
          ),
          <.div(^.className := "panel panel-default",
            <.div(^.className := "panel-heading",
              <.h3(^.className := "panel-title", <.i(^.className := "fa fa-linux")," Linux config")
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
                      <.code("sudo service docker restart")
                    )
                  )
                )
              )
            )
          )
        )
      )

    )
  )

}
