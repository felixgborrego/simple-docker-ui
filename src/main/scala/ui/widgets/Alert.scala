package ui.widgets

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

object Alert {

  case class Props(msg: String, links: Option[ReactTag], style: String)

  def apply(msg: String, links: Option[ReactTag] = None, style: String = "warning") =
    AlertRender.component(Props(msg, links, style))
}

private object AlertRender {

  import ui.widgets.Alert.Props

  val component = ReactComponentB[Props]("AppHeader")
    .render((P) => vdom(P))
    .build

  final val dataDismiss = "data-dismiss".reactAttr

  def vdom(props: Props) =
    <.div(^.className := "panel",
      <.div(^.className := s"alert alert-${props.style}",
        props.msg, <.br(),
        props.links.map(_("Go to Settings")(^.className := "alert-link"))
      )
    )

}

