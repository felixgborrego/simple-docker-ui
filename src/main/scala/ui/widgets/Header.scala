package ui.widgets


import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._
import ui.{WorkbenchRef, Workbench}
import ui.pages._


object Header {

  case class Props(workbenchRef: WorkbenchRef) {

    def selected:Page = workbenchRef.selectedPage
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
    .render((P) =>
    vdom(P)
    ).build


  def vdom(props: Props) = {

    <.nav(^.className := "navbar navbar-default", ^.role := "navigation",
      <.div(^.className := "navbar-header",
        <.a(^.href := "#", ^.className := "navbar-brand",
          <.img(^.src := "./img/logo_small.png", ^.className := "img-rounded")
        )
      ),
      <.div(^.id := "navbarCollapse", ^.className := "collapse navbar-collapse",
        <.ul(^.className := "nav navbar-nav",
          <.li(^.className := props.isHomeActive, props.workbenchRef.link(HomePage)("Home")),
          <.li(^.className := props.isContainerActive, props.workbenchRef.link(ContainersPage)("Containers")),
          <.li(^.className := props.isImagesActive, props.workbenchRef.link(ImagesPage)("Images")),
          <.li(^.className := props.isSettingsActive, props.workbenchRef.link(SettingsPage)("Settings"))
        )
      )
    )
  }
}
