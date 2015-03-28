package ui


import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model.WorkbenchError
import ui.pages.{HomePage, Page}
import ui.widgets.Header
import util.GoogleAnalytics

object Workbench {

  case class State(selectedPage: Page, error: Option[WorkbenchError])

  case class Props(pages: Set[Page])

  case class Backend(t: BackendScope[Props, State]) {

    val tracker = GoogleAnalytics()

    tracker.sendAppView(t.state.selectedPage.id)

    def show(page: Page) = t.modState { s =>
      tracker.sendAppView(page.id)
      Workbench.State(page, None)
    }

    def error(error: WorkbenchError) = t.modState(s => State(s.selectedPage, Some(error)))

    def componentWillMount(): Unit = {
      currentBackend = Some(this)
    }


  }

  def apply() = {
    val props = Props(Set(HomePage))
    WorkbenchRender.component(props)
  }

  var currentBackend: Option[Backend] = None

  def link(page: Page) = Workbench.currentBackend.map(b => <.a(^.onClick --> b.show(page), ^.href := "#")).get

  def error(error: WorkbenchError) = currentBackend.get.error(error)

}


object WorkbenchRender {

  import Workbench._

  val component = ReactComponentB[Props]("Workbench")
    .initialState(State(HomePage, None))
    .backend(new Backend(_))
    .render((P, S, B) => {
    <.div(
      Header(S.selectedPage),
      S.selectedPage.component
    )
  }).componentWillMount(_.backend.componentWillMount()).build
}
