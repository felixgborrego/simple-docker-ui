package ui

import japgolly.scalajs.react.ReactElement
import japgolly.scalajs.react.vdom.prefix_<^._

package object pages {

  trait Page {
    def id: String

    def component(ref: WorkbenchRef): ReactElement
  }

  case object EmptyPage extends Page{
    def id= "Empty"
    def component(ref: WorkbenchRef): ReactElement = <.div()
  }
}
