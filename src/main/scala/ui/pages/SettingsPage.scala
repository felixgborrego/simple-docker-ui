package ui.pages

import api.{ConfigStorage, DockerClient}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactEventI}
import model._
import ui.WorkbenchRef
import ui.widgets.Alert
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
        case None => t.modState(s => s.copy("", Some("There is no connection configuration")))
        case Some(c) => t.modState(s => s.copy(c.url, None))
      }
    }

    def save(): Unit = {
      val url = t.state.url
      if (url.startsWith("http")) {
        DockerClient(Connection(url)).ping().onComplete {
          case Success(_) =>
            t.modState(s => State(url, None))
            ConfigStorage.saveConnection(url).map(_ => t.props.ref.reconnect())
          case Failure(e) => {
            log.info("Unable to connected to " + url)
            t.modState(s => s.copy(url, Some("Unable to connected to " + url)))
          }
        }
      } else {
        log.info("Unable to connect to " + url)
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
              <.h3(^.className := "panel-title", "Mac OS config")
            ),
            <.div(^.className := "list-group",
              <.div(^.className := "list-group-item",
                <.p(^.className := "list-group-item-text",
                  "To allow Docker ui to connect to Docker you need to allow Chrome to use the ssl credentials used by boot2Docker.",
                  <.ul(
                    <.li("Add credentials to your Keychain", <.br(),
                      <.code("security import key.pem -k ~/Library/Keychains/login.keychain")
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
              <.h3(^.className := "panel-title", "Linux config")
            ),
            <.div(^.className := "list-group",
              <.div(^.className := "list-group-item",
                <.p(^.className := "list-group-item-text",
                  "To allow Docker ui to connect to Docker you need to enable      Docker Remote API. To do so you need to:",
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