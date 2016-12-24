package ui.widgets

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model.BasicWebSocket
import org.scalajs.dom
import org.scalajs.dom.{Event, UIEvent}
import util.Geometry
import util.logger._
import util.termJs._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.language.reflectiveCalls


object TerminalCard {

  val GREEN = "\u001B[32m"
  val RED = "\u001B[31m"
  val LIGHT_GRAY = "\u001B[37m"
  val RESET = "\u001B[0m"
  val WS_CLOSE_ABNORMAL = 1006

  def apply(info: TerminalInfo)(connectionFactory: () => Future[BasicWebSocket], resize: (Geometry) => Unit) = {
    val props = Props(info, connectionFactory, resize = resize)
    TerminalCardRender.component(props)
  }

  case class State(title: String = "Terminal",
                   currentWS: Option[BasicWebSocket] = None,
                   currentTerminal: Option[Terminal] = None)

  case class TerminalInfo(stdinOpened: Boolean, stdinAttached: Boolean, stOutAttached: Boolean)

  case class Props(info: TerminalInfo, connectionFactory: () => Future[BasicWebSocket], resize: (Geometry) => Unit)

  case class Backend(t: BackendScope[Props, State]) {

    def didMount(): Unit = {
      val config = if (t.props.info.stdinOpened) util.termJs.DefaultWithStdin else util.termJs.DefaultWithOutStdin
      val terminal = new Terminal(config)

      val element = dom.document.getElementById("terminal-container")

      val wsFut = t.props.connectionFactory()
      initTerminal(terminal, element)

      wsFut.onSuccess { case ws =>
        ws.send("\n")
        connectToWS(terminal, ws)
      }


      terminal.on("title", (data: String) => t.modState(s => s.copy(title = data)))

      dom.window.addEventListener("resize", { event: Event =>
        println("Resize event!! ")
        resize()
      })
    }

    def resize() = {
      t.state.currentTerminal.foreach { currentTerminal =>
        currentTerminal.fit()
        val geometry = currentTerminal.proposeGeometry()
        println(s"Current geometry: ${geometry.cols}, ${geometry.rows}")
        t.props.resize(geometry)
      }
    }

    def connectToWS(terminal: Terminal, ws: BasicWebSocket): Unit = {
      t.modState(_.copy(currentWS = Some(ws), currentTerminal = Some(terminal)))

      terminal.attach(ws)

      ws.addEventListener("close", { e: Any =>
          terminal.write("\r\n\u001B[31m[Disconnected] \u001B[0m")
      })

      ()
    }

    def willUnmount() = {
      log.info("Terminal willUnmount")
      t.state.currentTerminal.foreach(_.destroy())
    }
  }
}

object TerminalCardRender {

  import TerminalCard._

  val component = ReactComponentB[Props]("TerminalCard")
    .initialState(State())
    .backend(new Backend(_))
    .render((P, S, B) => vdom(S, P))
    .componentDidMount(_.backend.didMount())
    .componentWillUnmount(_.backend.willUnmount())
    .build

  def vdom(S: State, props: Props) = {
    <.div(^.id := "terminal-container", ^.className := "panel-body")
  }
}