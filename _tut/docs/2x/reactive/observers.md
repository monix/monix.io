---
layout: docs
title: Observers and Subscribers
description: |
    The listener types that can be called asynchronously with the events of a reactive stream. Used by the Monix Observable.
---

## Observer

<a href="{{ site.api2x }}#monix.reactive.Observer">API Documentation</a> •
<a href="{{ site.github.repo }}/blob/v{{ site.version2x }}/monix-reactive/shared/src/main/scala/monix/reactive/Observer.scala">Source</a>

The `Observer` from the Rx pattern is the trio of callbacks that get
subscribed to an [Observable](./observable.html) for receiving events.

In essence the `Observer` is this type:

```scala
trait Observer[-T] {
  def onNext(elem: T): Future[Ack]

  def onError(ex: Throwable): Unit

  def onComplete(): Unit
}
```

### Contract

Obviously the `Observer` interface doesn't do much other than
establishing a communication protocol between producers and consumers.
Therefore when pushing items into an `Observer`, we need a contract:

1. Grammar: `onNext` CAN be called zero, one or multiple times,
   followed by an optional `onComplete` or `onError` if the stream is
   finite, so in other words `onNext* (onComplete | onError)?`.
   And once a final event happens, either `onComplete` or `onError`, then
   no further calls are allowed.
2. Back-pressure: each `onNext` call MUST wait on a `Continue` result
   returned by the `Future[Ack]` of the previous `onNext` call.
3. Back-pressure for `onComplete` and `onError` is optional:
   when calling `onComplete` or `onError` you are not
   required to wait on the `Future[Ack]` of the previous `onNext`.
4. Stream cancellation: `onNext` calls can return `Stop` or
   `Future[Stop]` and after receiving it the data-source MUST no
   longer send any events.  Tied with the back-pressure requirement,
   it means that cancellation by means of `Stop` is always
   deterministic and immediate.
5. Ordering/non-concurrent guarantee: calls to `onNext`, `onComplete`
   and `onError` MUST BE ordered and thus non-concurrent.
   As a consequence `Observer` implementations don't normally need to
   `synchronize` their `onNext`, `onComplete` or `onError` methods.
6. Exactly once delivery for final events: you are allowed to call
   either `onComplete` or `onError` at most one time. And you cannot
   call both `onComplete` and `onError`.
7. The implementation of `onNext`, `onError` or `onComplete` MUST NOT throw
   exceptions. Never throw exceptions in their implementation and
   protect against code that might do that.

Corollaries:

1. An observer can subscribe to at most one observable and
   cannot subscribe to multiple observables.
2. When returning `Stop` from `onNext`, no further `onNext` events can
   be received, but this requirement is optional for `onComplete` and
   `onError` because back-pressure is also optional for these final
   events - in other words, after returning `Stop`, the observer can
   receive a final `onComplete` if that `onNext` happened to be the
   last one, or an `onError` if an error just happened.
3. Once a final event happened, like `onComplete`, we have nowhere to
   send eventual errors through `onError` and we cannot send `onError`
   twice, so such errors will at best get logged and at worst get lost.
   Behavior is undefined for errors that break the contract.
4. The data-source can get canceled without the observer receiving
   any notification about it.

### Building an Observer

Let's build an observer that just logs events:

```scala
// Back-pressure related acknowledgement
import monix.execution.Ack
import monix.execution.Ack.{Continue, Stop}
import monix.reactive.Observer
import scala.concurrent.Future

val observer = new Observer[Any] {
  def onNext(elem: Any): Future[Ack] = {
    println(s"O-->$elem")
    // Continue already inherits from Future[Ack],
    // so we can return it directly ;-)
    Continue
  }

  def onError(ex: Throwable): Unit =
    ex.printStackTrace()
  def onComplete(): Unit =
    println("O completed")
}
```

And in case you just want an empty `Observer` that does nothing but
logs `onError` in case it happens:

```scala
// Needed for logging errors
import monix.execution.Scheduler.Implicits.global

val observer = Observer.empty[Int]
```

Or you can quickly build an `Observer` that only logs the events that
it receives. We'll use this in other samples:

```scala
import monix.reactive.Observer

val out = Observer.dump("O")
// out: Observer.Sync[Any]

out.onNext(1)
//=> 0: O-->1
// res0: Ack = Continue

out.onNext(2)
//=> 1: O-->2
// res0: Ack = Continue

out.onComplete()
//=> 2: O completed
```

### Feeding an Observer

Feeding one element, then stopping. This is legal:

```scala
observer.onNext(1)
observer.onComplete()
```

Back-pressuring `onComplete` is optional, so you can also do this:

```scala
import monix.execution.Scheduler.Implicits.global

observer.onNext(1).map {
  case Continue =>
    observer.onComplete()
    Stop
  case Stop =>
    // At this point we know the observer wants
    // to stop, so no onComplete!
    Stop
}
```

Feeding two elements, then stopping. This is NOT legal:

```scala
// BAD SAMPLE
observer.onNext(1)
observer.onNext(2)
observer.onComplete()
```

The correct way of doing this:

```scala
import monix.execution.Scheduler.Implicits.global

observer.onNext(1).map {
  case Continue =>
    // We have permission to continue
    observer.onNext(2)
    // No back-pressure required here
    observer.onComplete()
    Stop
  case Stop =>
    // Nothing else to do
    Stop
}
```

Notice that the contract says that these calls must never be
concurrent, we need imposed ordering. But here we have clear
*happens-before* relationships between calls, so this code is correct.

All together now. Lets feed an entire `Iterator`:

```scala
import monix.execution.Ack.{Continue, Stop}
import monix.execution.{Ack, Scheduler}
import scala.concurrent.Future
import scala.util.control.NonFatal

def feed[A](in: Iterator[A], out: Observer[A])
  (implicit s: Scheduler): Future[Ack] = {

  // Indicates whether errors that happen inside the
  // logic below should be streamed downstream with
  // onError, or whether we aren't allowed because of
  // the grammar. Basically we need to differentiate
  // between errors triggered by our data source, the
  // Iterator, and errors triggered by our Observer,
  // which isn't allowed to triggered exceptions.
  var streamErrors = true
  try {
    // Iterator protocol, we need to ask if it hasNext
    if (!in.hasNext) {
      // From this point on, we aren't allowed to call onError
      // because it can break the contract
      streamErrors = false
      // Signaling the end of the stream, then we are done
      out.onComplete()
      Stop
    } else {
      // Iterator protocol, we get a next element
      val next = in.next()
      // From this point on, we aren't allowed to call onError
      // because it can break the contract
      streamErrors = false
      // Signaling onNext, then back-pressuring
      out.onNext(next).flatMap {
        case Continue =>
          // We got permission, go next
          feed(in, out)(s)
        case Stop =>
          // Nothing else to do, stop the loop
          Stop
      }
    }
  } catch {
    case NonFatal(ex) =>
      // The Iterator triggered the error, so stream it
      if (streamErrors)
        out.onError(ex)
      else // The Observer triggered the error, so log it
        s.reportFailure(ex)
      // Nothing else to do
      Stop
  }
}
```

You'll notice that the implementation tries really hard to not break
the contract. The `streamErrors` pattern is peculiar. We are making a
difference between errors thrown by the `Iterator`, which we should
stream with `onError` and errors thrown by the `Observer`
implementation. By contract the `Observer` is not allowed to throw
errors, ever, therefore if it happens, the behavior is undefined -
though we prefer to log it when we catch such instances.

## Subscriber

<a href="{{ site.api2x }}#monix.reactive.observers.Subscriber">API Documentation</a> •
<a href="{{ site.github.repo }}/blob/v{{ site.version2x }}/monix-reactive/shared/src/main/scala/monix/reactive/observers/Subscriber.scala">Source</a>

Given that, in order to do anything with an `Observer` we always need
a [Scheduler](../execution/scheduler.html), the `Subscriber` is a data
type that's an `Observer` with a `Scheduler` attached:

```scala
trait Subscriber[-T] extends Observer[T] {
  implicit def scheduler: Scheduler
}
```

When subscribing to an `Observable`, the base subscribe method wants a
`Subscriber`, because observables need a `Scheduler` when consumed. So
on `subscribe` you either have to specify it directly, or you specify
a plain `Observer` with a `Scheduler` taken implicitly and then under
the covers a `Subscriber` instance is being built for you.

To convert a plain `Observer` into a `Subscriber`:

```scala
import monix.execution.Scheduler.{global => scheduler}
import monix.reactive.observers._
import monix.reactive._

val observer = Observer.dump("O")
val subscriber = Subscriber(observer, scheduler)
```

To build a `Subscriber` instance that does nothing but logs
`onError` in case it happens:

```scala
import monix.execution.Scheduler.Implicits.global

val subscriber = Subscriber.empty[Int]
```

Or to build a `Subscriber` that logs events to standard output
for debugging purposes:

```scala
val subscriber = Subscriber.dump("O")
```

### Convert to a Reactive Streams Subscriber

Given the integration with
[Reactive Streams](http://www.reactive-streams.org/),
we can convert Monix Subscribers to Reactive Subscribers.

These subscribers have a similar interface and contract, but with a
slightly different API, being in fact equivalent.

```scala
import monix.execution.Ack
import monix.execution.Ack.{Continue, Stop}
import monix.execution.Scheduler.Implicits.global
import monix.reactive.observers.Subscriber
import scala.concurrent.Future

// Building a Monix Subscriber instance
val monixSubscriber = Subscriber.dump("O")

val reactiveSubscriber: org.reactivestreams.Subscriber[Any] =
  monixSubscriber.toReactive
```

And usage of the `org.reactivestreams.Subscriber` interface:

```scala
reactiveSubscriber.onSubscribe(
  new org.reactivestreams.Subscription {
    private var isCanceled = false

    def request(n: Long): Unit = {
      if (n > 0 && !isCanceled) {
        isCanceled = true
        reactiveSubscriber.onNext(1)
        reactiveSubscriber.onComplete()
      }
    }

    def cancel(): Unit =
      isCanceled = true
  })

//=> 0: O-->1
//=> 1: O completed
```

### Convert from a Reactive Streams Subscriber

Given the integration with
[Reactive Streams](http://www.reactive-streams.org/),
we can convert Reactive Subscribers to Monix Subscribers.

Let's implement an `org.reactivestreams.Subscriber`:

```scala
import org.reactivestreams.{Subscription => RSubscription}

val reactiveSubscriber =
  new org.reactivestreams.Subscriber[Int] {
    private var s: RSubscription = null

    def onSubscribe(s: RSubscription): Unit = {
      this.s = s
      s.request(1)
    }

    def onNext(elem: Int): Unit = {
      println(s"O-->$elem")
      s.request(1)
    }

    def onComplete(): Unit =
      println("O completed")
    def onError(ex: Throwable): Unit =
      ex.printStackTrace()
  }
```

And now we can covert it into a `Subscriber` that Monix can use:

```scala
import monix.execution.Scheduler.Implicits.global
import monix.reactive.observers.Subscriber
import monix.execution.Cancelable

val monixSubscriber =
  Subscriber.fromReactiveSubscriber(
    reactiveSubscriber,
    Cancelable(() => println("Was canceled!"))
  )

monixSubscriber.onNext(1)
//=> O-->1
monixSubscriber.onNext(2)
//=> O-->2
monixSubscriber.onComplete()
//=> O completed
```

### Safe Subscriber

<a href="{{ site.api2x }}#monix.reactive.observers.SafeSubscriber">API Documentation</a> •
<a href="{{ site.github.repo }}/blob/v{{ site.version2x }}/monix-reactive/shared/src/main/scala/monix/reactive/observers/SafeSubscriber.scala">Source</a>

The `SafeSubscriber` wraps a `Subscriber` implementation into one that
is safer for usage and protecting some parts of the contract:

1. Exceptions thrown in the underlying subscriber implementation are
   being caught and treated, since exceptions break the contract.
2. If the downstream subscriber returns `Stop` from `onNext`, then
   this will enforce the grammar by stopping further `onNext` and even
   `onComplete` and `onError` events.
3. Once final events, `onComplete` or `onError` are noticed, no
   further events are accepted.
4. The `onComplete` and `onError` are back-pressured. Even though this
   is optional, for users of the API it's safer if they are.

```scala
import monix.execution.Ack
import monix.execution.Scheduler.Implicits.global
import monix.reactive.observers._

def create[A]: SafeSubscriber[A] =
  SafeSubscriber(new Subscriber[A] {
    val scheduler = global

    def onNext(elem: A): Ack =
      throw new IllegalStateException("onNext")
    def onComplete(): Unit =
      println("Completed!")
    def onError(ex: Throwable): Unit =
      System.err.println(s"Error: $ex")
  })

val out = create[Int]
// Error in onNext gets caught and handled
out.onNext(1)
//=> Error: java.lang.IllegalStateException: onNext
// res: Future[Ack] = Stop

// Repeating it will have no further effect
out.onNext(10)
// res: Future[Ack] = Stop

val out2 = create[Int]
out2.onComplete()
//=> Completed!

// No further events are accepted
out2.onNext(1)
// res: Future[Ack] = Stop
```

Note that when subscribing to observables, if you use the regular
`subscribe` methods (and not `unsafeSubscribeFn`) the callbacks you
give are automatically wrapped in a `SafeSubscriber`, so you don't
have to do it by yourself.

### Connectable Subscriber

<a href="{{ site.api2x }}#monix.reactive.observers.ConnectableSubscriber">API Documentation</a> •
<a href="{{ site.github.repo }}/blob/v{{ site.version2x }}/monix-reactive/shared/src/main/scala/monix/reactive/observers/ConnectableSubscriber.scala">Source</a>

Wraps a `Subscriber` implementation into one that back-pressures the
upstream until the call to `connect()` happens. Before `connect()` it
also allows for scheduling the delivery of additional items before any
other `onNext`.

```scala
import monix.execution.Scheduler.Implicits.global
import monix.reactive.observers._

val underlying = Subscriber.dump("O")
val connectable = ConnectableSubscriber(underlying)

// Queue for delivery after connect happens and after
// enqueued items by means of pushNext. At this point
// the subscriber back-pressures the source with a
// Future[Ack] that will complete only after connect()
val ack = connectable.onNext("b1")

// Acknowledgement not given because we are back-pressuring
ack.isCompleted
// res: Boolean = false

// Queueing items to be delivered first on connect()
connectable.pushFirst("a1")
connectable.pushFirst("a2")

// Nothing gets streamed until now:
connectable.connect()
//=> 0: O-->a1
//=> 1: O-->a2
//=> 2: O-->b1

// The data-source is now no longer paused
ack.isCompleted
// res: Boolean = true
```

### Cache Until Connect Subscriber

<a href="{{ site.api2x }}#monix.reactive.observers.CacheUntilConnectSubscriber">API Documentation</a> •
<a href="{{ site.github.repo }}/blob/v{{ site.version2x }}/monix-reactive/shared/src/main/scala/monix/reactive/observers/CacheUntilConnectSubscriber.scala">Source</a>

Wraps an underlying `Subscriber` into an implementation that caches
all events until the call to `connect()` happens. After being
connected, the buffer is drained into the underlying subscriber, after
which all subsequent events are pushed directly.

```scala
import monix.execution.Scheduler.Implicits.global
import monix.execution._
import monix.reactive.observers._

val underlying = Subscriber.dump("O")
val subscriber = CacheUntilConnectSubscriber(underlying)

// Gets cached in an underlying buffer
// to be streamed after connect
subscriber.onNext(10)
// res0: Future[Ack] = Continue
subscriber.onNext(20)
// res1: Future[Ack] = Continue
subscriber.onNext(30)
// res2: Future[Ack] = Continue

// Nothing happens until connect
val result: CancelableFuture[Ack] =
  subscriber.connect()
//=> 0: O-->10
//=> 1: O-->20
//=> 2: O-->30
```

### Buffered Subscriber

<a href="{{ site.api2x }}#monix.reactive.observers.BufferedSubscriber">API Documentation</a> •
<a href="{{ site.github.repo }}/blob/v{{ site.version2x }}/monix-reactive/shared/src/main/scala/monix/reactive/observers/BufferedSubscriber.scala">Source</a>

Observers have a strong contract and consequently:

- are not thread-safe
- have a back-pressure requirement

There are instances in which these requirements are limiting. A
`BufferedSubscriber` describes (and Monix can wrap any implementation
into) a `Subscriber` that:

1. has `onNext`, `onComplete` and `onError` methods that can be called
   concurrently
2. has implementations that always have synchronous behavior, returning
   an immediate `Continue`
3. has an `onNext` that returns an immediate `Continue` for as long as
   the buffer isn't full
4. buffers the connection between the upstream and the underlying
   subscriber such that the underlying subscriber can consume events
   at its own pace

Given that the underlying consumer can be slower than the source and
given that we have a buffer between the data source and the consumer,
we can talk about *overflows* and *overflow strategies*.

The
[OverflowStrategy]([ExecutionModel]({{ site.api2x }}#monix.execution.schedulers.ExecutionModel))
parameter dictates the strategy of the used buffer. We've got these
strategies available:

- `Unbounded` indicates that the buffer should be unlimited. In this
  case the buffer can expand to fill the whole available heap
  memory. And so in case of slow consumers, the process can naturally
  run out of memory.
- `Fail` indicates a limited size for the buffer and on overflow the
  connection will be closed, the underlying subscriber receiving an
  `onError` notification and the data source receiving a `Stop` on
  `onNext`.
- `BackPressure` indicates a limited size for the buffer and on
  overflow it should try to back-pressure the source until the buffer
  is empty again. This isn't a `Synchronous` strategy obviously and so
  it cannot be used in cases where one really needs `Synchronous`
  behavior.
- `DropNew` indicates a limited size for the buffer and on overflow
  it should drop incoming events.
- `DropOld` indicates a limited size for the buffer and on overflow
  it should drop older enqueued events.
- `ClearBuffer` indicates a limited size for the buffer and on overflow
  it should drop the entire buffer and start fresh.
