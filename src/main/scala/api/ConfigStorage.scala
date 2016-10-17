package api

import model.Connection
import util.PlatformService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ConfigStorage {

  val DefaultMacUrl = "https://192.168.59.103:2376"
  val DefaultLinuxUrl = "http://localhost:2375"
  val DefaultWinUrl = "http://192.168.59.103:2375"
  val ParamUrlConnection = "url"
  val ParamSavedUrlConnection ="saved_urls"

  def saveConnection(url: String) = save(ParamUrlConnection, url)

  def urlConnection: Future[Option[Connection]] = get(ParamUrlConnection)
    .map(url => Some(Connection(url)))
    .recover { case _ => None }

  private def save(key: String, value: String): Future[Unit] =
    PlatformService.current.save(key, value)

  private def get(key: String): Future[String] =
    PlatformService.current.get(key)

  private def remove(key: String): Future[Unit] =
    PlatformService.current.remove(key)

  def defaultUrl: Future[String] = PlatformService.current.defaultUrl

  def isRunningBoot2Docker:Future[Boolean] =  os.map {
    case "mac" | "win" => true
    case _ => false
  }

  private def os: Future[String] = PlatformService.current.osName

  // Return the list of url already used
  val Separator = "!;!"
  def savedUrls(): Future[Seq[String]] = get(ParamSavedUrlConnection)
    .map(_.split(Separator).toSeq)
    .recover {
      case ex:Exception => Seq.empty
    }

  def saveUrls(urls:Seq[String]) = save(ParamSavedUrlConnection,urls.mkString(Separator))

  def saveRunCommand(containerId: String, command: String): Future[Unit] = {
    save(containerId, command)
  }

  def getRunCommand(containerId: String): Future[Option[String]] = get(containerId)
    .map(Some(_))
    .recover { case _ => None }

  def removeRunCommand(containerId: String): Future[Unit] = {
    remove(containerId)
  }
}
