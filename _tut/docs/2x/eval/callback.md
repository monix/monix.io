---
layout: docs
title: Callback
type_api: monix.eval.Callback
type_source: monix-eval/shared/src/main/scala/monix/eval/Callback.scala
description: |
  A listener type that can be called asynchronously with the result of a computation. Used by the Monix Task.
    
tut:
  scala: 2.11.8
  binaryScala: "2.11"
  dependencies:
    - io.monix::monix-eval:version2x
---

`Callback` is a listener type that can be called asynchronously with
the result of a computation.

When building an asynchronous `Task`, on execution the API gives you a
`Callback` instance that you can invoke with the result of a
computation, on completion. Its definition is something like:

```tut:invisible
import scala.util.Try
```

```tut:silent
trait Callback[-T] extends (Try[T] => Unit) {
  def onSuccess(value: T): Unit
  def onError(ex: Throwable): Unit
}
```

This callback type has a contract:

1. on completion, if it was a successful execution, one needs to call `onSuccess`
2. on completion, if the execution ended in error, one needs to call `onError`
3. after calling `onSuccess` or `onError` then no more calls are allowed, as
   that would be a contract violation

In order to protect the contract, you can wrap any such callback into
a "safe" implementation that protects against violations:

```tut:reset:silent
import monix.eval.Callback

val callback = new Callback[Int] {
  def onSuccess(value: Int): Unit = 
    println(value)
  def onError(ex: Throwable): Unit =
    System.err.println(ex)
}

// We need an exception reporter, but we can just a Scheduler
import monix.execution.Scheduler.Implicits.global

val safeCallback1 = Callback.safe(callback)

// But really, we don't need a Scheduler
import monix.execution.UncaughtExceptionReporter
import UncaughtExceptionReporter.{LogExceptionsToStandardErr => r}

val safeCallback2 = Callback.safe(callback)(r)
```

**NOTE:** when executing `Task.runAsync(callback)`, the provided
callback is automatically wrapped in `Callback.safe`, so you don't
need to worry about it.

In case you just want an *empty* callback that doesn't do anything
on `onSuccess`, but that can log errors when `onError` happens,
maybe because you just want the side-effects:

```tut:book
val task = monix.eval.Task(println("Sample"))

task.runAsync(Callback.empty)
```

Or maybe you want to convert a Scala `Promise` to a `Callback`:

```tut:book
val p = scala.concurrent.Promise[String]()

val callback = Callback.fromPromise(p)
```

An interesting effect when dealing with callbacks is that callbacks
calling other callbacks can quickly and easily lead to stack-overflow
errors. So to force a protective asynchronous boundary when calling
`onSuccess` or `onError` (which may or may not fork a thread):

```tut:silent
// Lets pretend we have something meaningful
val ref = Callback.empty[String]

val asyncCallback = Callback.async(ref)
```