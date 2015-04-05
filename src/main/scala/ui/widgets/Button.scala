package ui.widgets

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Button {

  case class State(running: Boolean = false)

  case class Props(val text: String, val icon: String, var command: () => Future[Any])

  case class Backend(t: BackendScope[Props, State]) {

    def className =
      if (t.state.running)
        "glyphicon glyphicon-refresh glyphicon-spin"
      else
        "glyphicon glyphicon " + t.props.icon

    def text = if (t.state.running) "" else t.props.text

    def click() = {
      t.modState(s => s.copy(running = true))
      t.props.command().map { _ =>
        t.modState(s => s.copy(running = false))
      }
    }
  }

  def apply(text: String, icon: String)(command: => Future[Any]) =
    ButtonRender.component(Props(text, icon, () => command))
}

private object ButtonRender {

  import ui.widgets.Button._

  val component = ReactComponentB[Props]("Button")
    .initialState(State())
    .backend(new Backend(_))
    .render((P, S, B) => vdom(B))
    .build

  def vdom(B: Backend) =
    <.button(^.className := "btn btn-default", ^.onClick --> B.click(),
      <.i(^.className := B.className, B.text)
    )


}

