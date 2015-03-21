package ui.widgets

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._
import model.DockerMetadata

object ServerInfoCard {

  case class Props(docker: DockerMetadata)

  def apply(docker: DockerMetadata) = {
    val props = new Props(docker)
    ServerInfoCardRender.component(props)
  }
}

object ServerInfoCardRender {

  import ServerInfoCard._

  val component = ReactComponentB[Props]("ServerInfoCard")
    .render((P) =>
    vdom(P.docker)
    ).build

  def vdom(docker: DockerMetadata) =
    <.div(^.className := "container  col-sm-4",
      <.div(^.className := "panel panel-default",
        <.div(^.className := "panel-heading clearfix",
          <.h3(^.className := "panel-title pull-left")("System")
        ),
        <.div(^.className := "list-group",
          <.div(^.className := "list-group-item",
            <.i(^.className := "list-group-item-text")("Connected to"),
            <.p(^.className := "list-group-item-heading")(docker.connection.url)
          )
        ),
        <.div(^.className := "list-group",
          <.div(^.className := "list-group-item",
            <.i(^.className := "list-group-item-text")("Version"),
            <.p(^.className := "list-group-item-heading")(docker.version.Version + "(api: " + docker.version.ApiVersion + ")")
          )
        )
      )
    )
}
