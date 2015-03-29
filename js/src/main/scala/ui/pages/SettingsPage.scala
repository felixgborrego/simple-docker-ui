package ui.pages

import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._
import ui.WorkbenchRef

object SettingsPage extends Page {

  val id = "Settings"

  case class Props(ref: WorkbenchRef)

  def component(ref: WorkbenchRef) = {
    val props = Props(ref)
    SettingsPageRender.component(props)
  }
}


object SettingsPageRender {

  import SettingsPage._

  val dom = <.div(
    <.div(^.className := "container  col-sm-10",
      <.div(^.className := "panel panel-default",
        <.div(^.className := "panel-heading clearfix",
          <.h3(^.className := "panel-title pull-left", "Connection to Docker Remote Api"),
          <.div(^.className := "btn-group pull-right",
            <.button(^.className := "btn btn-warning",
              <.i(^.className := "fa fa-times", "Test")
            ),
            <.button(^.className := "btn btn-success",
              <.i(^.className := "fa fa-check", "Save")
            )
          )
        ),
        <.div(^.className := "modal-body",
          <.form(^.className := "form-horizontal",
            <.div(^.className := "form-group",
              <.label(^.className := "col-xs-3 control-label", "Url"),
              <.div(^.className := "col-xs-9",
                <.input(^.`type` := "text", ^.className := "form-control", ^.value := "http://localhost:1233"
                )

              )
            ),
            <.div(^.className := "panel-footer",
              <.small("Connected!!")
            )
          ),

          <.div(^.className := "panel panel-default",
            <.div(^.className := "panel-heading",
              <.h3(^.className := "panel-title", "Linux config")
            ),
            <.div(^.className := "list-group",
              <.div(^.className := "list-group-item",
                <.p(^.className := "list-group-item-text",
                  "To allow Docker ui to connect to Docker you need to enable      Docker Remote API. To do so you need to:",
                  <.ul(
                    <.li("Edit ", <.code("/etc/default/docker"), " to allow connections adding:", <.br(),
                      <.code("DOCKER_OPTS='-H tcp://0.0.0.0:4243 -H unix:///var/run/docker.sock'")
                    ),
                    <.li("Restart the Docker service using:", <.br(),
                      <.code("sudo service docker restart")
                    )
                  )
                )
              )
            )
          ),


          <.div(^.className := "panel panel-default",
            <.div(^.className := "panel-heading",
              <.h3(^.className := "panel-title", "MacOS config")
            ),
            <.div(^.className := "list-group",
              <.div(^.className := "list-group-item",
                <.p(^.className := "list-group-item-text",
                  "To allow Docker ui to connect to Docker you need to allow Chrome to use the ssl credentials used by boot2Docker.",
                  <.ul(
                    <.li("Add credentials to your Keychain", <.br(),
                      <.code("security import key.pem -k ~/Library/Keychains/login.keychain")
                    ),
                    <.li("Try to reconnect!")
                  )
                )
              )
            )
          )
        )
      )

    )
  )


  val component = ReactComponentB[Props]("SettingsPage")
    .render((P) => {
    dom
  }).build

}