---
layout: docs3x
title: Callback
type_api: monix.execution.Callback
type_source: monix-execution/shared/src/main/scala/monix/execution/Callback.scala
description: |
  A listener type that can be called asynchronously with the result of a computation. Used by the Monix Task.
---

`Callback` is a listener type that can be called asynchronously with
the result of a computation.

When building an asynchronous `Task`, on execution the API gives you a
`Callback` instance that you can invoke with the result of a
computation, on completion. Its definition is something like:


```scala mdoc:silent:nest
trait Callback[-E, -A] extends (Either[E, A] => Unit) {
  def onSuccess(value: A): Unit
  def onError(ex: E): Unit
}
```

This callback type has a contract:

1. on completion, if it was a successful execution, one needs to call `onSuccess`
2. on completion, if the execution ended in error, one needs to call `onError`
3. after calling `onSuccess` or `onError` then no more calls are allowed, as
   that would be a contract violation

In order to protect the contract, you can wrap any such callback into
a "safe" implementation that protects against violations:

```scala mdoc:reset:silent
import monix.execution.Callback

val callback = new Callback[Throwable, Int] {
  def onSuccess(value: Int): Unit = 
    println(value)
  def onError(ex: Throwable): Unit =
    System.err.println(ex)
}

// We need an exception reporter, but we can just use a Scheduler
import monix.execution.Scheduler.Implicits.global

val safeCallback1 = Callback.safe(callback)

// But really, we don't need a Scheduler
import monix.execution.UncaughtExceptionReporter
import UncaughtExceptionReporter.{default => r}

val safeCallback2 = Callback.safe(callback)(r)
```

**NOTE:** when executing `Task.runToFuture(callback)`, the provided
callback is automatically wrapped in `Callback.safe`, so you don't
need to worry about it.

In case you just want an *empty* callback that doesn't do anything
on `onSuccess`, but that can log errors when `onError` happens,
maybe because you just want the side-effects:

```scala mdoc:nest
val task = monix.eval.Task(println("Sample"))

task.runAsync(Callback.empty[Throwable, Unit])
```

Or maybe you want to convert a Scala `Promise` to a `Callback`:

```scala mdoc:nest
val p = scala.concurrent.Promise[String]()

val callback = Callback.fromPromise(p)
```

An interesting effect when dealing with callbacks is that callbacks
calling other callbacks can quickly and easily lead to stack-overflow
errors. So to force a protective asynchronous boundary when calling
`onSuccess` or `onError` (which may or may not fork a thread):

```scala mdoc:silent:nest
// Lets pretend we have something meaningful
val ref = Callback.empty[Throwable, String]

val asyncCallback = Callback.forked(ref)
```
