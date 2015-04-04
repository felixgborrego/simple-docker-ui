package util

import org.scalajs.dom
import org.scalajs.dom.raw.{Element, HTMLDivElement}

import scala.scalajs.js
import scala.scalajs.js.Object
import scala.scalajs.js.annotation.JSName

object termJs {

  @JSName("Terminal")
  class Terminal(options: Object) extends Object {

    def open(element: Element): Unit = js.native

    def write(any: Any): Unit = js.native

    def resize(cols: Int, rows: Int): Unit = js.native

    def on(event: String, f: js.Function1[String, _]): Unit = js.native
  }


  def Default = js.Dynamic.literal(cols = 150, rows = 20, screenKeys = true, useStyle = true)

  def initTerminal(terminal: Terminal, element: Element) = {
    terminal.open(element)
  }


  def autoResize(terminal: Terminal, element: Element) = {
    // val div = element.parentNode.parentNode.asInstanceOf[HTMLDivElement]
    val terminalWidth = element.asInstanceOf[HTMLDivElement].offsetWidth
    val width = dom.document.body.clientWidth
    val cols = (width / 6.8).toInt
    val extraCols = if (cols < 129) -3 else if (cols < 148) -2 else if (cols < 180) -1 else 0
    val fixedCols = cols + extraCols
    println("terminal width:" + terminalWidth + " Parent width: " + width + ", cols: " + cols + "  " + fixedCols)
    terminal.resize(fixedCols, 20)
  }

}

