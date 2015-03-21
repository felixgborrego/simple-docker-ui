lazy val root = project.in(file(".")).
  enablePlugins(ScalaJSPlugin)

name := "Scala.js manager for Docker"

scalaVersion := "2.11.5"

persistLauncher in Compile := true

pollInterval := 100

libraryDependencies ++= Seq(
  "net.lullabyte" %%% "scala-js-chrome" % "0.0.1-SNAPSHOT" withSources() withJavadoc(),
  "org.scala-js" %%% "scalajs-dom" % "0.6.1" withSources() withJavadoc(),

  // Minimal usage
  "com.github.japgolly.scalajs-react" %%% "extra" % "0.8.2"
)

libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.2.6"


fork in run := true