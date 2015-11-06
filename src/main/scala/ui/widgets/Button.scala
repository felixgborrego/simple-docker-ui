package ui.widgets

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import org.scalajs.jquery.jQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Button {

  case class State(running: Boolean = false)

  case class Props(text: String, icon: String, title: String, disabled: Boolean, command: () => Future[Any])

  case class Backend(t: BackendScope[Props, State]) {

    def className =
      if (t.state.running)
        "glyphicon glyphicon-refresh glyphicon-spin"
      else
        "glyphicon " + t.props.icon

    def text = t.props.text

    def click() = {
      t.modState(s => s.copy(running = true))
      t.props.command().andThen { case _ =>
        t.modState(s => s.copy(running = false))
      }
    }

    def didMount() = {
      val element = jQuery(t.getDOMNode()).asInstanceOf[scalajs.js.Dynamic]
      element.tooltip()
    }
  }

  def apply(text: String, icon: String, title: String = "", disabled: Boolean = false)(command: => Future[Any]) =
    ButtonRender.component(Props(text, icon, title, disabled, () => command))
}

private object ButtonRender {

  import ui.widgets.Button._

  val component = ReactComponentB[Props]("Button")
    .initialState(State())
    .backend(new Backend(_))
    .render((P, S, B) => vdom(P, S, B))
    .componentDidMount(_.backend.didMount())
    .build

  val data_toggle = "data-toggle".reactAttr
  val data_placement = "data-placement".reactAttr
  val title = "title".reactAttr
  val data_container = "data-container".reactAttr

  def vdom(P: Props, S: State, B: Backend) =
    <.button(^.id := "test", ^.className := "btn btn-default", ^.onClick --> B.click(),
      data_toggle := "tooltip", title := P.title, data_placement := "top", data_container := "body",
      (P.disabled || S.running) ?= (^.disabled := "disabled"),
      <.i(^.className := B.className), <.i(B.text)
    )
}

