package util

import org.scalajs.dom.raw.Element

import scala.scalajs.js
import scala.scalajs.js.Object
import scala.scalajs.js.annotation.{JSName, ScalaJSDefined}

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

    // use addons fit.js
    def fit(): Unit = js.native

    // use addons attach.js
    def attach(any: Any): Unit = js.native

    def proposeGeometry(): Geometry = js.native
  }


  def DefaultWithStdin = js.Dynamic.literal(cols = 150, rows = ROWS, screenKeys = true, cursorBlink = true)

  def DefaultWithOutStdin = js.Dynamic.literal(cols = 150, rows = ROWS, screenKeys = false, useStyle = false, cursorBlink = false)

  def initTerminal(terminal: Terminal, element: Element) = {
    terminal.open(element)
    terminal.fit()
  }

  val ROWS = 24

}

@ScalaJSDefined
class Geometry(
  val cols: Int,
  val rows: Int
) extends js.Object

