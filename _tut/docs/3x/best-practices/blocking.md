---
layout: docs
title: "Best Practice: Should Not Block Threads"
description: "Blocking threads is incredibly error prone. And if you must block, do so with Scala's BlockContext and with explicit timeouts."
---

When you have a choice, you should never block. For example, don't do
this:

```scala
def fetchSomething: Future[String] = ???

// later ...
val result = Await.result(fetchSomething, Duration.Inf)
result.toUpperCase
```

Prefer keeping the context of that Future all the way, until the edges of your program:

```scala
def fetchSomething: Future[String] = ???

fetchSomething.map(_.toUpperCase)
```

**PRO-TIP:** for Scala's Future, checkout the
[Scala-Async](https://github.com/scala/async) project to make this
easier.

**REASON:** blocking threads is error prone because you have to know
and control the configuration of the underlying thread-pool. For
example even Scala's `ExecutionContext.Implicits.global` has an upper
limit to the number of threads spawned, which means that you can end
up in a *dead-lock*, because all of your threads can end up blocked,
with no threads available in the pool to finish the required
callbacks.

## If blocking, specify explicit timeouts

If you have to block, specify explicit timeouts for failure and never
use APIs that block on some result and that don't have explicit
timeouts.

For example Scala's own `Await.result` is very well behaved ands
that's good:

```scala
Await.result(future, 3.seconds)
```

But for example when using
[Java's Future](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html),
never do this:

```scala
val future: java.util.concurrent.Future[T] = ???

// BAD CODE, NEVER DO THIS !!!
future.get
```

Instead always specify timeouts, because in case the underlying
thread-pool is limited and there are no more threads left, at least
some of them will get unblocked after the specified timespan:

```scala
val future: java.util.concurrent.Future[T] = ???

// GOOD
future.get(TimeUnit.SECONDS, 3)
```

## If blocking, use Scala's BlockContext

This includes all blocking I/O, including SQL queries. Real sample:

```scala
// BAD SAMPLE!
Future {
  DB.withConnection { implicit connection =>
    val query = SQL("select * from bar")
    query()
  }
}
```

Blocking calls are error-prone because one has to be aware of exactly
what thread-pool gets affected and given the default configuration of
the backend app, this can lead to non-deterministic dead-locks. It's a
bug waiting to happen in production.

Here's a simplified example demonstrating the issue for didactic purposes:

```scala
implicit val ec = ExecutionContext
  .fromExecutor(Executors.newFixedThreadPool(1))

def addOne(x: Int) = Future(x + 1)

def multiply(x: Int, y: Int) = Future {
  val a = addOne(x)
  val b = addOne(y)
  val result = for (r1 <- a; r2 <- b) yield r1 * r2

  // This can dead-lock due to the limited size 
  // of our thread-pool!
  Await.result(result, Duration.Inf)
}
```

This sample is simplified to make the effect deterministic, but all
thread-pools configured with upper bounds will sooner or later be
affected by this.

Blocking calls have to be marked with a `blocking` call that signals
to the `BlockContext` a blocking operation. It's a very neat mechanism
in Scala that lets the `ExecutionContext` know that a blocking operation
happens, such that the `ExecutionContext` can decide what to do about
it, such as adding more threads to the thread-pool (which is what
Scala's ForkJoin thread-pool does).

**WARNING:** Scala's `ExecutionContext.Implicits.global` is backed by
a cool `ForkJoinPool` implementation that has an absolute maximum
number of threads limit. What this means is that, in spite of well
behaved code, you can still hit that limit and you can still end up in
a dead-lock. This is why blocking threads is error prone, as nothing
saves you from knowing and controlling the thread-pools that you end
up blocking.

## If blocking, use a separate thread-pool for blocking I/O

If you're doing a lot of blocking I/O (e.g. a lot of calls to JDBC),
it's better to create a second thread-pool / execution context and
execute all blocking calls on that, leaving the application's
thread-pool to deal with CPU-bound stuff.

So you could initialize another I/O related thread-pool like so:

```scala
import java.util.concurrent.Executors

// ...
private val io = Executors.newCachedThreadPool(
  new ThreadFactory {
    private val counter = new AtomicLong(0L)

    def newThread(r: Runnable) = {
      val th = new Thread(r)
      th.setName("io-thread-" +
      counter.getAndIncrement.toString)
      th.setDaemon(true)
      th
    }
  })
```

Note that here I prefer to use an unbounded "cached thread-pool", so
it doesn't have a limit. When doing blocking I/O the idea is that
you've got to have enough threads that you can block. But if unbounded
is too much, depending on use-case, you can later fine-tune it, the
idea with this sample being that you get the ball rolling.

You could also use Monix's `Scheduler.io` of course, which is also
backed by a "cached thread-pool":

```scala
import monix.execution.Scheduler

private val io = 
  Scheduler.io(name="engine-io")
```

And then you could provide a helper, like:

```scala
def executeBlockingIO[T](cb: => T): Future[T] = {
  val p = Promise[T]()

  io.execute(new Runnable {
    def run() = try {
      p.success(blocking(cb))
    }
    catch {
      case NonFatal(ex) =>
        logger.error(s"Uncaught I/O exception", ex)
        p.failure(ex)
    }
  })

  p.future
}
```


