---
layout: docs3x
title: Consumer
type_api: monix.reactive.Consumer
type_source: monix-reactive/shared/src/main/scala/monix/reactive/Consumer.scala
description: |
    A consumer specifies how to consume observables, being a factory of subscribers that can turn observables into tasks.
    
tut:
  scala: 2.12.7
  binaryScala: "2.12"
  dependencies:
    - io.monix::monix-reactive:version3x
---

## Introduction

A `Consumer` specifies how to consume observables.  A `Consumer` is a factory of
[subscribers](./observers.html#subscriber) with a completion callback
attached, being effectively a way to transform observables into tasks
for less error prone consuming of streams.

We already have [observers and subscribers](./observers.html),
callbacks that we can feed into the `subscribe` method of `Observable`
for consuming a stream. The problems that `Consumer` is solving:

- `Observer` (and `Subscriber`) instances are stateful and error
  prone; for example, by contract, with an observer instance you can
  subscribe only to a single data-source
- there's no standard way to describe observers that will produce a
  final result; sure, with observables we could `foldLeftF` and then
  `runAsyncGetFirst` or something along those lines, but those operators
  are usually meant for pure functions and there is no standard way to
  describe consumers that consume some streams and then finally
  produce a result; like for example a consumer that writes into a
  NIO async file channel and then when the stream is finished to
  return some statistics, like the number of bytes written to disk
- and you can't compose these subscribers very well; for example if
  you have a subscriber instance you can't feed it into something that
  can build a load-balancer that processes things in parallel

Teaser:

```tut:invisible
import monix.execution.schedulers.TestScheduler
implicit val global = TestScheduler()
```
```tut:silent
import monix.eval._
import monix.reactive._

// A consumer that folds over the elements of the stream,
// producing a sum as a result
val sumConsumer = Consumer.foldLeft[Long,Long](0L)(_ + _)

// For processing sums in parallel, useless of course, but can become 
// really helpful for logic sprinkled with I/O bound stuff
val loadBalancer = {
  Consumer
    .loadBalance(parallelism=10, sumConsumer)
    .map(_.sum)
}

val observable: Observable[Long] = Observable.range(0, 100000)
// Our consumer turns our observable into a Task processing sums, w00t!
val task: Task[Long] = observable.consumeWith(loadBalancer)

// Consume the whole stream and get the result
task.runToFuture.foreach(println)
//=> 4999950000
```

## Creating Consumers

### Implementing the low-level interface

The low-level way of implementing the `Consumer` is to simply
implement that trait. Lets implement a `Consumer` that calculates the
sum of all the `Int` elements of a stream:

```tut:silent
import monix.execution.Scheduler
import monix.execution.cancelables.AssignableCancelable
import monix.execution.Ack
import monix.execution.Ack.Continue
import monix.execution.Callback
import monix.reactive.Consumer
import monix.reactive.observers.Subscriber

val sumConsumer: Consumer[Int,Long] =
  new Consumer[Int,Long] {
    def createSubscriber(cb: Callback[Throwable, Long], s: Scheduler) = {
      val out = new Subscriber.Sync[Int] {
        implicit val scheduler = s
        private var sum = 0L

        def onNext(elem: Int): Ack = {
          sum += elem
          Continue
        }

        def onComplete(): Unit = {
          // We are done so we can signal the final result
          cb.onSuccess(sum)
        }

        def onError(ex: Throwable): Unit = {
          // Error happened, so we signal the error
          cb.onError(ex)
        }
      }

      // Returning a tuple of our subscriber and a dummy 
      // AssignableCancelable because we don't intend to use it
      (out, AssignableCancelable.dummy)
    }
  }

// USAGE:
{
  import monix.reactive.Observable
  import monix.execution.Scheduler.Implicits.global

  Observable.fromIterable(0 until 10000)
    .consumeWith(sumConsumer)
    .runToFuture
    .foreach(r => println(s"Result: $r"))
    
  //=> Result: 49995000
}
```

So for signaling the final result, we need to call the provided
callback. Some notes:

- calling the callback must obey the contract for the
  [Callback](../execution/callback.html) type, in particular either 
  `onSuccess` or `onError` must be called exactly once
- the given callback should always get called unless the upstream
  gets canceled
- the given callback can be called when the subscriber is finished
  processing, but not necessarily
- if the given callback isn't called after the subscriber is
  done processing, then the `Task` returned by `Consumer#apply`
  loses the ability to cancel the stream, as that `Task` will
  complete before the stream is finished

Canceling the stream in an `Observer` usually happens by returning a
`Stop` result from `onNext`. But there are cases where that is too
limiting. For example maybe we want to timeout with an error in case
the stream takes too long to emit the next event, in which case we
want to terminate with an error and preferably cancel the stream in
advance.

The returned tuple contains an `AssignableCancelable`. This interface
is about cancelables that can be assigned another reference, such as
`SingleAssignmentCancelable`. And we can use this
`AssignableCancelable` to cancel the streaming without waiting for an
`onNext` event. In the example above we aren't using it, so we simply
use a `dummy`. But we could have returned a
`SingleAssignmentCancelable` reference that we could have used in our
subscriber implementation to cancel the stream.

### Consumer.create

For a more refined experience when creating consumers, one can use the
`Consumer.create` builder:

```tut:silent
import monix.reactive._
import monix.execution.Ack
import monix.execution.Ack.Continue

val sumConsumer: Consumer[Int,Long] =
  Consumer.create[Int,Long] { (scheduler, cancelable, callback) =>
    new Observer.Sync[Int] {
      private var sum = 0L

      def onNext(elem: Int): Ack = {
        sum += elem
        Continue
      }

      def onComplete(): Unit = {
        // We are done so we can signal the final result
        callback.onSuccess(sum)
      }

      def onError(ex: Throwable): Unit = {
        // Error happened, so we signal the error
        callback.onError(ex)
      }
    }
  }
```

Using the `create` builder is similar to implementing the `Consumer`
trait directly. Differences:

1. the factory function gets surrounded with `try/catch` and in case
   of failure, the error is raised by means of the callback and the
   stream gets canceled
2. a cancelable instance gets automatically injected; calling cancel
   on it will attempt to cancel the stream, following the rules of
   cancelables returned by observable subscriptions and its usage
   remains optional

### Consumer.fromObserver

A simpler way to create a `Consumer[A, Unit]` out of any `Observer`
instance is `fromObserver`. These consumers only signal when they are
done processing the stream with a `Unit`. Let's build a simple
consumer that does nothing but to log incoming items to `stdout`:

```tut:silent
import monix.reactive.{Consumer, Observer}
import monix.execution.Ack
import monix.execution.Ack.Continue
import java.io.PrintStream

def dumpConsumer[A](
  prefix: String, 
  out: PrintStream = System.out): Consumer[A, Unit] = {

  Consumer.fromObserver[A] { implicit scheduler =>
    new Observer.Sync[A] {
      def onNext(elem: A): Ack = {
        out.println(s"O--->$elem")
        Continue
      }
      
      def onComplete() = 
        out.println(s"O is complete")
      def onError(ex: Throwable) = 
        out.println(s"O terminated with $ex")
    }
  }
}
```

Notice there is no callback or cancelable to worry about, as the
underlying implementation of `Consumer.fromObserver` takes care of it.

## Pre-built Consumers

### Consume a stream until completion

The pre-built `Consumer.complete` will consume a stream until its
completion and then finally trigger a notification when `onComplete`
happens, or signal the error if `onError` happens:

```tut:silent
{
  Observable.range(0, 4).dump("O")
    .consumeWith(Consumer.complete)
    .runToFuture
    .foreach(_ => println("Consumer completed"))

  //=> 0: O-->0
  //=> 1: O-->1
  //=> 2: O-->2
  //=> 3: O-->3
  //=> 4: O completed
  //=> Consumer completed
}
```

### Cancel the stream on subscription

Here's a consumer that immediately cancels its upstream after
subscription, such that consuming a stream with this consumer will
result in a subscription followed by its immediate cancellation:


```tut:silent
Consumer.cancel
```

A similar consumer, one that immediately cancels the upstream, but
that also signals an error:

```tut:silent
Consumer.raiseError(new RuntimeException("Don't know how!"))
```

### Accumulate Items with FoldLeft

Here's a consumer that applies a function to an initial state and
the first item emitted by the source observable, then feeding the
result of this function back into the function along with the second
item emitted by the source observable, continuing this process until
the source emits its final item and completes, whereupon the consumer
will emit the final value returned by the function:

```tut:silent
import monix.execution.Scheduler.Implicits.global
import monix.reactive._

val sum = Consumer.foldLeft[Long,Long](0L)(_ + _)

// Usage
{
  Observable.range(0, 1000)
    .consumeWith(sum)
    .runToFuture
    .foreach(r => println(s"SUM: $r"))
  
  //=> SUM: 499500
}
```

In the example the fold function is returning a simple sum between the
current state (also called the accumulator) and the current item and
we start from zero, thus the built consumer will consume streams of
long integers and signal a total sum on their completion.

A second variant of the `foldLeft` consumer allows for returning a
`Task` as the result of the fold function, thus being useful for doing
asynchronous processing as part of that logic. This sample isn't
useful, but you can easily see that you can insert I/O logic in there:

```tut:silent
import monix.eval.Task
import concurrent.duration._

val sum = Consumer.foldLeftTask[Long,Long](0L) { (acc, elem) => 
  Task(acc + elem).delayExecution(10.millis)
}
```

### Returning the head

Observables have powerful facilities for transforming and processing a
data-source and often all we need is to return the first generated
item and then stop. In such instances we can use `Consumer.head`:

```tut:silent
import monix.reactive._

val sum: Task[Long] = {
  Observable
  .range(0,1000)
  .sumF
  .consumeWith(Consumer.head)
}
  
import monix.execution.Scheduler.Implicits.global
sum.runToFuture.foreach(println)
//=> 499500
```

The `Consumer.head` can be problematic for empty streams, since it
assumes that the source will generate at least one item. Therefore for
empty streams it ends up signaling a `NoSuchElementException`:

```tut:silent
{
  Observable.empty[Int]
    .consumeWith(Consumer.head)
    .failed.runToFuture.foreach(println)
  
  //=> java.util.NoSuchElementException: head
}
```

To play it safe, we can use `Consumer.headOption` instead:

```tut:silent
val first: Task[Option[Int]] =
  Observable.empty[Int].consumeWith(Consumer.headOption)
  
first.runToFuture.foreach(println)
//=> None
```

Or we can simply get the first `Notification` that happened, be it
`OnNext`, `OnComplete` or `OnError` and then stop, by means of
`Consumer.firstNotification`:

```tut:silent
import monix.reactive.Notification.{OnNext, OnComplete, OnError}

val observable = Observable.empty[Int]
val task =
  observable.consumeWith(Consumer.firstNotification)

task.runToFuture.foreach {
  case OnComplete => println("onComplete")
  case OnError(ex) => println(s"onError($ex)")
  case OnNext(elem) => println(s"onNext(elem)")
}
```

### Foreach item execute a callback

The classic foreach operation is available for consumers:

```tut:silent
import monix.reactive._

val source = Observable.range(0,1000)
val logger = Consumer.foreach[Long](x => println(s"Elem: $x"))

val task = source.consumeWith(logger)
// task: Task[Unit] = Async(<function3>)
```

In the context of asynchronous streaming, for each item many times you
want to execute an asynchronous process, like a web request. In such a
case there's `Consumer.foreachAsync` whose callback can return a
`Task[Unit]` instead:

```scala
Consumer.foreachAsync[Long] { item =>
  // Play WS, or whatever ...
  val req = ws.clientUrl(s"https://web.com/request/$item").post()
  Task.fromFuture(req).map(_ => ())
}
```

Or if you want to impress your friends, you can execute that foreach
in parallel:

```tut:silent
Consumer.foreachParallel[Long](parallelism=10) { item => ??? }
```

Well OK, this seems more glamour than substance, as for this to be worth
it, that function needs to do something really expensive, as otherwise
it will be much less efficient than the simple version. And the author
hopes you're not doing blocking I/O.

For doing I/O on the other hand, this can be very useful. For example,
with the parallelism specified as `10`, we could ensure parallel
execution of 10 http requests at the same time and no more:

```scala
Consumer.foreachParallelTask[Long](parallelism=10) { item =>
  // Play WS, or whatever ...
  val req = ws.clientUrl(s"https://web.com/request/$item").post()
  Task.fromFuture(req).map(_ => ())
}
```

### Load-balancing consumers and parallel processing

As a generalization of `foreachParallel` and `foreachParallelTask`,
we can place a load-balancer in front of any consumer. Here's a
consumer that processes sums in parallel:

```tut:silent
val sumConsumer = 
  Consumer.foldLeft[Long,Long](0L)(_+_)
val parallelConsumer = 
  Consumer.loadBalance(parallelism=10, sumConsumer).map(_.sum)

{
  Observable.range(0,10000)
    .consumeWith(parallelConsumer)
    .runToFuture
    .foreach(r => println(s"Result: $r"))

  //=> Result: 49995000
}
```

This can be used:

1. to process things in parallel, but with a parallelism limit
2. it can also be used for I/O, like for doing at most 10 requests in parallel
   and back-pressure the source if the consumers are too slow

You can also combine different consumer instances that can have
different behavior:

```tut:silent
val sumConsumer1 = 
  Consumer.foldLeft[Long,Long](0L)(_+_+1)
val sumConsumer2 = 
  Consumer.foldLeft[Long,Long](0L)(_+_+2)

val parallelConsumer = 
  Consumer.loadBalance(sumConsumer1, sumConsumer2).map(_.sum)
```

The final generated result is going to be a sequence of results collected
from all subscribers, hence the final `.map(_.sum)` to aggregate them all. 
