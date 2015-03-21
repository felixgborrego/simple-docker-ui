package ui.pages

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

object ImagesPage {


  def apply() = {
    ImagesPageRender.component({})
  }
}


object ImagesPageRender {
  val dom = <.div("TODO Docker Images here!")

  val component = ReactComponentB[Unit]("ImagesPage")
    .render((P) => {
    dom
  }).build

}