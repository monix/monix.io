---
layout: docs3x
title: MVar
type_api: monix.catnap.MVar
type_source: monix-catnap/shared/src/main/scala/monix/catnap/MVar.scala
description: |
  A mutable location that can be empty or contains a value, asynchronously blocking reads when empty and blocking writes when full.
---

An `MVar` is a mutable location that can be empty or contains a value,
asynchronously blocking reads when empty and blocking writes when full.

## Introduction

Use-cases:

1. As synchronized, thread-safe mutable variables
2. As channels, with `take` and `put` acting as "receive" and "send"
3. As a binary semaphore, with `take` and `put` acting as "acquire" and "release"

It has these fundamental, atomic operations:

- `put` which fills the var if empty, or blocks (asynchronously) until the var is empty again
- `tryPut` which fills the var if empty; returns `true` if successful
- `take` which empties the var if full, returning the contained value, or blocks (asynchronously) otherwise until there is a value to pull
- `tryTake` empties if full, returns `None` if empty.
- `read` which reads the current value without touching it, assuming there is one, or otherwise it waits until a value is made available via put
- `tryRead` returns `Some(a)` if full, without modifying the var, or else returns `None`
- `isEmpty` returns `true` if currently empty
    
<p class="extra" markdown='1'>
In this context "<i>asynchronous blocking</i>" means that we are not blocking
any threads. Instead the implementation uses callbacks to notify clients
when the operation has finished (notifications exposed by means of [Task](./task.md))
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

## Cats-Effect

`MVar` is generic, being built to abstract over the effect type via the
[Cats-Effect](https://typelevel.org/cats-effect/) type classes, meaning
you can use it with Monix's `Task` just as well as with 
[cats.effect.IO](https://typelevel.org/cats-effect/datatypes/io.html)
or any data types implementing `Async` or `Concurrent`.

Note that `MVar` is already described in
[cats.effect.concurrent.MVar](https://typelevel.org/cats-effect/concurrency/mvar.md)
and Monix's implementation does in fact implement that interface.

<a href="https://typelevel.org/cats-effect/concurrency/mvar.md" target="_blank" 
  title="cats.effect.concurrent.MVar" alt="cats.effect.concurrent.MVar">
  <img src="{{ site.url }}/public/images/concurrency-mvar.png" width="400" />
</a>

`MVar` will remain in Monix as well because:

1. it shares implementation with
   [monix.execution.AsyncVar]({{ page.path | api_base_url }}monix/execution/AsyncVar.html),
   the `Future`-enabled alternative
2. we can use our [Atomic](../execution/atomic.md) implementations
3. at this point Monix's `MVar` has some fixes that have to wait for
   the next version of Cats-Effect to be merged upstream

## Use-case: Synchronized Mutable Variables

```scala mdoc:invisible:nest
import monix.eval._
import monix.execution._
import monix.execution.Scheduler
import monix.execution.schedulers.TestScheduler

implicit val scheduler: Scheduler = TestScheduler()
```

```scala mdoc:silent:nest
import monix.execution.CancelableFuture
import monix.catnap.MVar
import monix.eval.Task

def sum(state: MVar[Task, Int], list: List[Int]): Task[Int] =
  list match {
    case Nil => state.take
    case x :: xs =>
      state.take.flatMap { current =>
        state.put(current + x).flatMap(_ => sum(state, xs))
      }
  }

val task = 
  for {
    state <- MVar[Task].of(0)
    r <- sum(state, (0 until 100).toList)
  } yield r

// Evaluate
task.runToFuture.foreach(println)
//=> 4950
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

```scala mdoc:silent:nest
final class MLock(mvar: MVar[Task, Unit]) {
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

object MLock {
  /** Builder. */
  def apply(): Task[MLock] =
    MVar[Task].of(()).map(v => new MLock(v))
}
```

And now we can apply synchronization to the previous example:

```scala mdoc:silent:nest
val task = 
  for {
    lock <- MLock()
    state <- MVar[Task].of(0)
    task = sum(state, (0 until 100).toList)
    r <- lock.greenLight(task)
  } yield r

// Evaluate
task.runToFuture.foreach(println)
//=> 4950
```

## Use-case: Producer/Consumer Channel

An obvious use-case is to model a simple producer-consumer channel.

Say that you have a producer that needs to push events.
But we also need some back-pressure, so we need to wait on the
consumer to consume the last event before being able to generate
a new event.

```scala mdoc:silent:nest
// Signaling option, because we need to detect completion
type Channel[A] = MVar[Task, Option[A]]

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

val count = 100000

val sumTask =
  for {
    channel <- MVar[Task].empty[Option[Int]]()
    producerTask = producer(channel, (0 until count).toList).executeAsync
    consumerTask = consumer(channel, 0L).executeAsync
    // Ensure they run in parallel, not really necessary, just for kicks
    sum <- Task.parMap2(producerTask, consumerTask)((_,sum) => sum)
  } yield sum

// Evaluate
sumTask.runToFuture.foreach(println)
//=> 4999950000
```

Running this will work as expected. Our `producer` pushes values
into our `MVar` and our `consumer` will consume all of those values.
