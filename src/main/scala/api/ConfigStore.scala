package api

import model.Connection

object ConfigStore {

  // TODO get connection from local storage
  def connection = new Connection("http://localhost:4243")
}
