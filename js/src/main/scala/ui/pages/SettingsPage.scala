package ui.pages

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

object SettingsPage extends Page {

  val id = "Settings"
  def component() = {
    SettingsPageRender.component()
  }
}


object SettingsPageRender {

  val dom = <.div("TODO Settings Here!")

  val component = ReactComponentB[Unit]("SettingsPage")
    .render((P) => {
    dom
  }).buildU

}