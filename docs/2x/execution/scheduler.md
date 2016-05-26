---
layout: docs
title: Scheduler
type_api: monix.execution.Scheduler
type_source: monix-execution/shared/src/main/scala/monix/execution/Scheduler.scala
description: |
  A cross-platform execution context, can execute logic asynchronously and with a delay, typically but not necessarily on a thread-pool.
---

A cross-platform execution-context, can execute logic asynchronously
and with a delay, typically but not necessarily on a thread-pool.

The Monix `Scheduler` is inspired by
[ReactiveX](http://reactivex.io/){:target="_blank"}, being an enhanced Scala
[ExecutionContext]({{ site.scalaapi }}#scala.concurrent.ExecutionContext){:target="_blank"} and also a replacement for
Java's
[ScheduledExecutorService](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ScheduledExecutorService.html){:target="_blank"},
but also for Javascript's 
[setTimeout](https://developer.mozilla.org/en-US/docs/Web/API/WindowTimers/setTimeout){:target="_blank"}.

## Rationale

An `ExecutionContext` is too limited, having the following problems:

1. It cannot execute things with a given delay
2. It cannot execute units of work periodically (e.g. once every
   second)
3. The `execute()` method doesn't return a token you could use to
   cancel the pending execution of a task

Developers using Akka do have a
[nicer interface](http://doc.akka.io/docs/akka/current/scala/scheduler.html)
that solve the above problems in the form of
[akka.actor.Scheduler](http://doc.akka.io/api/akka/current/index.html#akka.actor.Scheduler),
so you can do this:

```scala
val task: akka.actor.Cancellable =
  ActorSystem("default").scheduler.scheduleOnce(1.second) {
    println("Executing asynchronously ...")
  }

// canceling it before execution happens
task.cancel()
```

There are problems with the above approach - Akka's Scheduler is an
integral part of Akka's actors system and their usage implies a
dependency on Akka, which is a pretty heavy dependency and there's no
good reason for that, Cancelables are useful outside the context of
Schedulers or Akka and in terms of the API, as you'll see, we can do better.

Another approach is to use a
[ScheduledExecutorService](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ScheduledExecutorService.html)
from Java's standard library and is fairly capable and standard, however the API is not idiomatic Scala,
with the results returned being of type
[j.u.c.ScheduledFuture](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ScheduledFuture.html),
which are pretty heavy and have nothing to do with Scala's Futures and
again, this API can surely use improvement.

And neither Akka's `Scheduler` nor Java's `ScheduledExecutorService`
can run on top of [Scala.js](http://www.scala-js.org/), whereas Monix
provides a common API reusable in both environments. Remember, the
`Scheduler` is not about multi-threading, but about asynchrony.

## Importing &amp; Implicits

The `Scheduler` can be a replacement for Scala's `ExecutionContext`
because:

```scala
abstract class Scheduler extends ExecutionContext
```

And there's also a lazy `global` that you can use as an implicit:

```scala
import monix.execution.Scheduler.Implicits.global
```

We can now execute futures, because this will be our execution
context:

```scala
import concurrent.Future
Future(1 + 1).foreach(println)
// 2
```

On the JVM this lazily initialized `global` instance executes tasks by
means of a Scala "ForkJoinPool" (actually being backed by Scala's own
`ExecutionContext.Implicits.global`), and it can be tuned
by setting the following system properties:

- "*scala.concurrent.context.minThreads*" an integer specifying the minimum
  number of active threads in the pool
- "*scala.concurrent.context.maxThreads*" an integer specifying the maximum
  number of active threads in the pool
- "*scala.concurrent.context.numThreads*" can be either an integer,
  specifying the parallelism directly or a string with the format "xNUM"
  (e.g. "x1.5") specifying the multiplication factor of the number of
  available processors (taken with `Runtime.availableProcessors`)
  
Example of setting a system property:

```sh
java -Dscala.concurrent.context.minThreads=10 ...
```

On top of Javascript with Scala.js, the `global` scheduler is simply
backed by an implementation using `setTimeout` under the hood.

## Execute Runnables

In order to schedule a 
[Runnable](https://docs.oracle.com/javase/7/docs/api/java/lang/Runnable.html){:target="_blank"}
to execute asynchronously:

```scala
import monix.execution.Scheduler.{global => scheduler}

scheduler.execute(new Runnable {
  def run(): Unit = {
    println("Hello, world!")
  }
})
```

Once a task has been scheduled for execution like this, 
there's no way to cancel it.

## Schedule with a Delay

To execute a `Runnable` with a given delay, let's
say for example 5 seconds:

```scala
import java.util.concurrent.TimeUnit

val cancelable = scheduler.scheduleOnce(
  5, TimeUnit.SECONDS,
  new Runnable {
    def run(): Unit = {
      println("Hello, world!")
    }
  })
  
// In case we change our mind, before time's up
cancelable.cancel()
```

Monix also supplies a more Scala-friendly extension:

```scala
import scala.concurrent.duration._

val c = scheduler.scheduleOnce(5.seconds) {
  println("Hello, world!")
}
```

## Schedule Repeatedly

We can schedule tasks to run repeatedly,
let's say with an initial delay of 3 seconds before the
first execution and then with a fixed delay between subsequent
executions of 5 seconds:

```scala
val c = scheduler.scheduleWithFixedDelay(
  3, 5, TimeUnit.SECONDS,
  new Runnable {
    def run(): Unit = {
      println("Fixed delay task")
    }
  })
  
// If we change our mind and want to cancel
c.cancel()
```

Note that it doesn't matter how much the execution takes,
the delay between tasks will be constant. So in this 
sample, we are actually going to have an accumulated
delay of 7 seconds between `println` calls:

```scala
val c = scheduler.scheduleWithFixedDelay(
  3, 5, TimeUnit.SECONDS,
  new Runnable {
    def run(): Unit = {
      Thread.sleep(2000) // 2 seconds
      println("Fixed delay task")
    }
  })
```

There's also a more Scala-friendly extension:

```scala
scheduler.scheduleWithFixedDelay(3.seconds, 5.seconds) {
  println("Fixed delay task")
}
```

So, in order to take execution duration into account,
we can use the second variant, scheduling periodic
execution at a fixed rate. 

```scala
val c = scheduler.scheduleAtFixedRate(
  3, 5, TimeUnit.SECONDS,
  new Runnable {
    def run(): Unit = {
      println("Fixed delay task")
    }
  })
  
// If we change our mind and want to cancel
c.cancel()
```

With `scheduleAtFixedRate` executions will commence after
`initialDelay` then `initialDelay+period`, then `initialDelay + 2 *
period`, and so on. If any execution of this task takes longer than
its period, then subsequent executions may start late, but will not
concurrently execute.

## Injecting Time and Tests

The Monix `Scheduler` can inject the time by means of
`Scheduler.currentTimeMillis`, which is a Unix timestamp and thus
returns the number of milliseconds since the 1970-01-01 00:00:00 UTC.

Doing this is bad because it is a global singleton and we cannot
override its behavior:

```scala
System.currentTimeMillis
// res1: Long = 1464223070198
```

But if given a scheduler, we can now do this:

```scala
scheduler.currentTimeMillis
// res2: Long = 1464223092089
```

All of Monix's time-based operations are relying on this.
Which means that in tests we can *mock time* along with
faking asynchronous execution. Here's how:

```scala
import monix.execution.schedulers.TestScheduler

val testScheduler = TestScheduler()

testScheduler.execute(new Runnable {
  def run() = println("Immediate!")
})

testScheduler.scheduleOnce(1.second) {
  println("Delayed execution!")
}

// Now we can fake it. Executes immediate tasks,
// on the current thread:
testScheduler.tick()
// => Immediate!

// Simulate passage of time, current thread:
testScheduler.tick(1.second)
// => Delayed execution!
```

But non-determinism is still simulated. For example if we do this, the
order of execution for tasks that have the same priority will be
randomly chosen, so you can't say which is going to execute first or
which is second.

```scala
// runnable1 might execute first, or second
testScheduler.execute(runnable1)
// runnable2 might execute first, or second
testScheduler.execute(runnable2)
```

## Execution Model

Along with time, the `Scheduler` also specifies the
[ExecutionModel]({{ site.api2x }}#monix.execution.schedulers.ExecutionModel), 
which is a specification that acts as a guideline for pieces of computations
that are doing possibly asynchronous execution in loops.
For example in Monix, this affects how both `Task` and `Observable` 
are evaluated.

Currently there are 3 execution models available:

- [BatchedExecution]({{ site.api2x }}#monix.execution.schedulers.ExecutionModel$$BatchedExecution),
  the Monix default, specifies a mixed execution mode under which tasks are 
  executed synchronously in batches up to a maximum size, after
  which an asynchronous boundary is forced. This execution mode
  is recommended because we don't want to block threads / run-loops
  indefinitely, especially on top of Javascript where a long loop
  can mean that the UI gets frozen and where we need to be cooperative.
- [AlwaysAsyncExecution]({{ site.api2x }}#monix.execution.schedulers.ExecutionModel$$AlwaysAsyncExecution$)
  specifies that units of work within a loop should always execute
  asynchronously on each step, being basically the mode of operation
  for Scala's `Future`.
- [SynchronousExecution]({{ site.api2x }}#monix.execution.schedulers.ExecutionModel$$SynchronousExecution$)
  specifies that synchronous execution should always be preferred,
  for as long as possible, being basically the mode of operation
  for the Scalaz `Task`.

You can retrieve the configured `ExecutionModel` by calling
`Scheduler.executionModel`.  Here's the default:

```scala
global.executionModel
// res: monix.execution.schedulers.ExecutionModel = 
//   BatchedExecution(1024)
```

You can configure the batch size for this default 
by setting a system property like:

```
java -Dmonix.environment.batchSize=256 ...
```

So if you want to specify a configuration different from the default,
you need to build a new `Scheduler` instance.

## Builders on the JVM

On top of the JVM you can build a `Scheduler` instance manually,
by piggy-backing on an existing Scala `ExecutionContext` that will
actually execute the tasks and on top of an existing 
Java `ScheduledExecutorService` that will be in charge of
scheduling delayed executions, but that won't run the tasks
themselves. There are multiple overloads available, but lets do 
the most general:

```scala
import java.util.concurrent.Executors
import monix.execution.schedulers.ExecutionModel.AlwaysAsyncExecution
import monix.execution.{Scheduler, UncaughtExceptionReporter}

// Will schedule things with delays
val scheduledExecutor =
  Executors.newSingleThreadScheduledExecutor()
// For actual execution of tasks
val executorService = 
  scala.concurrent.ExecutionContext.Implicits.global
// Logs errors to stderr or something
val uncaughtExceptionReporter =
  UncaughtExceptionReporter(executorService.reportFailure)

val scheduler = Scheduler(
  scheduledExecutor,
  executorService,
  uncaughtExceptionReporter,
  AlwaysAsyncExecution)
```

There are multiple overloads available, so you may skip
some of those params:

```scala
val scheduler = Scheduler(scheduledExecutor, executorService)
```

Or even:

```scala
val scheduler = Scheduler(executorService)
```

Even if you specify just an `ExecutorService`, it still knows how to
build a scheduler, because we also have a default
`Executors.newSingleThreadScheduledExecutor` being used as the
`ScheduledExecutorService` used to schedule things to be executed with
a delay. It uses a single thread because it's in charge only of
scheduling, the actual execution being done by the given
`ExecutorService`.

But maybe we want to only wrap just a Java `ScheduledExecutorService`
instance, a service capable of everything we'd want out of our
`Scheduler`. We can do that as well:

```scala
val javaService = Executors.newScheduledThreadPool(10)
val scheduler = Scheduler(javaService)

// Optional ExecutionModel
val scheduler = Scheduler(javaService, AlwaysAsyncExecution)
```

Also on the JVM, we can create a `ForkJoinPool` meant for
CPU-bound tasks like so:

```scala
// Simple constructor
Scheduler.computation(parallelism=10)

// Specify an optional ExecutionModel
Scheduler.computation(
  parallelism = 10
  executionModel = AlwaysAsyncExecution)
```

Or we can create an unbounded thread-pool meant for I/O-bound tasks,
backed by a Java
[CachedThreadPool](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Executors.html#newCachedThreadPool()){:target="_blank"}:

```scala
Scheduler.io()
Scheduler.io(name="my-io")

Scheduler.io(
  name="my-io",
  executionModel = AlwaysAsyncExecution)
```

Or in case we want to be precise or feel like emulating Javascript's
environment, we could create a single threaded thread-pool, backed
by a Java [SingleThreadScheduledExecutor](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Executors.html#newSingleThreadScheduledExecutor()){:target="_blank"}:

```scala
Scheduler.singleThread(name="my-thread")
```

Or a thread-pool with an exact number of threads (and not a 
variable one like the `ForkJoinPool` above), backed by a Java
[ScheduledThreadPool](https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Executors.html#newScheduledThreadPool(int)){:target="_blank"}
for both executing and scheduling delays:

```scala
Scheduler.fixedPool(name="my-fixed", poolSize=10)
```

## Builders for Javascript

On top of Javascript things are simpler, since you can 
rely on `setTimeout`. But you might still want to tweak settings,
so this works:

```scala
Scheduler(executionModel=AlwaysAsyncExecution)
```

We might also want to execute undelayed tasks immediately
by means of an internal trampoline:

```scala
Scheduler.trampoline()

Scheduler.trampoline(executionModel=AlwaysAsyncExecution)
```

Note that the trampoline cannot fake delayed execution,
so it will still use `setTimeout` when delays are involved.