---
layout: docs
title: MVar
type_api: monix.eval.MVar
type_source: monix-eval/shared/src/main/scala/monix/eval/MVar.scala
description: |
  A mutable location that can be empty or contains a value, asynchronously blocking reads when empty and blocking writes when full.
tut:
  scala: 2.11.8
  binaryScala: "2.11"
  dependencies:
    - io.monix::monix-eval:version2x
---

An `MVar` is a mutable location that can be empty or contains a value,
asynchronously blocking reads when empty and blocking writes when full.

## Introduction

Use-cases:

1. As synchronized, thread-safe mutable variables
2. As channels, with `take` and `put` acting as "receive" and "send"
3. As a binary semaphore, with `take` and `put` acting as "acquire" and "release"

It has two fundamental (atomic) operations:

- `put`: fills the `MVar` if it is empty, or blocks (asynchronously)
  if the `MVar` is full, until the given value is next in line to be
  consumed on `take`
- `take`: tries reading the current value, or blocks (asynchronously)
  until there is a value available, at which point the operation resorts
  to a `take` followed by a `put`

An additional but non-atomic operation is `read`, which tries reading the
current value, or blocks (asynchronously) until there is a value available,
at which point the operation resorts to a `take` followed by a `put`.

<p class="extra" markdown='1'>
In this context "<i>asynchronous blocking</i>" means that we are not blocking
any threads. Instead the implementation uses callbacks to notify clients
when the operation has finished (notifications exposed by means of [Task](./task.html))
and it thus works on top of Javascript as well.
</p>

### Inspiration

This data type is inspired by `Control.Concurrent.MVar` from Haskell, introduced in the paper
[Concurrent Haskell](http://research.microsoft.com/~simonpj/papers/concurrent-haskell.ps.gz),
by Simon Peyton Jones, Andrew Gordon and Sigbjorn Finne, though some details of
their implementation are changed (in particular, a put on a full `MVar` used
to error, but now merely blocks).

Appropriate for building synchronization primitives and  performing simple
interthread communication, it's the equivalent of a `BlockingQueue(capacity = 1)`,
except that there's no actual thread blocking involved and it is powered by `Task`.

## Use-case: Synchronized Mutable Variables

```tut:invisible
import monix.eval._
import monix.execution._
import monix.execution.Scheduler
import monix.execution.schedulers.TestScheduler

implicit val scheduler: Scheduler = TestScheduler()
```

```tut:silent
import monix.execution.CancelableFuture
import monix.eval.{MVar, Task}

def sum(state: MVar[Int], list: List[Int]): Task[Int] =
  list match {
    case Nil => state.take
    case x :: xs =>
      state.take.flatMap { current =>
        state.put(current + x).flatMap(_ => sum(state, xs))
      }
  }

val state = MVar(0)
val task = sum(state, (0 until 100).toList)

// Evaluate
val f: CancelableFuture[Int] = task.runAsync
```

This sample isn't very useful, except to show how `MVar` can be used
as a variable. The `take` and `put` operations are atomic.
The `take` call will (asynchronously) block if there isn't a value
available, whereas the call to `put` blocks if the `MVar` already
has a value in it waiting to be consumed.

Obviously after the call for `take` and before the call for `put` happens
we could have concurrent logic that can update the same variable.
While the two operations are atomic by themselves, a combination of them
isn't atomic (i.e. atomic operations don't compose), therefore if we want
this sample to be *safe*, then we need extra synchronization.

## Use-case: Asynchronous Lock (Binary Semaphore, Mutex)

The `take` operation can act as "acquire" and `put` can act as the "release".
Let's do it:

```tut:silent
final class MLock {
  private[this] val mvar = MVar(())

  def acquire: Task[Unit] =
    mvar.take

  def release: Task[Unit] =
    mvar.put(())

  def greenLight[A](fa: Task[A]): Task[A] =
    for {
      _ <- acquire
      a <- fa.doOnCancel(release)
      _ <- release
    } yield a
}
```

And now we can apply synchronization to the previous example:

```tut:silent
val lock = new MLock
val state = MVar(0)
val task = sum(state, (0 until 100).toList)

val atomicTask = lock.greenLight(task)

// Evaluate
val f: CancelableFuture[Int] = atomicTask.runAsync
```

## Use-case: Producer/Consumer Channel

An obvious use-case is to model a simple producer-consumer channel.

Say that you have a producer that needs to push events.
But we also need some back-pressure, so we need to wait on the
consumer to consume the last event before being able to generate
a new event.

```tut:silent
// Signaling option, because we need to detect completion
type Channel[A] = MVar[Option[A]]

def producer(ch: Channel[Int], list: List[Int]): Task[Unit] =
  list match {
    case Nil =>
      ch.put(None) // we are done!
    case head :: tail =>
      // next please
      ch.put(Some(head)).flatMap(_ => producer(ch, tail))
  }

def consumer(ch: Channel[Int], sum: Long): Task[Long] =
  ch.take.flatMap {
    case Some(x) =>
      // next please
      consumer(ch, sum + x)
    case None =>
      Task.now(sum) // we are done!
  }

val channel = MVar.empty[Option[Int]]
val count = 100000

val producerTask = producer(channel, (0 until count).toList).executeWithFork
val consumerTask = consumer(channel, 0L).executeWithFork

// Ensure they run in parallel, not really necessary, just for kicks
val sumTask = Task.mapBoth(producerTask, consumerTask)((_,sum) => sum)

// Evaluate
val f: CancelableFuture[Long] = sumTask.runAsync
```

Running this will work as expected. Our `producer` pushes values
into our `MVar` and our `consumer` will consume all of those values.
