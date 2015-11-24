package util

import model._
import upickle.default._
import util.StringUtils._

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

/**
 * Custom parser to process a partial http response for onreadystatechange = Loadding.
 * Note: Using this for performance. The functional impl was too slow and can't afford to parse the http every time.
 */
object PullEventsCustomParser {

  case class EventStream(var activeEvents: ArrayBuffer[EventStatus] = ArrayBuffer.empty,
                         var finishedEvents: ArrayBuffer[EventStatus] = ArrayBuffer.empty,
                         var indexLastInValid: Int = 0, var data: String = "", var done: Boolean = false) {
    def refreshEvents() = {
      PullEventsCustomParser.parse(this, data)
      (activeEvents,finishedEvents)
    }
  }

  case class EventStatus(id: String, var progressValue: Int, var status: String, var progressText: String)

  // Using here a 'custom' streaming json parser Seq[PullProgressEvent]
  def parse(currentStream: EventStream, data: String): Unit = {
    val partialData = data.substring(currentStream.indexLastInValid)
    val newRawEvents = partialData.split( """\{"status":""")
      .drop(1)
      .map(element => """{"status":""" + element)
      .map { text =>
      Try {
        Some(read[PullProgressEvent](text))
      }.getOrElse {
        currentStream.indexLastInValid = currentStream.indexLastInValid + partialData.lastIndexOf(text)
        None
      }
    }.flatten.toSeq

    progressEvents(newRawEvents, currentStream)
  }

  // transform events and grouping duplicates together
  def progressEvents(newEvents: Seq[PullProgressEvent], currentStream: EventStream): Unit = {
    newEvents.foreach { rawEvent =>
      val index = currentStream.activeEvents.indexWhere(_.id == rawEvent.id)
      if (index != -1) {
        // using index as an optimization
        val event = currentStream.activeEvents(index)
        event.progressValue = progressValue(rawEvent)
        event.progressText = progressText(rawEvent)
        event.status = rawEvent.status
        if(event.status == "Download complete" || event.status == "Pull complete") {
          currentStream.activeEvents.remove(index)
          currentStream.finishedEvents.prepend(event)
        }
      } else {
        currentStream.activeEvents.prepend(EventStatus(rawEvent.id, progressValue(rawEvent), rawEvent.status, progressText(rawEvent)))
      }
    }
  }


  def progressValue(event: PullProgressEvent): Int = {
    if (event.progressDetail.total == 0) 0
    else ((event.progressDetail.current.toDouble / event.progressDetail.total.toDouble) * 100).toInt
  }

  def progressText(event: PullProgressEvent) =
    substringAfter(event.progress, "]")
}

object EventsCustomParser {

  case class DockerEventStream(var events: ArrayBuffer[DockerEvent] = ArrayBuffer.empty, var indexLastInValid: Int = 0)

  // Using here a 'custom' DockerEventStream json parser Seq[PullProgressEvent]
  def parse(currentStream: DockerEventStream, data: String): Unit = {
    val partialData = data.substring(currentStream.indexLastInValid)
    val newRawEvents = partialData.split( """\{"status":""")
      .drop(1)
      .map(element => """{"status":""" + element)
      .map { text =>
      Try {
        Some(read[DockerEvent](text))
      }.getOrElse {
        currentStream.indexLastInValid = currentStream.indexLastInValid + partialData.lastIndexOf(text)
        None
      }
    }.flatten.reverse.toSeq
    currentStream.events.insertAll(0, newRawEvents.filterNot(e => currentStream.events.contains(e)))
  }

}