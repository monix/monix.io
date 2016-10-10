---
layout: docs
title: Callback
type_api: monix.eval.Callback
type_source: monix-eval/shared/src/main/scala/monix/eval/Callback.scala
description: |
  A listener type that can be called asynchronously with the result of a computation. Used by the Monix Task.
---

`Callback` is a listener type that can be called asynchronously with
the result of a computation.

When building an asynchronous `Task`, on execution the API gives you a
`Callback` instance that you can invoke with the result of a
computation, on completion. Its definition is something like:

```scala
abstract class Callback[-T] extends (Try[T]) ⇒ Unit {
  abstract def onSuccess(value: T): Unit
  abstract def onError(ex: Throwable): Unit
}
```

This callback type has a contract:

1. on completion, if it was a successful execution, one needs to call `onSuccess`
2. on completion, if the execution ended in error, one needs to call `onError`
3. after calling `onSuccess` or `onError` then no more calls are allowed, as
   that would be a contract violation

In order to protect the contract, you can wrap any such callback into
a "safe" implementation that protects against violations:

```scala
import monix.eval.Callback

val callback = new Callback[Int] {
  def onSuccess(value: Int): Unit = 
    println(value)
  def onError(ex: Throwable): Unit =
    System.err.println(ex)
}

val safeCallback = Callback.safe(callback)
```

**NOTE:** when executing `Task.runAsync(callback)`, the provided
callback is automatically wrapped in `Callback.safe`, so you don't
need to worry about it.
