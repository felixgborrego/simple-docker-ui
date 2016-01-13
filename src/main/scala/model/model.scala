package model

import util.StringUtils._
import util.collections._
import util.momentJs.Moment

import scala.collection.immutable.ListMap
import scala.util.Try

/**
 * This models match with the Json coming from the Docker Remote API.
 * Note: Docker Api use upper camel case, using here the same to make parsing easier.
 */


case class Connection(url: String) {
  import util.StringUtils._
  def ip = substringBefore(substringAfter(url, "://"), ":")
}

case class DockerMetadata(connection: Connection,
                          info: Info,
                          version: Version,
                          containers: Seq[Container]) {

  def totalContainersSize = bytesToSize(containers.map(c => c.SizeRootFs + c.SizeRw).sum)
}

case class Info(Containers: Int,
                Driver: String,
                ExecutionDriver: String,
                Images: Int,
                IndexServerAddress: String,
                InitPath: String,
                KernelVersion: String,
                MemTotal: Float,
                NEventsListener: Int,
                NGoroutines: Int,
                DriverStatus: Seq[Seq[String]] = Seq.empty,
                OperatingSystem: String) {

  lazy val allDriverStatus: Seq[(String, String)] = Try(DriverStatus.map {
    case Seq(property, value) => Some(property.replace("\b", "") -> value)
    case _ => None
  }.flatten).getOrElse(Seq.empty)

  lazy val isSwarmMaster = allDriverStatus.exists { case (property, _) => property == "Nodes" }

  lazy val swarmMasterInfo: ListMap[String, String] = if (isSwarmMaster) {
    ListMap(allDriverStatus.takeTo { case (property, value) => property != "Nodes" }: _*)
  } else {
    ListMap.empty
  }

  lazy val swarmNodesInfo = allDriverStatus.dropWhile { case (property, value) => property != "Nodes" }.drop(1)

  lazy val swarmNodesDescription: Seq[ListMap[String, String]] = {
    val (nodes, lastNode) = swarmNodesInfo.foldLeft((Seq.empty[ListMap[String, String]], ListMap.empty[String, String])) {
      case ((result, currentNode), (property, value)) =>
        if (property.startsWith(" ")) {
          val nodeAcc = currentNode + (property -> value)
          (result, nodeAcc)
        } else {
          (result :+ currentNode, ListMap((property -> value)))
        }

    }
    (nodes :+ lastNode).filterNot(_.isEmpty)
  }
}

case class Version(ApiVersion: String,
                   Arch: String,
                   GitCommit: String,
                   GoVersion: String,
                   KernelVersion: String,
                   Os: String,
                   Version: String) {

  def apiVersion: (Int, Int) = ApiVersion.split( """\.""").map(_.toInt) match {
    case Array(mayor, minor, _*) => (mayor, minor)
  }
}

case class Container(Command: String,
                     Created: Int,
                     Id: String,
                     Image: String,
                     Status: String,
                     Names: Seq[String],
                     SizeRootFs: Int = 0,
                     SizeRw: Int = 0,
                     val Ports: Seq[Port] = Seq.empty
                      ) {
  def id = subId(Id)


  def created = {
    val timeStamp = Created.toLong * 1000L
    Moment(timeStamp).fromNow()
  }

  def ports = Ports.map { p =>
    val external = if (p.PublicPort == 0) "" else p.PublicPort + "->"
    val internal = if (p.PrivatePort == 0) "" else p.PrivatePort
    external + internal
  }.reverse
}

case class ContainersInfo(running: Seq[Container] = Seq.empty,
                          all: Seq[Container] = Seq.empty) {
  def history = all.diff(running)
}


case class ContainerInfo(Args: Seq[String],
                         Id: String,
                         Image: String,
                         Name: String,
                         Path: String,
                         Created: String,
                         Config: ContainerConfig,
                         State: ContainerState,
                         Volumes: Map[String, String] = Map.empty,
                         HostConfig: HostConfig,
                         NetworkSettings: NetworkSettings) {
  def id = subId(Id)

  def image = subId(Image)

  def created = {
    Moment(Created).fromNow()
  }

}

case class ContainerState(Running: Boolean)

case class ContainerConfig(AttachStderr: Boolean = false,
                           AttachStdin: Boolean = false,
                           AttachStdout: Boolean = false,
                           Cmd: Seq[String] = Seq.empty,
                           Entrypoint: Seq[String] = Seq.empty,
                           Env: Seq[String] = Seq.empty,
                           Hostname: String = "",
                           Image: String = "",
                           OpenStdin: Boolean = false,
                           StdinOnce: Boolean = false,
                           Tty: Boolean = false,
                           User: String = "",
                           WorkingDir: String = "",
                           Volumes: Map[String, VolumeEmptyHolder] = Map.empty,
                           ExposedPorts: Map[String, NetworkSettingsPort] = Map.empty) {

  // Workaround, Docker may return null
  val cmd = Option(Cmd).getOrElse(Seq.empty)
  val env = Option(Env).getOrElse(Seq.empty)
  val exposedPorts = Option(ExposedPorts).getOrElse(Map.empty)
  val volumes = Option(Volumes).getOrElse(Map.empty)
  val portBindings = exposedPorts.map { case (containerPort, hostInfo) => (containerPort, Seq(hostInfo)) }
}

case class Port(Type: String,
                IP: String = "",
                PrivatePort: Int = 0,
                PublicPort: Int = 0)

case class ContainerTop(Titles: Seq[String],
                        Processes: Seq[Seq[String]])


case class Image(Created: Int, Id: String,
                 ParentId: String,
                 RepoTags: Seq[String],
                 Size: Int,
                 VirtualSize: Int) {
  def created = {
    val timeStamp = Created.toLong * 1000L
    Moment(timeStamp).fromNow()
  }

  def id = subId(Id)

  def virtualSize = bytesToSize(VirtualSize)
}

case class NetworkSettings(IPAddress: String,
                           Ports: Map[String, Seq[NetworkSettingsPort]] = Map.empty) {
  def ports: Seq[(String, String)] = Option(Ports).map { p =>
    p.map { case (port, settingsPorts) =>
      Option(settingsPorts).getOrElse(Seq.empty).map(_.HostPort).map((_, port))
    }.flatten.toSeq
  }.getOrElse(Seq.empty)

  //def ports: Seq[(String, String)] = Seq.empty
}

case class NetworkSettingsPort(HostIp: String = "",
                               HostPort: String = "")

// https://docs.docker.com/reference/api/docker_remote_api_v1.17/#exec-create
case class ExecCreated(Id: String)

// https://docs.docker.com/reference/api/docker_remote_api_v1.17/#exec-start
case class ExecStart(Detach: Boolean,
                     Tty: Boolean)

//  https://docs.docker.com/reference/api/docker_remote_api_v1.17/#inspect-an-image
case class ImageInfo(Id: String,
                     Created: String,
                     Config: ContainerConfig,
                     Architecture: String,
                     Author: String,
                     Comment: String,
                     Container: String,
                     DockerVersion: String,
                     Os: String,
                     Parent: String,
                     Size: Int,
                     VirtualSize: Int)

// https://docs.docker.com/reference/api/docker_remote_api_v1.17/#get-the-history-of-an-image
case class ImageHistory(Created: Int,
                        CreatedBy: String,
                        Id: String,
                        Size: Int,
                        Tags: Seq[String] = Seq.empty) {
  def created = {
    val timeStamp = Created.toLong * 1000L
    Moment(timeStamp).fromNow()
  }

  def id = subId(Id) + tags

  def tags = Option(Tags).getOrElse(Seq.empty).mkString(" ", ", ", " ")

  def size = bytesToSize(Size)
}

//https://docs.docker.com/reference/api/docker_remote_api_v1.17/#search-images
case class ImageSearch(description: String,
                       is_official: Boolean,
                       name: String,
                       star_count: Int)

case class PullProgressEvent(status: String = "",
                             id: String = "",
                             progressDetail: ProgressEventDetail = ProgressEventDetail(),
                             progress: String = "")

case class ProgressEventDetail(current: Int = 0,
                               total: Int = 0,
                               start: Int = 0) {
  def startM = {
    val timeStamp = start.toLong * 1000L
    Moment(timeStamp).fromNow()
  }
}


case class CreateContainerRequest(AttachStdin: Boolean,
                                  AttachStdout: Boolean,
                                  AttachStderr: Boolean,
                                  OpenStdin: Boolean,
                                  Cmd: Seq[String],
                                  Image: String,
                                  Tty: Boolean,
                                  HostConfig: HostConfig,
                                  Volumes: Map[String, VolumeEmptyHolder] = Map.empty[String, VolumeEmptyHolder],
                                  VolumesRW: Map[String, Boolean] = Map.empty[String, Boolean],
                                  ExposedPorts: Map[String, NetworkSettingsPort] = Map.empty[String, NetworkSettingsPort],
                                  Env: Seq[String] = Seq.empty,
                                  name: String) {
  val cmd = Option(Cmd).getOrElse(Seq.empty)
}

case class VolumeEmptyHolder(info: Option[String] = None)

// HostConfig for https://docs.docker.com/reference/api/docker_remote_api_v1.17/#create-a-container
case class HostConfig(PublishAllPorts: Boolean,
                      Binds: Seq[String],
                      Links: Seq[String],
                      PortBindings: Map[String, Seq[NetworkSettingsPort]] = Map.empty) {
  lazy val links =  Option(Links).getOrElse(Seq.empty).mkString(", ")
}

case class CreateContainerResponse(Id: String, Warnings: Seq[String] = Seq.empty)

// https://docs.docker.com/reference/api/docker_remote_api_v1.17/#monitor-dockers-events
case class DockerEvent(status: String, id: String, from: String = "", time: Double) {
  def since = Moment(time * 1000).fromNow()

  def shortId = subId(id)
}

// https://docs.docker.com/reference/api/docker_remote_api_v1.17/#inspect-changes-on-a-containers-filesystem
case class FileSystemChange(Path: String, Kind: Int) {
  def kind = Kind match {
    case 2 => "Delete"
    case 1 => "Add"
    case 0 => "Update"
    case x => s"Unknown $x"
  }
}

package stats {

  case class Network(rx_bytes: Int, rx_packets: Int, rx_errors: Int, rx_dropped: Int, tx_bytes: Int, tx_packets: Int, tx_errors: Int, tx_dropped: Int)

  case class CpuUsage(total_usage: Double,
                      percpu_usage: Seq[Double],
                      usage_in_kernelmode: Double,
                      usage_in_usermode: Double)

  case class ThrottlingData(periods: Double, throttled_periods: Double, throttled_time: Double)

  case class MemoryStats(usage: Double, max_usage: Double, /*stats: MemoryStatsStats,*/ failcnt: Double, limit: Double)

  case class PrecpuStats(cpu_usage: CpuUsage,
                         system_cpu_usage: Double,
                         throttling_data: ThrottlingData)

  case class ContainerStats(read: String,
                            network: Network,
                            precpu_stats: PrecpuStats,
                            cpu_stats: PrecpuStats,
                            memory_stats: MemoryStats
                            /* blkio_stats: BlkioStats*/) {


    def cpuPercent: Double = {
      val previousCpu = precpu_stats.cpu_usage.total_usage
      val previousSystem = precpu_stats.system_cpu_usage

      // change for the cpu usage of the container in between readings
      val cpuDelta = cpu_stats.cpu_usage.total_usage - previousCpu
      //change for the entire system between readings
      val systemDelta = cpu_stats.system_cpu_usage - previousSystem

      if (systemDelta > 0.0 && cpuDelta > 0.0)
        (cpuDelta / systemDelta) * cpu_stats.cpu_usage.percpu_usage.size * 100.0
      else
        0.0
    }

    def memory: Double = memory_stats.usage

    def memoryLimit: Double = memory_stats.limit

    def memPercent: Double = if (memory_stats.limit != 0.0)
      memory_stats.usage / memory_stats.limit * 100.0
    else
      0

  }

}