package ui.pages

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model.{ContainerInfo, ContainerTop}
import ui.WorkbenchRef
import ui.widgets.{Alert, InfoCard, TableCard}
import util.logger._

import scala.concurrent.ExecutionContext.Implicits.global

object ContainerPage {

  case class State(info: Option[ContainerInfo] = None,
                   top: Option[ContainerTop] = None,
                   error: Option[String] = None)

  case class Props(ref: WorkbenchRef, containerId: String)

  case class Backend(t: BackendScope[Props, State]) {

    def willStart(): Unit = {
      t.props.ref.client.map { client =>
        val result = for {
          info <- client.containerInfo(t.props.containerId)
          top <- client.top(t.props.containerId)
        } yield t.modState(s => s.copy(Some(info), Some(top)))

        result.onFailure {
          case ex: Exception =>
            log.error("Unable to get Metadata", ex)
            t.modState(s => s.copy(None, None, Some("Unable to get data: " + ex.getMessage)))
        }
      }

    }
  }

  def apply(containerId: String, ref: WorkbenchRef) = new Page {
    val id = ContainersPage.id

    def component(ref: WorkbenchRef) = {
      val props = Props(ref, containerId)
      ContainerPageRender.component(props)
    }
  }
}

object ContainerPageRender {

  import ui.pages.ContainerPage._

  val component = ReactComponentB[Props]("ContainerPage")
    .initialState(State())
    .backend(new Backend(_))
    .render((P, S, B) => {
    <.div(
      S.error.map(Alert(_, Some(P.ref.link(SettingsPage)))),
      S.info.map(vdomInfo),
      S.top.map(vdomTop),
      vdomLogs()
    )
  }).componentWillMount(_.backend.willStart())
    .build

  def vdomInfo(containerInfo: ContainerInfo) = {
    val generalInfo = Map(
      "Id / Name" -> containerInfo.id,
      "Image" -> containerInfo.image,
      "Created" -> containerInfo.created,
      "Status" -> "---"
    )
    val executionInfo = Map(
      "Command" -> containerInfo.Config.cmd.mkString(" "),
      "Arguments" -> containerInfo.Args.mkString(" "),
      "Environment" -> containerInfo.Config.env.mkString(" "),
      "WorkingDir" -> containerInfo.Config.WorkingDir
    )
    val networkInfo = Map(
      "Public Ip" -> "---",
      "Port Mapping" -> "---",
      "Volumes" -> "---"
    )
    <.div(
      InfoCard(generalInfo),
      InfoCard(executionInfo),
      InfoCard(networkInfo)
    )
  }


  def vdomTop(top: ContainerTop) = {
    val keys = top.Titles
    val values = top.Processes.map(data => keys.zip(data).toMap)
    println(values)
    <.div(
      TableCard(values, Some("Processes running inside the container"))
    )
  }

  def vdomLogs() = {
    InfoCard(Map.empty, InfoCard.LARGE, Some("Logs"))
  }
}