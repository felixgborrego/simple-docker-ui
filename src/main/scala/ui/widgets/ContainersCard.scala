package ui.widgets

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._
import model.DockerMetadata
import ui.WorkbenchRef
import ui.pages.{ContainersPage, ImagesPage}
import ui.widgets.ContainersCard.Props

object ContainersCard {

  case class Props(docker: DockerMetadata, ref: WorkbenchRef, refresh: () => Unit)

  def apply(docker: DockerMetadata, ref: WorkbenchRef)(refresh: () => Unit) = {
    val props = Props(docker, ref, refresh)
    ContainersCardRender.component(props)
  }
}

object ContainersCardRender {

  val component = ReactComponentB[Props]("ContainersCard")
    .render((P) => vdom(P.docker, P))
    .build

  def vdom(docker: DockerMetadata, P: Props) =
    <.div(^.className := "container  col-sm-8",
      <.div(^.className := "panel panel-default bootcards-summary",
        <.div(^.className := "panel-heading clearfix",
          <.h3(^.className := "panel-title pull-left")(<.i(^.className:="fa fa-pie-chart")," Containers"),
          <.a(^.className := "btn pull-right glyphicon glyphicon-refresh", ^.onClick --> P.refresh())
        ),

        <.div(^.className := "panel-body",
          <.div(^.className := "row",
            <.div(^.className := "col-sm-4",
              P.ref.link(ContainersPage)(^.className := "bootcards-summary-item",
                <.i(^.className := "glyphicon glyphicon3x glyphicon-transfer"),
                <.h4("Running containers", <.span(^.className := "label label-info")(docker.containers.size))
              )
            ),
            <.div(^.className := "col-sm-4",
              P.ref.link(ContainersPage)(^.className := "bootcards-summary-item",
                <.i(^.className := "glyphicon glyphicon3x glyphicon-equalizer"),
                <.h4("All containers", <.span(^.className := "label label-info")(docker.info.Containers))
              )
            ),
            <.div(^.className := "col-sm-4",
              P.ref.link(ImagesPage)(^.className := "bootcards-summary-item",
                <.i(^.className := "glyphicon glyphicon3x glyphicon-cloud-download"),
                <.h4("Images", <.span(^.className := "label label-info")(docker.info.Images))
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
