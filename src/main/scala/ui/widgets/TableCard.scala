package ui.widgets

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

object TableCard {

  case class Props(data: Seq[Map[String, String]], columnWidth: Map[String, String]) {
    lazy val keys = data.headOption.map(_.keys.toSeq).getOrElse(Seq.empty)
  }

  def apply(data: Seq[Map[String, String]], columnWidth: (String, String)*) = {
    val props = new Props(data, columnWidth.toMap)
    TableCardRender.component(props)
  }
}

object TableCardRender {

  import TableCard._

  val component = ReactComponentB[Props]("TableCard")
    .render((P) => vdom(P))
    .build

  def vdom(props: Props) =
    <.div(^.className := "table-responsive",
      <.table(^.className := "table table-hover table-striped table-condensed",
        <.thead(
          <.tr(props.keys.map(key =>
            <.th(^.className := props.columnWidth.get(key))(key)
          ))
        ),
        <.tbody(
          props.data.map { row =>
            <.tr(props.keys.map { key =>
              <.td(row(key))
            }
            )
          }
        )
      )
    )


}
