lazy val root = project.in(file(".")).
  enablePlugins(ScalaJSPlugin)

name := "docker-ui-chrome-app"

scalaVersion := "2.11.6"

persistLauncher in Compile := true

pollInterval := 100

libraryDependencies ++= Seq(
//  "net.lullabyte" %%% "scala-js-chrome" % "0.0.1-SNAPSHOT" withSources() withJavadoc(),
  "org.scala-js" %%% "scalajs-dom" % "0.8.0" withSources() withJavadoc(),

  // react.js
  "com.github.japgolly.scalajs-react" %%% "extra" % "0.8.2"
)

libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.2.6"


fork in run := true
