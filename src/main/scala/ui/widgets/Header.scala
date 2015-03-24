package ui.widgets


import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.extra.router.Path
import japgolly.scalajs.react.vdom.prefix_<^._
import ui.{Links, MainRouter}

object Header {

  case class Props(container: Boolean, images: Boolean, home: Boolean, settings: Boolean) {

    val Active = Some("active")
    def isHomeActive = if(home) Active else None
    def isImagesActive = if(images) Active else None
    def isContainerActive = if(container) Active else None
    def isSettingsActive = if(settings) Active else None

  }

  def apply(path: Path) = {

    val props = Props(
      container = path == MainRouter.containersPageLoc.path,
      home = path == MainRouter.root.path,
      images = path == MainRouter.imagesPageLoc.path,
      settings = path == MainRouter.settingsPageLoc.path
    )

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
          <.img(^.src := "res/img/logo_small.png", ^.className := "img-rounded")
        )
      ),
      <.div(^.id := "navbarCollapse", ^.className := "collapse navbar-collapse",
        <.ul(^.className := "nav navbar-nav",
          <.li(^.className :=  props.isHomeActive , Links.homeLink("Home")),
          <.li(^.className :=  props.isContainerActive, Links.containersLink("Containers")),
          <.li(^.className :=  props.isImagesActive, Links.imagesLink("Images")),
          <.li(^.className :=  props.isSettingsActive, Links.settingsLink("Settings"))
        )
      )
    )
  }
}
