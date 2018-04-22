---
layout: docs3x
title: Observable
type_api: monix.reactive.Observable
type_source: monix-reactive/shared/src/main/scala/monix/reactive/Observable.scala
description: |
  A data type for modeling and processing asynchronous and reactive streaming of events with non-blocking back-pressure.
---

## Introduction

The `Observable` is a data type for modeling and processing
asynchronous and reactive streaming of events with non-blocking
back-pressure. 

The `Observable` is strongly inspired by
[ReactiveX](http://reactivex.io/), but with an idiomatic Scala API and
influenced by the Scala ecosystem of projects such as
[Cats](http://typelevel.org/cats/) and
[Scalaz](http://scalaz.org/). It's also compatible with the
[Reactive Streams](http://www.reactive-streams.org/) specification,
hence it has good interoperability.

```scala
// We need a Scheduler in scope in order to make 
// the Observable produce elements when subscribed
import monix.execution.Scheduler.Implicits.global
import monix.reactive._

import concurrent.duration._

// We first build an observable that emits a tick per second, 
// the series of elements being an auto-incremented long
val source = Observable.interval(1.second)
  // Filtering out odd numbers, making it emit every 2 seconds
  .filter(_ % 2 == 0)
  // We then make it emit the same element twice
  .flatMap(x => Observable(x, x))
  // This stream would be infinite, so we limit it to 10 items
  .take(10)
  
// Observables are lazy, nothing happens until you subscribe...
val cancelable = source
  // On consuming it, we want to dump the contents to stdout
  // for debugging purposes
  .dump("O")
  // Finally, start consuming it
  .subscribe()

```

At its simplest, `Observable` is a replacement for your regular
[Iterable]({{ site.scalaapi }}#scala.collection.Iterable)
or Scala 
[Stream]({{ site.scalaapi }}#scala.collection.immutable.Stream), 
but with the ability to process asynchronous events without blocking. 
And in fact you can convert any `Iterable` into an `Observable`.

But `Observable` scales to complex problems, touching on
*[functional reactive programming (FRP)](https://en.wikipedia.org/wiki/Functional_reactive_programming)*,
or it can model complex interactions between producers and consumers,
being a potent alternative for
[the actor model](http://akka.io/).

### Design Summary

A visual representation of where it sits in the design space:

|                    |        Single       |           Multiple         |
|:------------------:|:-------------------:|:--------------------------:|
| **Synchronous**    |          A          |         Iterable[A]        |
| **Asynchronous**   | Future[A] / Task[A] |        Observable[A]       |

The Monix `Observable`:

- models lazy & asynchronous streaming of events
- it is highly composable and lawful
- it's basically the
  [Observer pattern](https://en.wikipedia.org/wiki/Observer_pattern)
  on steroids
- you can also think of it as being like a Scala
  [Future]({{ scalaapi }}#scala.concurrent.Future) or like
  a [Task](../eval/task.html), except with the ability to stream 
  multiple items instead of just one, or you can think of it as an asynchronous 
  and non-blocking
  [Iterable]({{ site.scalaapi }}#scala.collection.Iterable)
  with benefits
- models producer-consumer relationships, where you can have a single
  producer pushing data into one or multiple consumers
- works best for unidirectional communications
- allows fine-grained control over the [execution model](../execution/scheduler.html#execution-model)
- doesn’t trigger the execution, or any effects until `subscribe`
- allows for cancelling of active streams
- never blocks any threads in its implementation 
- does not expose any API calls that can block threads
- compatible with [Scala.js](http://www.scala-js.org/) like the rest of Monix

See **[comparisons with similar tools, like Akka or FS2](./observable-comparisons.html)**.

## Observable Contract

`Observable` can be thought about as the next layer of abstraction in regard to `Observer` and `Subscriber`.

You could describe `Observable` in the following way:

```scala
trait Observable[+A] {
  def subscribe(o: Observer[A]): Cancelable
}
```

where `Observer` is:

```scala
trait Observer[-T] {
  def onNext(elem: T): Future[Ack]

  def onError(ex: Throwable): Unit

  def onComplete(): Unit
}
```

Due to this connection `Observable` respects `Observer` [contract](./observers.md#Contract).

## Execution

When you create `Observable` nothing actually happens until you call `subscribe`. 
It means that (by default) `Observable` preserves referential transparency. 

`Subscribe` is considered low-level operator and it is advised not to use it unless you know exactly what you are doing. 
You can think about it as `unsafePerformIO`.

Preferred way to deal with `Observable` is to convert it into [Task](./../eval/task.md) and compose it all the way through your program
until the very end (Main method). 

Two main ways to convert `Observable` into `Task` are described below.

### Consumer

One of the ways to trigger `Observable` is to use [Consumer](./consumer.md) which can be described as a function that converts `Observable` into `Task`.

You can either create your own `Consumer` or use one of many prebuilt ones:

```scala
val list: Observable[Long] = 
    Observable.range(0, 1000)
      .take(100)
      .map(_ * 2)
      
val consumer: Consumer[Long, Long] =
    Consumer.foldLeft(0L)(_ + _)
    
val task: Task[Long] =
    list.consumeWith(consumer)
```

You can find more examples in [Consumer documentation](./consumer.md).

### FoldLeft Methods

Any method suffixed with `L` in `Observable` API converts it into `Task`.

For example you can use `firstL` to obtain the first element of the `Observable`:
```scala
// Task(0)
val task: Task[Int] = Observable.range(0, 1000).firstL
```

Equivalent to the `Task` described in previous section would be:

```scala
val list: Observable[Long] = 
    Observable.range(0, 1000)
      .take(100)
      .map(_ * 2)
       
val task: Task[Long] =
    list.foldLeftL(0L)(_ + _)
```
## Building Observable

There are many builders available in the Monix `Observable` companion object. 
This section will cover only some of them so please refer to the latest API docs for the rest of them.

### Observable.pure (now)

`Observable.pure` (alias for `now`) simply lifts an already known 
value in the `Observable` context.

```tut:silent
val obs = Observable.now { println("Effect"); "Hello!" }
//=> Effect
// obs: monix.reactive.Observable[String] = NowObservable@327a283b
```
 
### Observable.delay (eval)

`Observable.delay` (alias for `eval`) lifts non-strict
value in the `Observable`. It is evaluated upon subscription.

```tut:silent
val obs = Observable.delay { println("Effect"); "Hello!" }
// obs: monix.reactive.Observable[String] = EvalAlwaysObservable@48a8050
val task = obs.foreachL(println)
// task: monix.eval.Task[Unit] = Task.Async$1782722529

task.runAsync
//=> Effect
//=> Hello!

// The evaluation (and thus all contained side effects)
// gets triggered on each runAsync:
task.runAsync
//=> Effect
//=> Hello!
```

### Observable.evalOnce

`Observable.evalOnce` takes a non-strict value and converts it into an Observable
that emits a single element and that memoizes the value for subsequent invocations.
It also has guaranteed idempotency and thread-safety:

```tut:silent
val obs = Observable.evalOnce { println("Effect"); "Hello!" }
// obs: monix.reactive.Observable[String] = EvalOnceObservable@3233e694
val task = obs.foreachL(println)
// task: monix.eval.Task[Unit] = Task.Async$1782722529

task.runAsync
//=> Effect
//=> Hello!

// Result was memoized on the first run!
task.runAsync.foreach(println)
//=> Hello!
```

### Observable.fromIterable

`Observable.fromIterable` converts any `Iterable` into `Observable`:

```tut:silent
val obs = Observable.fromIterable(List(1, 2, 3))
// obs: monix.reactive.Observable[Int] = IterableAsObservable@7b0e123d

obs.foreachL(println).runAsync
//=> 1
//=> 2
//=> 3
```

### Observable.suspend (defer)

`Observable.suspend` (alias for defer) allows suspending side effects:

```tut:silent
def readFile(path: String): Observable[String] = 
    Observable.suspend {
        // The side effect won't happen until subscription
        val lines = Source.fromFile(path).getLines
        Observable.fromIterator(lines)
    }
```

### Observable.raiseError

`Observable.raiseError` constructs an Observable that calls `onError` on any subscriber emitting specified `Exception`:

```tut:silent
val observable = Observable.raiseError[Int](new Exception("my exception"))
// observable: monix.reactive.Observable[Int]

observable
  .onErrorHandle {ex => println(s"Got exception: ${ex.getMessage}"); 1}
  .foreachL(println)
//=> Got exception: my exception
//=> 1
```

## Error Handling

`Observable` provides `MonadError[Observable, Throwable]` instance so you can use either any `MonadError` operator or additional methods provided in `Observable` class. 

### handleError (onErrorHandle)

`Observable.handleError` (alias for `onErrorHandle`) mirrors original source unless error happens - in which case it fallbacks to an `Observable` emitting one specified element generated by given total function.

```tut:silent
val observable = Observable(1, 2, 3) ++ Observable.raiseError(new Exception)
// observable: monix.reactive.Observable[Int]

observable
  .handleError(_ => 4)
  .foreachL(println)
//=> 1
//=> 2
//=> 3
//=> 4
```

### handleErrorWith (onErrorHandleWith)

`Observable.handleErrorWith` (alias for `onErrorHandleWith`) mirrors original source unless error happens - in which case it fallbacks to an `Observable` generated by given total function.

```tut:silent
val observable = Observable(1, 2) ++ Observable.raiseError(new Exception)
// observable: monix.reactive.Observable[Int]

observable
  .handleErrorWith(_ => Observable(3, 4))
  .foreachL(println)
//=> 1
//=> 2
//=> 3
//=> 4
```

### recover (onErrorRecover)

`Observable.recover` (alias for `onErrorRecover`) mirrors original source unless error happens - in which case it fallbacks to an `Observable` emitting one specified element generated by given partial function. The difference between `recover` and `handleError` is that the latter takes total function as a parameter.

### recoverWith (onErrorRecoverWith)

`Observable.recoverWith` (alias for `onErrorRecoverWith`) mirrors original source unless error happens - in which case it fallbacks to an `Observable` generated by given partial function. The difference between `recoverWith` and `handleErrorWith` is that the latter takes total function as a parameter.

### onErrorFallbackTo

`Observable.onErrorFallbackTo` mirrors the behavior of the source, unless it is terminated with an `onError`, in which case the streaming of events continues with the specified backup sequence regardless of error.

```tut:silent
val observable = Observable(1, 2) ++ Observable.raiseError(new Exception)
// observable: monix.reactive.Observable[Int]

observable
  .onErrorFallbackTo(Observable(3, 4))
  .foreachL(println)
//=> 1
//=> 2
//=> 3
//=> 4
```

This is equivalent to:
```tut:silent
observable
  .handleErrorWith(_ => Observable(3, 4))
```

### onErrorRestart

`Observable.onErrorRestart` mirrors the behavior of the source unless it is terminated with an `onError`, in which case it tries subscribing to the source again in the hope that it will complete without an error.

The number of retries is limited by the specifiec `maxRetries` parameter.

There is also `onErrorRestartUnlimited` variant for unlimited number of retries.

### onErrorRestartIf

`Observable.onErrorRestartIf` mirrors the behavior of the source unless it is terminated with an `onError`, in which case it invokes provided function and tries subscribing to the source again in the hope that it will complete without an error.

```tut:silent
case object TimeoutException extends Exception

val observable = Observable(1, 2) ++ Observable.raiseError(TimeoutException)
// observable: monix.reactive.Observable[Int]

observable
  .onErrorRestartIf {
    case TimeoutException => true
    case _ => false
  }
  .foreachL(println)
//=> 1
//=> 2
//=> 3
//=> 1
//=> 2
//=> 3
// ... fails and restarts infinitely
```

## Callback Operators

There are many many options for executing given callback when the stream acquires specific type of event.

### doOnNext

Executes given callback for each element generated by the source `Observable`, useful for doing side-effects.

```tut:silent
var counter = 0
val observable = Observable(1, 2, 3)
// obs: monix.reactive.Observable[Int]

observable
  .doOnNext(e => counter += e)
  .foreachL(e => println(s"elem: $e, counter: $counter"))
//=> elem: 1, counter: 1
//=> elem: 2, counter: 3
//=> elem: 3, counter: 6
```

There are also `doOnNextEval` and `doOnNextTask` async variants for usage with `Task` or any datatype with `cats.effect.Effect` instance.

```tut:silent
var counter = 0
val observable = Observable(1, 2, 3)
// obs: monix.reactive.Observable[Int]

observable
  .doOnNextEval(e => IO(counter += e))
  .foreachL(e => println(s"elem: $e, counter: $counter"))
//=> elem: 1, counter: 1
//=> elem: 2, counter: 3
//=> elem: 3, counter: 6
```

... TO BE CONTINUED ...

## Hot and Cold Observables

As mentioned before - by default `Observable` doesn't emit any items until something subscribes to it. This is called *cold Observable.*
In cold Observable there is only one subscriber who is guaranteed to see all the emitted items.

On the other hand there is also a notion of *hot Observable* denoted as `ConnectableObservable` whose source is shared between many subscribers. 
It means that it matters when you subscribe to it and the order of subscription can affect your program.
For this reason tread carefully when using `ConnectableObservable` because it breaks referential transparency.

### Turning cold Observable into hot Observable

There are several methods which turn `Observable` into `ConnectableObservable` using corresponding `Subject`.

`Subject` is a sort of bridge or proxy that acts both as an `Observer` and as an `Observable`.
We can use `Subject` to feed elements into our `Observable`:

```tut:silent
// creating BehaviorSubject which will emit the most recently emitted item by the source,
// or the initialValue (as the seed) in case no value has yet been emitted, then continuing
// to emit events subsequent to the time of invocation.

val subject = BehaviorSubject[Int](0)

subject.onNext(1)
subject.onNext(2)

// returns Task(2)
subject.firstL 
```

### Observable.publish (PublishSubject)
`Observable.publish` will convert this `Observable` into a multicast `Observable` using
`PublishSubject` which emits to a subscriber only those items that are emitted by the source subsequent to the time of the subscription.

```tut:silent
var result = 0
val observable = Observable(1, 2, 3).publish

observable.foreach(e => result += e)

// Start the streaming
observable.connect()

// result == 6

// happens after all items are emitted so it doesn't add anything
observable.foreach(e => result += e)

// result == 6 
```

### Observable.publishLast (AsyncSubject)
`Observable.publishLast` uses `AsyncSubject` as underlying `Subject` which emits only the last value emitted by the source
and only after the source completes.

```tut:silent
var result = 0
val observable = Observable(1, 2, 3).publishLast

observable.foreach(e => result += e)

// Start the streaming
observable.connect()

// result == 3
```

### Observable.replay (ReplaySubject)
`Observable.replay` is powered by `ReplaySubject` which emits to any observer all of the items that were emitted by the source, 
regardless of when the observer subscribes. 

```tut:silent
var result = 0
val observable = Observable(1, 2, 3).replay

// Start the streaming
observable.connect()

observable.foreach(e => result += e)

// result == 6
```

It optionally takes `bufferSize` parameter which limits the number
of items that can be replayed (on overflow the head starts being dropped):

```tut:silent
var result = 0
val observable = Observable(1, 2, 3).replay(1)

// Start the streaming
observable.connect()

observable.foreach(e => result += e)

// result == 3
```

### Observable.behavior (BehaviorSubject)
`Observable.behavior` relies on `BehaviorSubject` which is emitting the most recently emitted item by the source, or
the `initialValue` (as the seed) in case no value has yet been emitted.

```tut:silent
var result = 0
val observable = Observable(1, 2, 3).behavior(0)

// Start the streaming
observable.connect()

observable.foreach(e => result += e)

// result == 3
```

## Interoperability with other Streams API (Akka Streams, FS2)

Due to compability with the [Reactive Streams](http://www.reactive-streams.org/)
specification `Observable` allows good interoperability with other libraries.

The next subsections contain examples how to convert between Monix `Observable` and
two other popular streaming libraries but it should work in similar way with every other library
compatible with Reactive Streams protocol.

### Akka Streams

Necessary imports and initialization for `Akka Streams`:

```tut:silent
import monix.reactive.Observable
import monix.execution.Scheduler.Implicits.global
import akka._
import akka.stream._
import akka.stream.scaladsl._
import akka.actor.ActorSystem

implicit val system = ActorSystem("akka-streams")
implicit val materializer = ActorMaterializer()
```

To convert Akka `Source` to Monix `Observable`:

```tut:silent
val source = Source(1 to 3)
// source: akka.stream.scaladsl.Source[Int,akka.NotUsed] = Source(SourceShape(StatefulMapConcat.out(1887925338)))

val publisher = source.runWith(Sink.asPublisher[Int](fanout = false))
// publisher: org.reactivestreams.Publisher[Int] = VirtualPublisher(state = Publisher[StatefulMapConcat.out(1887925338)])

val observable = Observable.fromReactivePublisher(publisher)
// observable: monix.reactive.Observable[Int] = monix.reactive.internal.builders.ReactiveObservable@72f8ecd
```

To go back from Monix `Observable` to Akka `Source`:

```tut:silent
val observable = Observable(1, 2, 3)
// observable: monix.reactive.Observable[Int] = monix.reactive.internal.builders.IterableAsObservable@4783cc8

val source = Source.fromPublisher(observable.toReactivePublisher)
// source: akka.stream.scaladsl.Source[Int,akka.NotUsed] = Source(SourceShape(PublisherSource.out(989856637)))
```

### FS2

To go between [FS2](https://github.com/functional-streams-for-scala/fs2) Stream 
and Monix `Observable` you need to use [fs2-reactive-streams](https://github.com/zainab-ali/fs2-reactive-streams)
library but conversion remains very straightforward.

Necessary imports:

```tut:silent
import cats.effect._, fs2._
import fs2.interop.reactivestreams._
import monix.reactive.Observable
import monix.execution.Scheduler.Implicits.global
```

To convert FS2 `Stream` to Monix `Observable`:

```tut:silent
val stream = Stream(1, 2, 3).covary[IO]
// stream: fs2.Stream[cats.effect.IO,Int] = Stream(..)

val publisher = stream.toUnicastPublisher()
// publisher: fs2.interop.reactivestreams.StreamUnicastPublisher[cats.effect.IO,Int] = fs2.interop.reactivestreams.StreamUnicastPublisher@6418f777

val observable = Observable.fromReactivePublisher(publisher)
// observable: monix.reactive.Observable[Int] = monix.reactive.internal.builders.ReactiveObservable@7130d725
```

To go back from Monix `Observable` to FS2 `Stream`:

```tut:silent
val observable = Observable(1, 2, 3)
// observable: monix.reactive.Observable[Int] = monix.reactive.internal.builders.IterableAsObservable@4783cc8

val stream = observable.toReactivePublisher.toStream[IO]
// stream: fs2.Stream[cats.effect.IO,Int] = Stream(..)
```