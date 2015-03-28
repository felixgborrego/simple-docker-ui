package api

import model.Connection

object ConfigStore {

  // TODO get connection from local storage
  // linux
   def connection = new Connection("http://localhost:4243")
  //macos
  //def connection = new Connection("https://192.168.59.103:2376")

}
