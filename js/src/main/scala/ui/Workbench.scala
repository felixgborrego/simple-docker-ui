package ui


import api.{ConfigStore, DockerClient}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model._
import ui.Workbench.{Backend, State}
import ui.pages.{HomePage, Page, SettingsPage}
import ui.widgets.{Alert, Header}
import util.GoogleAnalytics
import util.logger.log

import scala.concurrent.ExecutionContext.Implicits.global

object Workbench {

  case class State(selectedPage: Page, error: Option[WorkbenchError], connection: Option[Connection])

  case class Props(pages: Set[Page])

  case class Backend(t: BackendScope[Props, State]) {

    val tracker = GoogleAnalytics()

    tracker.sendAppView(t.state.selectedPage.id)

    def show(page: Page) = t.modState { s =>
      log.info("page: " + page + " " + s.error)
      tracker.sendAppView(page.id)
      Workbench.State(page, s.error, s.connection)
    }

    def error(error: WorkbenchError) = {
      log.error(error)
      error match {
        case NoConfigurationError => t.modState(s => State(SettingsPage, Some(error), s.connection))
        case ex: ConnectionError => t.modState(s => State(SettingsPage, Some(error), s.connection))
        case ex: GenericError => t.modState(s => State(s.selectedPage, Some(error), s.connection))
      }
    }

    def componentWillMount(): Unit = {
      ConfigStore.getUrlConnection().map {
        case None => error(NoConfigurationError)
        case connection => t.modState(s => Workbench.State(s.selectedPage, None, connection))
      }
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
    .initialState(State(HomePage, None, None))
    .backend(new Backend(_))
    .render((P, S, B) => {
    <.div(
      Header(WorkbenchRef(S, B)),
      S.error.map { e =>
        Alert(e.msg, None)
      },
      S.selectedPage.component(WorkbenchRef(S, B))
    )
  }).componentWillMount(_.backend.componentWillMount()).build
}

case class WorkbenchRef(state: State, backend: Backend) {
  def selectedPage = state.selectedPage

  def link(page: Page) = <.a(^.onClick --> backend.show(page), ^.href := "#")

  def client: Option[DockerClient] = {
    val c = state.connection.map(DockerClient)
    if (!c.isDefined) {
      backend.error(NoConfigurationError)
    }
    c
  }

  def error(error: WorkbenchError) = backend.error(error)
}



