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
  val settingsPageLoc = register(location("#settings", SettingsPage()))
  val containersPageLoc = register(location("#containers", ContainersPage()))
  val imagesPageLoc = register(location("#images", ImagesPage()))

  private val containerPathMatch = "^#containers/(.+)$".r
  register(parser { case containerPathMatch(n) => n }.location(n => ContainerPage(n)))
  val containerPageLoc = dynLink[String](n => s"#containers/$n")

  // initialize router and its React component
  val router = routingEngine(baseUrl, Router.consoleLogger)
  val routerComponent = Router.component(router)


  override protected val notFound = redirect(root, Redirect.Replace)

  override protected def interceptRender(i: InterceptionR): ReactElement = {
    <.div(
      Header(i.loc.path),
      i.element
    )
  }

}

object Links {

  import MainRouter._

  def homeLink = router.link(root)

  def settingsLink = router.link(settingsPageLoc)

  def containersLink = router.link(containersPageLoc)

  def imagesLink = router.link(imagesPageLoc)

  def containerLink(containerId: String) = router.link(containerPageLoc(containerId))
}
