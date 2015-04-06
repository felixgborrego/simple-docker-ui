package ui.widgets


import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._
import ui.WorkbenchRef
import ui.pages._


object Header {

  case class Props(workbenchRef: WorkbenchRef) {

    def selected: Page = workbenchRef.selectedPage

    val Active = Some("active")

    def isHomeActive = if (selected.id == HomePage.id) Active else None

    def isImagesActive = if (selected.id == ImagesPage.id) Active else None

    def isContainerActive = if (selected.id == ContainersPage.id) Active else None

    def isSettingsActive = if (selected.id == SettingsPage.id) Active else None

  }

  def apply(workbenchRef: WorkbenchRef) = {
    val props = Props(workbenchRef)
    HeaderRender.component(props)
  }
}

object HeaderRender {

  import ui.widgets.Header._

  val component = ReactComponentB[Props]("AppHeader")
    .render((P) => vdom(P))
    .build

  val data_toggle = "data-toggle".reactAttr
  val data_target = "data-target".reactAttr

  def vdom(props: Props) =
    <.nav(^.className := "navbar navbar-default navbar-fixed-top", ^.role := "navigation",
      //<.div(^.className := "container-fluid",
        <.div(^.className := "navbar-header",
          <.a(^.href := "#", ^.className := "navbar-brand",
            <.img(^.src := "./img/logo_small.png", ^.className := "img-rounded")
          ),
          <.button(^.`type` := "button", ^.className := "navbar-toggle collapsed", data_toggle := "collapse", data_target := "#navbarCollapse",
            <.span(^.className := "sr-only")("Toggle navigation"),
            <.span(^.className := "icon-bar"),
            <.span(^.className := "icon-bar"),
            <.span(^.className := "icon-bar")
          )
        ),

        <.div(^.id := "navbarCollapse", ^.className := "collapse navbar-collapse",
          <.ul(^.className := "nav navbar-nav",
            <.li(^.className := props.isHomeActive, props.workbenchRef.link(HomePage)(<.span(^.className := "glyphicon glyphicon-home"), " Home")),
            <.li(^.className := props.isContainerActive, props.workbenchRef.link(ContainersPage)(<.span(^.className := "glyphicon glyphicon-equalizer"), " Containers")),
            <.li(^.className := props.isImagesActive, props.workbenchRef.link(ImagesPage)(<.span(^.className := "glyphicon glyphicon-picture"), " Images")),
            <.li(^.className := props.isSettingsActive, props.workbenchRef.link(SettingsPage)(<.span(^.className := "glyphicon glyphicon-wrench"), " Settings"))
          )
        )
    //  )
    )
}
