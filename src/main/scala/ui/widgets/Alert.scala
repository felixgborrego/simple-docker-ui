package ui.widgets

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

object Alert {

  case class Props(msg: String, links: Option[ReactTag])

  def apply(msg: String, links: Option[ReactTag] = None) =
    AlertRender.component(Props(msg, links))
}

private object AlertRender {

  import ui.widgets.Alert.Props

  val component = ReactComponentB[Props]("AppHeader")
    .render((P) =>
    vdom(P)
    ).build

  // data-dismiss="alert"
  final val dataDismiss = "data-dismiss".reactAttr

  def vdom(props: Props) =
    <.div(^.className := "panel",
      <.div(^.className := "alert alert-danger",
        <.strong("Error!  "),
        props.msg,
        props.links.map(_(" Go to Settings")(^.className := "alert-link"))
      )
    )

}

