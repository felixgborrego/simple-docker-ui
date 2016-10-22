package ui.widgets

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import model.BasicWebSocket
import org.scalajs.dom
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

  def apply(info: TerminalInfo)(connectionFactory: () => Future[BasicWebSocket]) = {
    val props = Props(info, connectionFactory)
    TerminalCardRender.component(props)
  }

  case class State(title: String = "Terminal",
                   currentWS: Option[BasicWebSocket] = None,
                   currentTerminal: Option[Terminal] = None)

  case class TerminalInfo(stdinOpened: Boolean, stdinAttached: Boolean, stOutAttached: Boolean)

  case class Props(info: TerminalInfo, connectionFactory: () => Future[BasicWebSocket])

  case class Backend(t: BackendScope[Props, State]) {

    def didMount(): Unit = {
      val config = if (t.props.info.stdinOpened) util.termJs.DefaultWithStdin else util.termJs.DefaultWithOutStdin
      val terminal = new Terminal(config)
      val element = dom.document.getElementById("terminal")
      initTerminal(terminal, element)
      val wsFut = t.props.connectionFactory()

      wsFut.onSuccess { case ws =>
        connectToWS(terminal, ws)
      }


      terminal.on("title", (data: String) => t.modState(s => s.copy(title = data)))

      dom.window.addEventListener("resize", (e: dom.Event) => {
        autoResize(terminal, element)
      })

      scalajs.js.timers.setTimeout(200) {
        autoResize(terminal, element)
      }
    }

    def connectToWS(terminal: Terminal, ws: BasicWebSocket): Unit = {
      t.modState(_.copy(currentWS = Some(ws), currentTerminal = Some(terminal)))

      ws.onmessage = (event: {def data: js.Any}) => {
        terminal.write(event.data.toString.replace("\n", "\r\n"))
      }

      ws.onopen = (_: Unit) => {
        log.info(s"Connected")
        val info = t.props.info
        if (info.stdinOpened) {
          terminal.write(s"${GREEN}Connected $LIGHT_GRAY[STDIN open: ${info.stdinOpened}, STDIN attached: ${info.stdinAttached}, STOUT attached:${info.stOutAttached}] $RESET\r\n")
        } else {
          terminal.write(s"${RED}Connected $LIGHT_GRAY[STDIN open: ${info.stdinOpened}, STOUT attached:${info.stOutAttached}] ]$RESET\r\n")
          scalajs.js.timers.setTimeout(200) {
            //remove focuse from the terminal
            terminal.blur()
          }
        }
      }
      ws.onclose = (event: {def code: Int}) => {
        log.info(s"onclose code: ${event.code}")
        if (event.code == WS_CLOSE_ABNORMAL) {
          log.info("try to reconnect")
          terminal.destroy()
          didMount() // reconnect
        } else {
          terminal.write("\r\n\u001B[31m[Disconnected] \u001B[0m")
        }
      }
      ws.onerror = (event: {def message: String}) => {
        log.info("some error has occurred " + event.message)
        terminal.write("\u001B[31m[Connection error: " + event.message + "]\u001B[0m")
      }

      terminal.on("data", (data: String) => {
        if (t.props.info.stdinOpened) {
          ws.send(data)
        }
      })

      ()
    }

    def willUnmount() = {
      log.info("Terminal willUnmount")
      t.state.currentWS.foreach{ ws =>
        ws.onclose = { x: {def code: Int} => () }
        ws.onerror = (x: {def message: String}) => {}
        ws.onmessage = (x: {def data: js.Any}) => {}
        ws.close(1000, "Disconnect ws")
      }
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

  def vdom(S: State, props: Props) =
    <.div(^.id := "terminalPanel", ^.className := "terminal-col-fixed ",
      <.div(^.id := "terminal")
    )
}