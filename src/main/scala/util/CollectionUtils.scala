package util

object collections {
  implicit class RichSeq[A](seq: Seq[A]) {
    def replace(lookup: A, newValue: A): Seq[A] =
      seq.map { value =>
        if (value == lookup) newValue else value
      }

    // like takeWhile but including the last one
    def takeTo(p: A => Boolean): Seq[A] = seq.span(p) match {
      case (takeWhile, dropWhile) => takeWhile ++ dropWhile.headOption
    }

  }
}