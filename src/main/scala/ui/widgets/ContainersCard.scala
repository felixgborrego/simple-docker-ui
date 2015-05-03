package ui.widgets

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model.DockerMetadata
import ui.WorkbenchRef
import ui.pages.{ContainersPage, ImagesPage}
import util.StringUtils._

import scala.concurrent.ExecutionContext.Implicits.global
object ContainersCard {

  case class State(totalImagesSize: String = "", numImages: Int = 0)

  case class Props(docker: DockerMetadata, ref: WorkbenchRef, refresh: () => Unit)


  case class Backend(t: BackendScope[Props, State]) {
    def didMount(): Unit = t.props.ref.client.map { client =>
      client.images().map { images =>
        val totalImagesSize = bytesToSize(images.map(_.VirtualSize.toLong).sum)
        t.modState(_.copy(totalImagesSize = totalImagesSize, numImages = images.size))
      }
    }
  }

  def apply(docker: DockerMetadata, ref: WorkbenchRef)(refresh: () => Unit) = {
    val props = Props(docker, ref, refresh)
    ContainersCardRender.component(props)
  }
}

object ContainersCardRender {
  import ContainersCard._

  val component = ReactComponentB[Props]("ContainersCard")
    .initialState(State())
    .backend(new Backend(_))
    .render((P, S, B) => vdom(P.docker, S, P))
    .componentDidMount(_.backend.didMount)
    .build

  def vdom(docker: DockerMetadata, S: State, P: Props) =
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
                <.h4("All containers", <.span(^.className := "label label-info")(docker.info.Containers)),
                <.i(docker.totalContainersSize)
              )
            ),
            <.div(^.className := "col-sm-4",
              P.ref.link(ImagesPage)(^.className := "bootcards-summary-item",
                <.i(^.className := "glyphicon glyphicon3x glyphicon-cloud-download"),
                <.h4("Images", <.span(^.className := "label label-info")(S.numImages)),
                <.i(S.totalImagesSize)
              )
            )
          )
        ),
        <.div(^.className := "panel-footer",
          <.small(<.strong(S.numImages), " images downloaded & ", <.strong(docker.info.Containers), " containers")
        )
      )
    )


}
