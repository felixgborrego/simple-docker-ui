package ui.widgets

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._
import model.DockerMetadata
import ui.widgets.ContainersCard.Props

object ContainersCard {
  case class Props(docker: DockerMetadata)

  def apply(docker: DockerMetadata) = {
    val props = Props(docker)
    ContainersCardRender.component(props)
  }
}

object ContainersCardRender {

  val component = ReactComponentB[Props]("ContainersCard")
    .render((P) =>
     vdom(P.docker)
    ).build

  def vdom(docker: DockerMetadata) =
    <.div(^.className := "container  col-sm-5",
      <.div(^.className := "panel panel-default bootcards-summary",
        <.div(^.className := "panel-heading clearfix",
          <.h3(^.className := "panel-title pull-left")("Containers"),
          <.a(^.className := "btn pull-right glyphicon glyphicon-refresh", ^.href := "#")
        ),

        <.div(^.className := "panel-body",
          <.div(^.className := "row",
            <.div(^.className := "col-sm-4",
              <.a(^.className := "bootcards-summary-item", ^.href := "#",
                <.i(^.className := "glyphicon glyphicon3x glyphicon-cloud-download"),
                <.h4("Images", <.span(^.className := "label label-info")(docker.info.Images))
              )
            ),
            <.div(^.className := "col-sm-4",
              <.a(^.className := "bootcards-summary-item", ^.href := "#",
                <.i(^.className := "glyphicon glyphicon3x glyphicon-transfer"),
                <.h4("Running containers", <.span(^.className := "label label-info")(docker.containers.size))
              )
            ),
            <.div(^.className := "col-sm-4",
              <.a(^.className := "bootcards-summary-item", ^.href := "#",
                <.i(^.className := "glyphicon glyphicon3x glyphicon-equalizer"),
                <.h4("All containers", <.span(^.className := "label label-info")(docker.info.Containers))
              )
            )
          )
        ),
        <.div(^.className := "panel-footer",
          <.small(<.strong(docker.info.Images), " images downloaded & ", <.strong(docker.info.Containers), " containers")
        )
      )
    )


}
