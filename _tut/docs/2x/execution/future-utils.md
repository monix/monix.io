---
layout: docs
title: Future Utils
type_api: monix.execution.FutureUtils$
type_source: monix-execution/shared/src/main/scala/monix/execution/FutureUtils.scala
description: Utilities for Scala's standard Future.

tut:
  scala: 2.11.8
  binaryScala: "2.11"
  dependencies:
    - io.monix::monix-execution:version2x
---

Given that we've got [Scheduler](./scheduler.html), it would be a
shame to not use it to aid Scala's standard `Future` and so
`FutureUtils` does just that.

But first the imports:

```tut:silent
// Now we'll need a Scheduler for delaying stuff
import monix.execution.Scheduler.Implicits.global

// We could use the functions defined on the object
import monix.execution.FutureUtils

// Or the extension methods exposed
import monix.execution.FutureUtils.extensions._
```

## Timeout slow Futures

To timeout a `Future` that doesn't complete in due time:

```tut:reset:invisible
import monix.execution.schedulers.TestScheduler
implicit val global = TestScheduler()
import monix.execution.FutureUtils
import monix.execution.FutureUtils.extensions._
```

```tut:silent
import concurrent.{Promise, Future}
import concurrent.duration._

// Creating a never ending Future
val p = Promise[Unit]()
val never = p.future

// Creates a new Future that has a race condition 
// with an error signaling a `TimeoutException`
// if the source doesn't complete in time
never.timeout(3.seconds)

// Or as a simple function call
FutureUtils.timeout(never, 3.seconds)
```

Or to fallback to a backup:

```tut:silent
import scala.concurrent.TimeoutException

// After 3 seconds of inactivity, discards the
// source and fallbacks to the backup
never.timeoutTo(3.seconds, Future.failed(new TimeoutException))

// Or as a simple function call
FutureUtils.timeoutTo(never, 3.seconds, 
  Future.failed(new TimeoutException))
```

## Materialization

In case we want to expose errors, we can now convert `Future[T]` into
a `Future[Try[T]]`, allowing us to act upon the result with `map` and
`flatMap`, as frankly `recover` and `recoverWith` are not enough:

```tut:silent
import scala.util.Try

val f: Future[Int] = Future(1)

// Expose errors
val ft: Future[Try[Int]] = f.materialize

// Or as a simple function call
FutureUtils.materialize(f)
```

We can of course do this operation in reverse and revert a
materialized future, hiding errors:

```tut:silent
val ft: Future[Try[Int]] = Future(1).materialize

// Hide exposed errors
val f: Future[Int] = ft.dematerialize

// Or as a simple function call
FutureUtils.dematerialize(ft)
```

## Delayed Evaluation

Sometimes we want to execute things with a delay and get back the
result as a `Future`:

```tut:silent
// Will execute after 3 seconds
val f = FutureUtils.delayedResult(3.seconds) {
  "Hello, world!"
}
```