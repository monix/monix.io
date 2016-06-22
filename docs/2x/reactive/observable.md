---
layout: docs
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