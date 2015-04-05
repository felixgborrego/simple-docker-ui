package ui.widgets

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

object InfoCard {

  val SMALL = 4
  val LARGE = 12

  case class Props(info: Map[String, String], size: Int, title: Option[String], footer: Option[ReactTag])

  def apply(info: Map[String, String], size: Int = SMALL, title: Option[String] = None, footer: Option[ReactTag] = None) = {
    val props = Props(info, size, title, footer)
    InfoCardRender.component(props)
  }
}

object InfoCardRender {

  import InfoCard._

  val component = ReactComponentB[Props]("InfoCard")
    .render((P) => vdom(P))
    .build

  def vdom(props: Props) =
    <.div(^.className := "container  col-sm-" + props.size,
      <.div(^.className := "panel panel-default",
        props.title.map { title =>
          <.div(^.className := "panel-heading clearfix",
            <.h3(^.className := "panel-title pull-left")(title)
          )
        },
        props.info.map { case (key, value) =>
          <.div(^.className := "list-group",
            <.div(^.className := "list-group-item",
              <.i(^.className := "list-group-item-text")(key),
              <.p(^.className := "list-group-item-heading", ^.wordWrap := "break-word")(value)
            )
          )
        },
        props.footer
      )
    )
}
