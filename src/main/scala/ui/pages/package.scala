package ui.pages

import japgolly.scalajs.react.ReactElement
import japgolly.scalajs.react.vdom.prefix_<^._
import ui.WorkbenchRef

trait Page {
  def id: String

  def component(ref: WorkbenchRef): ReactElement
}

case object EmptyPage extends Page {
  def id = "Empty"

  def component(ref: WorkbenchRef): ReactElement = <.div()
}
