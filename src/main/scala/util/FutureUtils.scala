package util

import org.scalajs.dom
import util.logger.log

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

object FutureUtils {

  type LazyFuture[T] = () => Future[T]

  // Execute a list of LazyFutures, continuing and ignoring if there is any error.
  def sequenceWithDelay[T](tasks: List[LazyFuture[T]], ms: Int, ignoreErrors: Boolean)(implicit executionContext: ExecutionContext): Future[List[T]] = {
    val p = Promise[List[T]]()

    def exec(acc: List[T], remaining: List[LazyFuture[T]]): Unit = {
      remaining match {
        case head :: tail =>
          delay(ms) { () =>
            head().map { result =>
              exec(acc :+ result, tail)
            }.recover {
              case NonFatal(ex) if ignoreErrors =>
                log.info(s"Unable to process, Future Skipped - ${ex.getMessage}")
                exec(acc, tail)
              case NonFatal(ex) =>
                log.info(s"Unable to process, Stopping - ${ex.getMessage}")
                p.failure(ex)
            }
          }

        case Nil => p.success(acc)
      }
    }

    exec(List.empty, tasks)

    p.future
  }

  // task like Image GC, need some delay to keep responsive the ui
  val LongDelay = 200
  val SmallDelay = 20


  def delay[T](ms: Int)(task: () => Unit) = dom.setTimeout(task, ms)
}

