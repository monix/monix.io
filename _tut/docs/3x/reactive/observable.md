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

... TO BE CONTINUED ...

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
It means that `Observable` preserves referential transparency. 

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

## Hot and Cold Observables

As mentioned before - by default `Observable` doesn't emit any items until something subscribes to it. This is called *cold Observable.*
In cold Observable there is only one subscriber who is guaranteed to see all the emitted items.

On the other hand there is also notion of *hot Observable* whose source is shared between many subscribers. 
It means that it matters when you subscribe to it and the order of subscription can affect your program.
For this reason tread carefully when using *hot Observable* because it breaks referential transparency.

### Turning cold Observable into hot Observable

### Subjects


## Simple builders

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

### Observable.tailRecM

### Observable.create && unsafeCreate

### Observable.interval

## Operations

## Backpressure

## Error Handling

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