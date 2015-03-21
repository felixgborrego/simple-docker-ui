package model

case class Container(Command: String, Created: Int, Id: String, Image: String, Status: String, Names: Seq[String], Ports: Seq[String])
