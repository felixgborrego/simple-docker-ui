package ui.pages

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

object SettingsPage {

  def apply() = {
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