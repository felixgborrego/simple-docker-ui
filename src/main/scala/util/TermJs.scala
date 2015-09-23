package util

import org.scalajs.dom
import org.scalajs.dom.raw.Element

import scala.scalajs.js
import scala.scalajs.js.Object
import scala.scalajs.js.annotation.JSName

object termJs {

  @JSName("Terminal")
  @js.native
  class Terminal(options: Object) extends Object {

    def open(element: Element): Unit = js.native

    def write(any: Any): Unit = js.native

    def resize(cols: Int, rows: Int): Unit = js.native

    def on(event: String, f: js.Function1[String, _]): Unit = js.native

    def blur(): Unit = js.native

    //def reset(): Unit = js.native

    def destroy(): Unit = js.native
  }


  def DefaultWithStdin = js.Dynamic.literal(cols = 150, rows = ROWS, screenKeys = true, useStyle = true)

  def DefaultWithOutStdin = js.Dynamic.literal(cols = 150, rows = ROWS, screenKeys = false, useStyle = false, cursorBlink = false)

  def initTerminal(terminal: Terminal, element: Element) = {
    terminal.open(element)
  }

  val ROWS = 24
  def autoResize(terminal: Terminal, element: Element) = {
    val width = dom.document.body.clientWidth

    // get approximated size. TODO Find a better way to map between windows width and num columns
    val cols = (width / 6.8).toInt
    val extraCols = if (cols < 129) -3 else if (cols < 148) -2 else if (cols < 180) -1 else 0
    val fixedCols = cols + extraCols
    terminal.resize(fixedCols, ROWS)
  }

}

