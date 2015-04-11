import util.momentJs.Moment
import util.stringUtils._

/**
 * This models match with the Json coming from the Docker Remote API.
 * Note: Docker Api use upper camel case, using here the same to make parsing easier.
 */
package object model {

  case class Connection(url: String) {
    import util.stringUtils._
    def ip = substringBefore(substringAfter(url, "://"), ":")
  }

  case class DockerMetadata(connection: Connection,
                            info: Info,
                            version: Version,
                            containers: Seq[Container])

  case class Info(Containers: Int,
                  Debug: Int,
                  Driver: String,
                  ExecutionDriver: String,
                  IPv4Forwarding: Int,
                  Images: Int,
                  IndexServerAddress: String,
                  InitPath: String,
                  KernelVersion: String,
                  MemTotal: Float,
                  MemoryLimit: Int,
                  NEventsListener: Int,
                  NGoroutines: Int,
                  OperatingSystem: String,
                  SwapLimit: Int)

  case class Version(ApiVersion: String,
                     Arch: String,
                     GitCommit: String,
                     GoVersion: String,
                     KernelVersion: String,
                     Os: String,
                     Version: String)


  case class Container(Command: String,
                       Created: Int,
                       Id: String,
                       Image: String,
                       Status: String,
                       Names: Seq[String],
                       Ports: Seq[Port]) {
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
                             ExposedPorts: Map[String, NetworkSettingsPort] = Map.empty) {

    // Workaround, Docker may return null
    val cmd = Option(Cmd).getOrElse(Seq.empty)
    val env = Option(Env).getOrElse(Seq.empty)
    val exposedPorts = Option(ExposedPorts).getOrElse(Map.empty)

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
                                    Cmd: Seq[String],
                                    Image: String,
                                    Tty:Boolean,
                                    HostConfig: HostConfig,
                                    ExposedPorts: Map[String, NetworkSettingsPort] = Map.empty[String, NetworkSettingsPort],
                                    name: String) {
    val cmd = Option(Cmd).getOrElse(Seq.empty)
  }

  case class HostConfig(PublishAllPorts: Boolean,
                        PortBindings: Map[String, Seq[NetworkSettingsPort]] = Map.empty)

  case class CreateContainerResponse(Id: String, Warnings: Seq[String])

  // {"status": "create", "id": "dfdf82bd3881","from": "ubuntu:latest", "time":1374067924}
  case class DockerEvent(status: String, id: String, from: String = "", time: Double) {
    def since = Moment(time*1000).fromNow()
    def shortId = subId(id)
  }

}