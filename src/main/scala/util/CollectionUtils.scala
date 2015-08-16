package util

object collections {
  implicit class RichSeq[A](seq: Seq[A]) {
    def replace(lookup: A, newValue: A): Seq[A] =
      seq.map { value =>
        if (value == lookup) newValue else value
      }
  }
}