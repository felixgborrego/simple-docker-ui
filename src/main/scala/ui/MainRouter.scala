package ui

import japgolly.scalajs.react.ReactElement
import japgolly.scalajs.react.extra.router.{BaseUrl, Redirect, Router, RoutingRules}
import japgolly.scalajs.react.vdom.prefix_<^._
import ui.pages._
import ui.widgets.Header


object MainRouter extends RoutingRules {
  val baseUrl = BaseUrl.fromWindowOrigin + "/index.html"
  println("test  " + baseUrl)


  val root = register(rootLocation(HomePage()))
  val settingsPage = register(location("#settings", SettingsPage()))
  val containersPage = register(location("#containers", ContainersPage()))
  val imagesPage = register(location("#images", ImagesPage()))

  // initialize router and its React component
  val router = routingEngine(baseUrl)
  val routerComponent = Router.component(router)


  override protected val notFound = redirect(root, Redirect.Replace)

  override protected def interceptRender(i: InterceptionR): ReactElement = {
    <.div(
      Header(i.loc.path),
      i.element,
      <.footer(^.className := "footer")(
        <.div(^.className := "container",
          <.p("Manager for Docker. Prototype built using scala.js & React.js! ",
            <.a(^.href := "https://github.com/felixgborrego/docker-ui-chrome-app")("https://github.com/felixgborrego/docker-ui-chrome-app")
          )
        )
      )
    )
  }

}

object Links {

  import MainRouter._

  def homeLink = router.link(root)

  def settingsLink = router.link(settingsPage)

  def containersLink = router.link(containersPage)

  def imagesLink = router.link(imagesPage)
}
