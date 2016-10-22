package ui.pages

import java.util.UUID

import api.{ConfigStorage, DockerClient, DockerClientConfig}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactEventI}
import ui.WorkbenchRef
import ui.pages.SettingsPageModel._
import ui.widgets.{Alert, Button}
import util.{EventAction, EventCategory, PlatformService}
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SettingsPage extends Page {

  val id = "Settings"

  case class Props(ref: WorkbenchRef)
  case class State(info: Info, error: Option[String] = None)

  case class Backend(t: BackendScope[Props, State]) {
    def onChange(connection: Connection)(e: ReactEventI) = {
      val newValue = connection.copy(urlText = e.target.value)
      t.modState(s => s.copy(info = s.info.replace(connection, newValue)))
    }

    def willMount(): Unit = {
      loadSelectedUrl()
    }

    def loadSelectedUrl(): Unit = {
      val selectedUrlFut = t.props.ref.connection match {
        case None =>
          t.modState(s => s.copy(error = Some("There is no connection configuration")))
          ConfigStorage.defaultUrl
        case Some(connection) => Future.successful(connection.url)
      }

      val savedUrlsFut = ConfigStorage.savedUrls()

      for {
        urlSelected <- selectedUrlFut
        savedUrls <- savedUrlsFut
      } yield {
        val connections = savedUrls.map(Connection(_, ConnectionNoVerified)).toList
        val info = Info(connections).select(urlSelected)
        t.modState(_.copy(info = info))
        check(reconnect = false)
      }
    }

    def save() = check(reconnect = true)

    def check(reconnect: Boolean): Future[Unit] = {
      val info = t.state.info
      log.debug(s"Checking connection: $info")
      Future.sequence(info.connections.map(verifyConnection)).map { _ =>
        t.modState { state =>
          state.info.selected match {
            case None =>
              log.info("No url selected")
              state
            case Some(selected) if selected.isValid =>
              PlatformService.current.sendEvent(EventCategory.Connection, EventAction.Saved, "Settings")
              if (reconnect) for {
                _ <- ConfigStorage.saveUrls(info.connections.map(_.url))
                _ <- ConfigStorage.saveConnection(selected.url)
              } yield t.props.ref.reconnect()
              state.copy(error = None)
            case Some(connection) =>
              PlatformService.current.sendEvent(EventCategory.Connection, EventAction.Unable, "Settings")
              log.info(s"Invalid urls ${connection.url} - ${connection.state}")
              state.copy(error = Some(s"Unable to connected to the selected url '${connection.url}'"))
          }
        }
      }
    }

    def verifyConnection(connection: Connection): Future[Unit] =
      connection.checkConnection().map { c =>
        t.modState { s =>
          s.copy(info = s.info.replace(connection, c))
        }
      }


    def isSelected(connection: Connection) = t.state.info.selected == Some(connection)

    def selectConnection(connection: Connection)(e: ReactEventI) = {
      val info = t.state.info.select(connection.url)
      t.modState(_.copy(info = info), { () =>
        info.selected.map(verifyConnection)
        log.debug(s"selected: ${info.selected}")
      })
    }

    def showAddConnection = !t.state.info.connections.map(_.url).contains("")

    def addConnection() = t.modState { s =>
      val info = s.info.select("")
      s.copy(info = info)
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
    .initialState(State(Info()))
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
          <.h3(^.className := "panel-title pull-left", <.i(^.className := "fa fa-plug"), " Connection to Docker Remote API"),
          <.div(^.className := "btn-group pull-right",
            Button("Save", "fa fa-check", "Verify and Save connections")(B.save)
          )
        ),
        <.div(^.className := "modal-body",
          <.form(^.className := "form-horizontal",
            S.info.connections.map { connection =>
              <.div(^.className := "input-group",
                (S.info.showSelect) ?= <.span(^.className := "input-group-addon",
                  <.input(^.`type` := "radio", ^.name := "selected",
                    ^.checked := B.isSelected(connection), ^.onChange ==> B.selectConnection(connection))
                ),
                <.input(^.`type` := "text", ^.className := "form-control", ^.value := connection.url, ^.onChange ==> B.onChange(connection)),
                <.span(^.className := "input-group-btn",
                  Button("Verify!", connection.stateIcon, "Verify connection")(B.verifyConnection(connection))
                ),
                (!connection.stateMessage.isEmpty) ?= <.span(^.className := "input-group-addon", <.i(connection.stateMessage))
              )
            },
            B.showAddConnection ?= <.button(^.className := "btn btn-success add-button-settings", ^.onClick --> B.addConnection(),
              <.i(^.className := "fa fa-plus")
            )
          )
        )
      )
      ,
      <.div(^.className := "panel panel-default",
        <.div(^.className := "panel-heading",
          <.h3(^.className := "panel-title", <.i(^.className := "fa wrench"), " How to config?")
        ),
        <.div(^.className := "list-group",
          <.div(^.className := "list-group-item",
            <.p(^.className := "list-group-item-text",
              "To connect this app with Docker you need to enable the Docker Remote API:"),
            <.ul(
              <.li(
                <.a(^.href := "https://github.com/felixgborrego/docker-ui-chrome-app/wiki/Mac-OS-X", ^.target := "_blank",
                  <.i(^.className := "fa fa-apple"), "Mac OS X config")
              ),
              <.li(
                <.a(^.href := "https://github.com/felixgborrego/docker-ui-chrome-app/wiki/windows", ^.target := "_blank",
                  <.i(^.className := "fa fa-windows"), "Windows config")
              ),
              <.li(
                <.a(^.href := "https://github.com/felixgborrego/docker-ui-chrome-app/wiki/linux", ^.target := "_blank",
                  <.i(^.className := "fa fa-linux"), "Linux config")
              )
            )
          )
        )
      ),
      <.div(^.className := "panel panel-default",
        <.div(^.className := "panel-heading",
          <.h3(^.className := "panel-title", <.i(^.className := "fa wrench"), " Want to help?")
        ),
        <.div(^.className := "list-group",
          <.div(^.className := "list-group-item",
            <.p(^.className := "list-group-item-text",
              " Please note this is a beta version, ",
              <.a(^.href := "https://github.com/felixgborrego/docker-ui-chrome-app/issues", ^.target := "blank", "any feedback is more than welcome!")
            )
          )
        )
      )
    )
  )


}

object SettingsPageModel {

  object Info {
    def apply(selectedUrl: String) = {
      val connection = Connection(selectedUrl, ConnectionNoVerified)
      new Info(List(connection), Some(connection))
    }

    def apply() = new Info(List.empty, None)
  }

  case class Info(connections: List[Connection], selected: Option[Connection] = None) {
    def replace(original: Connection, newValue: Connection) = {
      val newConnections = connections.map {
        case `original` => newValue
        case other => other
      }
      val newSelected = if (Some(original) == selected) Some(newValue) else selected
      this.copy(connections = newConnections, selected = newSelected)
    }


    def select(url: String): Info = {
      val newConnections = connections.find(_.url == url) match {
        case None => connections :+ Connection(url, ConnectionNoVerified)
        case Some(_) => connections
      }
      log.debug(s"Select $url")
      val selected = newConnections.find(_.url == url)
      this.copy(connections = newConnections, selected = selected)
    }

    def showSelect = connections.size > 1
  }

  sealed trait ConnectionState
  object ConnectionVerifying extends ConnectionState
  object ConnectionNoVerified extends ConnectionState
  object ConnectionValid extends ConnectionState
  object ConnectionUnableToConnect extends ConnectionState
  object ConnectionInvalidApi extends ConnectionState

  case class Connection(urlText: String, state: ConnectionState, id: UUID = UUID.randomUUID) {
    def isValid = state == ConnectionValid
    val url = urlText.toLowerCase

    def checkConnection(): Future[Connection] = if (!url.startsWith("http") && !url.startsWith("unix")) {
      log.debug(s"Invalid url, unable to connect to $url")
      Future.successful(Connection(url, ConnectionUnableToConnect))
    } else {
      log.debug(s"Looks like a valid url")
      PlatformService.current.buildDockerClient(model.Connection(url)).checkVersion().map {
        case true => Connection(url, ConnectionValid)
        case false => Connection(url, ConnectionInvalidApi)
      }.recover {
        case ex: Exception =>
          log.info(s"Unable to connected to $url")
          Connection(url, ConnectionUnableToConnect)
      }
    }

    def stateMessage = state match {
      case ConnectionVerifying | ConnectionNoVerified => ""
      case ConnectionUnableToConnect => "Unable to connect"
      case ConnectionInvalidApi => s"Invalid API version, minimum API supported is ${DockerClientConfig.DockerVersion}"
      case ConnectionValid => "Valid connection"
    }

    def stateIcon = state match {
      case ConnectionVerifying => "glyphicon-refresh glyphicon-spin"
      case ConnectionNoVerified => ""
      case ConnectionValid => "glyphicon-saved"
      case ConnectionUnableToConnect => "glyphicon-alert"
      case ConnectionInvalidApi => "glyphicon-warning-sign"
    }
  }
}
