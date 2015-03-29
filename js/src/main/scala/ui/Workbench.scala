package ui


import api.{ConfigStorage, DockerClient}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model._
import ui.Workbench.{Backend, State}
import ui.pages.{EmptyPage, HomePage, Page, SettingsPage}
import ui.widgets.Header
import util.GoogleAnalytics
import util.logger.log

import scala.concurrent.ExecutionContext.Implicits.global

object Workbench {

  case class State(selectedPage: Option[Page], connection: Option[Connection])

  case class Props(pages: Set[Page])

  case class Backend(t: BackendScope[Props, State]) {

    val tracker = GoogleAnalytics()


    def show(page: Page) = t.modState { s =>
      log.info("page: " + page)
      tracker.sendAppView(page.id)
      Workbench.State(Some(page), s.connection)
    }

    def componentWillMount(): Unit =
      ConfigStorage.getUrlConnection().map { connection =>
        t.modState(s =>s.copy(connection=connection))
        connection.fold(show(SettingsPage))(_ => show(HomePage))
      }

    def reconnect(): Unit =
      ConfigStorage.getUrlConnection().map { connection =>
        log.info("workbench reconnected to " + connection)
        t.modState(s =>s.copy(connection=connection))
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
    .initialState(State(None, None))
    .backend(new Backend(_))
    .render((P, S, B) => {
    <.div(
      Header(WorkbenchRef(S, B)),
      S.selectedPage.map(_.component(WorkbenchRef(S, B)))
    )
  }).componentWillMount(_.backend.componentWillMount()).build
}

case class WorkbenchRef(state: State, backend: Backend) {
  def selectedPage = state.selectedPage.getOrElse(EmptyPage)

  def link(page: Page) = <.a(^.onClick --> backend.show(page), ^.href := "#")

  def client: Option[DockerClient] = {
    state.connection.map(DockerClient)
  }

  def connection: Option[Connection] = state.connection

  def reconnect() = backend.reconnect()
}



