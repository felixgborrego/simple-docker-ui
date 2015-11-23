package util

import scala.concurrent.{Future, ExecutionContext}
import scala.util.control.NonFatal
import util.logger.log
object FutureUtils {

  type LazyFuture[T] = () => Future[T]
  // Execute a list of LazyFutures, continuing and ignoring if there is any error.
  def sequenceIgnoringErrors[T](futures: List[LazyFuture[T]])(implicit executionContext: ExecutionContext): Future[List[T]] = futures match {
    case Nil => Future.successful(List.empty)
    case head::tail => head().flatMap { result =>
      sequenceIgnoringErrors(tail).map(tailResult => result :: tailResult)
    }.recoverWith {
      case NonFatal(ex) =>
        log.info(s"Unable to process, Future Skipped - ${ex.getMessage}")
        sequenceIgnoringErrors(tail)
    }
  }

}
