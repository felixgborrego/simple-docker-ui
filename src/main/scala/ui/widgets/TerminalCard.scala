package ui.widgets

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}
import org.scalajs.dom
import org.scalajs.dom.raw._
import util.logger._
import util.termJs._

object TerminalCard {

  case class State(title: String = "Terminal")

  case class Props(stdinAttached: Boolean, connectionFactory: () => WebSocket)

  case class Backend(t: BackendScope[Props, State]) {

    lazy val ws = t.props.connectionFactory()

    def didMount(): Unit = {
      log.info("Terminal: willStart")
      val terminal = new Terminal(util.termJs.Default)
      val element = dom.document.getElementById("terminal")
      initTerminal(terminal, element)

      ws.onmessage = (x: MessageEvent) => {
        terminal.write(x.data.toString.replace("\n", "\r\n"))
      }

      ws.onopen = (x: Event) => {
        log.info("Connected")
        terminal.write("\u001B[31m[Connected]\u001B[0m\r\n")
      }
      ws.onclose = (x: CloseEvent) => {
        log.info(x.toString)
        terminal.write("\r\n\u001B[31m[Disconnected] " + x.reason + "\u001B[0m")
      }
      ws.onerror = (x: ErrorEvent) => {
        log.info("some error has occurred " + x.message)
        terminal.write("\u001B[31m[Connection error: " + x.message + "]\u001B[0m")
      }


      terminal.on("data", (data: String) => {
        log.info("Terminal input:" + data)
        if (!t.props.stdinAttached) {
          terminal.write(data.toString.replace("\r", "\r\n"))
        }
        ws.send(data)
      })
      terminal.on("title", (data: String) => t.modState(s => s.copy(title = data)))

      dom.window.addEventListener("resize", (e: dom.Event) => {
        autoResize(terminal, element)
      })
      autoResize(terminal, element)
    }


    def willUnmount() = {
      log.info("Terminal willUnmount")
      ws.close(1000, "Disconnect ws")
    }
  }

  def apply(stdinAttached: Boolean)(connectionFactory: () => WebSocket) = {
    val props = Props(stdinAttached, connectionFactory)
    TerminalCardRender.component(props)
  }
}

object TerminalCardRender {

  import TerminalCard._

  val component = ReactComponentB[Props]("TerminalCard")
    .initialState(State())
    .backend(new Backend(_))
    .render((P, S, B) => {
    vdom(S, P)
  }).componentDidMount(_.backend.didMount())
    .componentWillUnmount(_.backend.willUnmount())
    .build

  def vdom(S: State, props: Props) =
    <.div(^.className := "terminal-col-fixed ",
      <.div(^.id := "terminal")
    )


}