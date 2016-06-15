---
layout: docs
title: "Recipe: java.io.InputStream to Observable"
description: |
  Cookbook recipe for efficiently turning any java.io.InputStream or a java.io.File into an Observable.
---

This is a cookbook recipe for efficiently turning any
[java.io.InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html)
or a 
[java.io.File](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)
into an `Observable`.

For now the Monix core doesn't provide a way to turn a
`java.io.InputStream` into an `Observable` or for reading from files. 

```scala
import java.util
import java.io.{File, FileInputStream, InputStream}
import scala.util.control.NonFatal
import monix.execution.Cancelable
import monix.reactive.Observable

/** Converts any `InputStream` into an `Observable`
  * emitting arrays of bytes.
  */
def fromStream(in: InputStream, chunkSize: Int = 256): Observable[Array[Byte]] = {
  // Building an `Observable` is not hard, but building an efficient
  // `Observable` is a little tricky, hence it's better to rely on the
  // library's builders. For reading an InputStream, it's better if we
  // build a simple Iterator out of it and then convert that Iterator
  // into an Observable:
  val iterator = new Iterator[Array[Byte]] {
    // The temporary buffer used for reading byte chunks
    private[this] val buffer = new Array[Byte](chunkSize)
    // Keeps the size of the last read chunk, should be less than
    // `chunkSize`. Turns to -1 when we reached EOF.
    private[this] var lastCount = 0

    def hasNext: Boolean =
      lastCount match {
        // Zero means we need to read again
        case 0 =>
          lastCount = in.read(buffer)
          // Zero is a valid value, not EOF ;-)
          lastCount >= 0
        case nr =>
          // We've already read a chunk, if positive then we are
          // waiting for next()
          nr > 0
      }

    def next(): Array[Byte] = {
      // in.read() returns -1 on EOF and this call is illegal if
      // lastCount is -1
      if (lastCount < 0)
        throw new NoSuchElementException("next")
      else {
        // We cannot stream buffer directly because it is being
        // mutated. Instead we make a copy.
        val result = util.Arrays.copyOf(buffer, lastCount)
        // Setting this to zero will trigger the next in.read()
        lastCount = 0
        result
      }
    }
  }

  Observable.fromIterator(iterator)
}

/** Converts any `File` into an `Observable` streaming
  * its contents.
  */
def fromFile(file: File, chunkSize: Int = 256): Observable[Array[Byte]] = {
  // We are going to rely on the `fromInputStream` function defined
  // above, however a `File` is like an `Iterable`, which means that
  // each new subscription should open a new file handler, hence we
  // need to return a factory. Using `unsafeCreate` here, because in
  // this case we know what we are doing ;-)
  Observable.unsafeCreate { subscriber =>
    // Guards against contract violations. Basically we cannot stream
    // a final event, like `onComplete` or `onError` twice.  Monix
    // does a reasonable job in doing that, but it's good if we also
    // guard against it ourselves.
    var streamErrors = true
    try {
      // This can trigger exceptions, hence the try/catch
      val in = new FileInputStream(file)
      val obs = fromInputStream(in, chunkSize)
      // From here on, whatever exceptions happen should no longer be
      // streamed to the subscriber, because that could lead to a
      // contract violation.
      streamErrors = false
      obs.unsafeSubscribeFn(subscriber)
    } catch {
      case NonFatal(ex) =>
        if (streamErrors) subscriber.onError(ex)
        else subscriber.scheduler.reportFailure(ex)
        // Error happened, our stream is already done
        // so nothing is left to cancel
        Cancelable.empty
    }
  }
}
```