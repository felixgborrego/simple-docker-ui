package util.chrome

import scala.scalajs.js


package object Api extends js.GlobalScope {

  def chrome: Chrome = js.native

}

trait Chrome extends js.Object {
  def storage: ChromeStorage = js.native
}

trait ChromeStorage extends js.Object {
  def local: Storage = js.native
}

trait Storage extends js.Object {
  def set(map: js.Dynamic, callback: js.Function0[Any]):Unit = js.native

  def get(key: String, callback: js.Function1[js.Dynamic, Any]):Unit = js.native
}


