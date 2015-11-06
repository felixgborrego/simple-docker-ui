package ui.widgets


import api.ConfigStorage
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import ui.WorkbenchRef
import ui.pages._
import scala.concurrent.ExecutionContext.Implicits.global

object Header {

  case class State(url: String = "", savedUrls: Seq[String] = Seq.empty)
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

  case class Backend(t: BackendScope[Props, State]) {
    def willMount(): Unit = loadProps(t.props)

    def loadProps(props: Props) = t.modState { s =>
      val workbenchState = props.workbenchRef.state
      val url = workbenchState.connection.map(_.url).getOrElse("")
      val savedConnections = workbenchState.savedConnections.map(_.url)
      s.copy(url = url, savedUrls = savedConnections)
    }

    def willReceiveProps(newProps: Props): Unit = loadProps(newProps)

    def select(url:String) = for{
      _ <- ConfigStorage.saveConnection(url)
      _ <- t.props.workbenchRef.reconnect()
    } yield {
        t.props.workbenchRef.show(SettingsPage)
        t.props.workbenchRef.show(HomePage)
      }
  }

}

object HeaderRender {

  import ui.widgets.Header._

  val component = ReactComponentB[Props]("AppHeader")
    .initialState(State())
    .backend(new Backend(_))
    .render((P, S, B) => vdom(P, S, B))
    .componentWillMount(_.backend.willMount)
    .componentWillReceiveProps((scope, newProps) => scope.backend.willReceiveProps(newProps))
    .build

  val data_toggle = "data-toggle".reactAttr
  val data_target = "data-target".reactAttr

  def vdom(P: Props, S: State, B: Backend) =
    <.nav(^.className := "navbar navbar-default navbar-fixed-top", ^.role := "navigation",
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
            <.li(^.className := P.isHomeActive, P.workbenchRef.link(HomePage)(<.span(^.className := "glyphicon glyphicon-home"), " Home")),
            <.li(^.className := P.isContainerActive, P.workbenchRef.link(ContainersPage)(<.span(^.className := "glyphicon glyphicon-equalizer"), " Containers")),
            <.li(^.className := P.isImagesActive, P.workbenchRef.link(ImagesPage)(<.span(^.className := "glyphicon glyphicon-picture"), " Images")),
            <.li(^.className := P.isSettingsActive, P.workbenchRef.link(SettingsPage)(<.span(^.className := "glyphicon glyphicon-wrench"), " Settings"))
          ),
          (S.savedUrls.size > 1) ?= <.ul(^.className := "nav navbar-nav navbar-right navbar-right-margin",
            <.li(^.className := "dropdown",
              <.a(^.className := "dropdown-toggle", data_toggle := "dropdown", ^.role := "button",
                ^.aria.haspopup := "true", ^.aria.expanded := "false", S.url,
                <.span(^.className := "caret")
              ),
              <.ul(^.className := "dropdown-menu",
                S.savedUrls.map { url =>
                  <.li(<.a(url, ^.onClick --> B.select(url)))
                }
              )
            )
          )
        )
    )
}
