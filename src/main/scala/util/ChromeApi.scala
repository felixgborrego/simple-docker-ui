package util.chrome

import scala.scalajs.js

@js.native
object api extends js.GlobalScope {

  def chrome: Chrome = js.native

  @js.native
  trait Chrome extends js.Object {
    def storage: ChromeStorage = js.native

    def runtime: Runtime = js.native
  }

  @js.native
  trait ChromeStorage extends js.Object {
    def local: Storage = js.native
  }

  @js.native
  trait Storage extends js.Object {
    def set(map: js.Dynamic, callback: js.Function0[Any]): Unit = js.native

    def get(key: String, callback: js.Function1[js.Dynamic, Any]): Unit = js.native
  }

  @js.native
  trait Runtime extends js.Object {
    def getPlatformInfo(callback: js.Function1[PlatformInfo, Any]): Unit = js.native

    def getManifest(): Manifest = js.native
  }


  @js.native
  trait PlatformInfo extends js.Object {
    def os: String = js.native

    def arch: String = js.native

    def nacl_arch: String = js.native
  }

  @js.native
  trait Manifest extends js.Object {
    def version: String = js.native
  }

}
