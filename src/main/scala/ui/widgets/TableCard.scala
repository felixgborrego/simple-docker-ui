package ui.widgets

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

object TableCard {

  case class Props(data: Seq[Map[String, String]])

  def apply(data: Seq[Map[String, String]]) = {
    val props = new Props(data)
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
    val keys = props.data.headOption.map(_.keys.toSeq) getOrElse (Seq.empty)
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


  }
}
