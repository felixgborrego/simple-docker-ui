import java.nio.charset.Charset
import org.scalajs.core.tools.io.{IO => toolsIO}

scalaVersion in ThisBuild := "2.11.8"

lazy val shared = project.in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    scalacOptions ++= Seq(
      "-Xlint",
      "-deprecation",
      "-Xfatal-warnings"
    ),
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "upickle" % "0.4.1",
      "org.scala-js" %%% "scalajs-dom" % "0.8.1" withSources() withJavadoc(),
      "be.doeraene" %%% "scalajs-jquery" % "0.8.0",
      "com.github.japgolly.scalajs-react" %%% "extra" % "0.8.3",
      "com.lihaoyi" %%% "utest" % "0.3.0" % "test"
    )
  )

lazy val chromeapp = project.in(file("chromeapp")).dependsOn(shared)
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "dockerui",
    persistLauncher := true,
    persistLauncher in Test := false,
    testFrameworks += new TestFramework("utest.runner.Framework"),
    requiresDOM := true,
    mainClass in Compile := Some("ChromeMainApp"),
    artifactPath in (Compile, fullOptJS) := baseDirectory.value / "scalajs-opt.js",
    artifactPath in (Compile, fastOptJS) := baseDirectory.value / "scalajs-fastopt.js",
    artifactPath in (Compile, packageScalaJSLauncher) := baseDirectory.value / "scalajs-launcher.js"
  )

lazy val electron = project.in(file("electron")).dependsOn(shared)
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "dockerui",
    libraryDependencies ++= Seq(
      "com.mscharley" %%% "scalajs-electron" % "0.1.2"  withSources(),
      "com.mscharley" %%% "scalajs-nodejs"   % "0.1.1"  withSources()
    ),
    persistLauncher := true,
    persistLauncher in Test := false,
    testFrameworks += new TestFramework("utest.runner.Framework"),
    requiresDOM := true,
    mainClass in Compile := Some("ElectronMainApp"),
    artifactPath in (Compile, fullOptJS) := baseDirectory.value /  "dockerui-scalajs.js",
    artifactPath in (Compile, fastOptJS) := baseDirectory.value / "dockerui-scalajs.js",
    artifactPath in (Compile, packageScalaJSLauncher) := baseDirectory.value / "scalajs-launcher.js"
  )

pollInterval := 100
fork in run := true
