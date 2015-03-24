package ui.widgets

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

object TableCard {

  case class Props(data: Seq[Map[String, String]], title: Option[String])

  def apply(data: Seq[Map[String, String]], title: Option[String] = None) = {
    val props = new Props(data, title)
    TableCardRender.component(props)
  }
}

object TableCardRender {

  import TableCard._

  val component = ReactComponentB[Props]("TableCard")
    .render((P) =>
    vdom(P)
    ).build

  def vdom(props: Props) = {
    val keys = props.data.head.keys.toSeq

    <.div(^.className := "container  col-sm-12",
      <.div(^.className := "panel panel-default",
        props.title.map { title =>
          <.div(^.className := "panel-heading clearfix",
            <.h3(^.className := "panel-title pull-left")(title)
          )
        },
        <.div(^.className := "table-responsive",
          <.table(^.className := "table table-hover",
            <.thead(
              <.tr(
                keys.map(<.th(_))
              )
            ),
            <.tbody(
              props.data.map { row =>
                <.tr(
                  keys.map { key =>
                    <.td(row(key))
                  }
                )
              }
            )
          )
        )
      )
    )
  }
}
