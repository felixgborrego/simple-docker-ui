package ui


import api.{ConfigStorage, DockerClient}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model._
import ui.Workbench.{Backend, State}
import ui.pages.{EmptyPage, HomePage, Page, SettingsPage}
import ui.widgets.Header
import util.googleAnalytics._
import util.logger.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Workbench {

  case class State(selectedPage: Option[Page], connection: Option[Connection], savedConnections: Seq[Connection])

  case class Props(pages: Set[Page])

  case class Backend(t: BackendScope[Props, State]) {

    def show(page: Page) = t.modState { s =>
      sendAppView(page.id)
      s.copy(selectedPage = Some(page))
    }

    def componentWillMount() = connectSavedConnection()

    def connectSavedConnection(): Unit = for {
      connection <- ConfigStorage.urlConnection
      savedUrls <- ConfigStorage.savedUrls()
    } yield {
      t.modState(_.copy(connection = connection, savedConnections = savedUrls.map(Connection)))
      connection match {
        case Some(url) =>
          sendEvent(EventCategory.Connection, EventAction.Connected, "SavedConnection")
          show(HomePage)
        case None => tryDefaultConnection()
      }
    }

    def tryDefaultConnection() = {
      sendEvent(EventCategory.Connection, EventAction.Try, "Default")
      val test = for {
        client <- ConfigStorage.defaultUrl.map(Connection).map(DockerClient)
        _ <- client.ping().map(_ => sendEvent(EventCategory.Connection, EventAction.Connected, "WithDefaultConnection"))
        _ <- ConfigStorage.saveConnection(client.connection.url)
      } yield connectSavedConnection()

      test.onFailure { case _ => show(SettingsPage) }
    }

    def reconnect(): Future[Unit] = for {
      connection <- ConfigStorage.urlConnection
      savedUrls <- ConfigStorage.savedUrls()
    } yield {
        log.info(s"workbench reconnected to $connection")
        val savedConnections = savedUrls.map(Connection)
        t.modState(s => s.copy(connection = connection, savedConnections = savedConnections))
      }
  }

  def apply() = {
    val props = Props(Set(HomePage))
    WorkbenchRender.component(props)
  }
}


object WorkbenchRender {

  import Workbench._

  val component = ReactComponentB[Props]("Workbench")
    .initialState(State(None, None, Nil))
    .backend(new Backend(_))
    .render((P, S, B) => vdom(S, B))
    .componentWillMount(_.backend.componentWillMount())
    .build

  def vdom(S: State, B: Backend) =
    <.div(
      Header(WorkbenchRef(S, B)),
      S.selectedPage.map(_.component(WorkbenchRef(S, B)))
    )
}

case class WorkbenchRef(state: State, backend: Backend) {
  def selectedPage = state.selectedPage.getOrElse(EmptyPage)

  def link(page: Page) = <.a(^.onClick --> backend.show(page), ^.href := "#")

  def show(page: Page) = backend.show(page)

  def client: Option[DockerClient] = {
    state.connection.map(DockerClient)
  }

  def connection: Option[Connection] = state.connection

  def reconnect() = backend.reconnect()
}



