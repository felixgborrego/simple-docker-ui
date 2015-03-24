package model

import util.momentJs.Moment

case class Container(Command: String, Created: Int, Id: String, Image: String, Status: String, Names: Seq[String], Ports: Seq[Port]) {
  def id = Id.take(12)

  def created = {
    val timeStamp = Created.toLong * 1000L
    Moment(timeStamp).fromNow()
  }

  def ports = Ports.map { p =>
    val external = if (p.PublicPort == 0) "" else p.IP + ":" + p.PublicPort + "->"
    val internal = if (p.PrivatePort == 0) "" else p.PrivatePort + "/" + p.Type
    external + internal
  }.reverse
}

case class ContainersInfo(running: Seq[Container] = Seq.empty, all: Seq[Container] = Seq.empty) {
  def history = all.diff(running)
}

case class ContainerInfo(Args: Seq[String], Id: String, Image: String, Name: String, Path: String, Created: String,
                         Config: ContainerConfig) {
  def id = Id.take(12)

  def image = Image.take(12)

  def created = {
    Moment(Created).fromNow()
  }

}

case class ContainerConfig(AttachStderr: Boolean, AttachStdin: Boolean, AttachStdout: Boolean,
                           Cmd: Seq[String] = Seq.empty, Entrypoint: String = "", Env: Seq[String] = Seq.empty,
                           Hostname: String, OpenStdin: Boolean, StdinOnce: Boolean, Tty: Boolean, User: String, WorkingDir: String)

case class Port(Type: String, IP: String = "", PrivatePort: Int = 0, PublicPort: Int = 0)

case class ContainerTop(Titles:Seq[String], Processes:Seq[Seq[String]])