package ui.pages

import api.{ConfigStore, DockerClient}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model.{Connection, ContainerInfo, ContainerTop}

import ui.widgets.{Alert, InfoCard, TableCard}

import scala.concurrent.ExecutionContext.Implicits.global

object ContainerPage {

  case class State(info: Option[ContainerInfo] = None,
                   top: Option[ContainerTop] = None)

  case class Props(connection: Connection, containerId: String)

  case class Backend(t: BackendScope[Props, State]) {

    def willStart(): Unit = {
      for {
        info <- DockerClient(t.props.connection).containerInfo(t.props.containerId)
        top <- DockerClient(t.props.connection).top(t.props.containerId)
      } yield t.modState(s => State(Some(info), Some(top)))
    }
  }

  def apply(containerId: String) = new Page{
    val id = ContainersPage.id
    def component() = {
      val props = Props(ConfigStore.connection, containerId)
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
        S.info.map(vdomInfo),
        S.top.map(vdomTop), vdomLogs()
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
      "Command" -> containerInfo.Config.Cmd.mkString(" "),
      "Arguments" -> containerInfo.Args.mkString(" "),
      "Environment" -> containerInfo.Config.Env.mkString(" "),
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