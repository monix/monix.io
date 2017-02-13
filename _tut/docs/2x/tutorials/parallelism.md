---
layout: docs
title: Parallel Processing
description: |
  Recipes for achieving parallelism
  
tut:
  scala: 2.11.8
  binaryScala: "2.11"
  dependencies:
    - io.monix::monix-reactive:version2x
---

Monix provides multiple ways for achieving parallelism, depending on use-case.

The samples in this document are copy/paste-able, but to get the imports out of the way:

```tut:silent
// On evaluation a Scheduler is needed
import monix.execution.Scheduler.Implicits.global
// For Task
import monix.eval._
// For Observable
import monix.reactive._
```

```tut:invisible
import monix.execution.schedulers.TestScheduler
implicit val global = TestScheduler()
```

## Parallelism with Task

We can do parallel execution in batches, that does deterministic
(ordered) signaling of results with the help of [Task](../eval/task.html).

### The Naive Way

The following example uses
[Task.gather]({{ site.api2x }}monix/eval/Task$.html#gather[A,M[X]<:TraversableOnce[X]](in:M[monix.eval.Task[A]])(implicitcbf:scala.collection.generic.CanBuildFrom[M[monix.eval.Task[A]],A,M[A]]):monix.eval.Task[M[A]]),
which does parallel processing while preserving result ordering, 
but in order to ensure that parallel processing actually happens,
the tasks need to be effectively asynchronous, which for simple
functions need to fork threads, hence the usage of 
[Task.apply]({{ site.api2x }}monix/eval/Task$.html#apply[A](f:=>A):monix.eval.Task[A]),
although remember that you can apply 
[Task.fork]({{ site.api2x }}monix/eval/Task$.html#fork[A](fa:monix.eval.Task[A]):monix.eval.Task[A])
to any task.

```tut:silent
val items = 0 until 1000

// The list of all tasks needed for execution
val tasks = items.map(i => Task(i * 2))
// Processing in parallel
val aggregate = Task.gather(tasks).map(_.toList)

// Evaluation:
aggregate.foreach(println)
//=> List(0, 2, 4, 6, 8, 10, 12, 14, 16,...
```

If ordering of results does not matter, you can also use 
[Task.gatherUnordered]({{ site.api2x }}monix/eval/Task$.html#gatherUnordered[A](in:TraversableOnce[monix.eval.Task[A]]):monix.eval.Task[List[A]])
instead of `gather`, which might yield better results, given its non-blocking execution.

### Imposing a Parallelism Limit

The `Task.gather` builder, as exemplified above, will potentially execute
all given tasks in parallel, the problem being that this can lead to inefficiency.
For example we might be doing HTTP requests and starting 10000 HTTP
requests in parallel is not necessarily wise as it can choke the
server on the other end.

To solve this we can split the workload in batches of parallel tasks that
are then sequenced:

```tut:silent
val items = 0 until 1000
// The list of all tasks needed for execution
val tasks = items.map(i => Task(i * 2))
// Building batches of 10 tasks to execute in parallel:
val batches = tasks.sliding(10,10).map(b => Task.gather(b))
// Sequencing batches, then flattening the final result
val aggregate = Task.sequence(batches).map(_.flatten.toList)

// Evaluation:
aggregate.foreach(println)
//=> List(0, 2, 4, 6, 8, 10, 12, 14, 16,...
```

Note how this strategy is difficult to achieve with Scala's `Future`
because even though we have `Future.sequence`, its behavior is strict
and is thus not able to differentiate well between sequencing and
parallelism, this behavior being controlled by passing a lazy or a
strict sequence to `Future.sequence`, which is obviously error-prone.

### Batched Observables

We can also combine this with `Observable.flatMap` for doing requests
in batches:

```tut:silent
import monix.eval._
import monix.reactive._

// The `bufferIntrospective` will do buffering, up to a certain
// `bufferSize`, for as long as the downstream is busy and then
// stream a whole sequence of all buffered events at once
val source = Observable.range(0,1000).bufferIntrospective(256)

// Processing in batches, powered by `Task`
val batched = source.flatMap { items =>
  // The list of all tasks needed for execution
  val tasks = items.map(i => Task(i * 2))
  // Building batches of 10 tasks to execute in parallel:
  val batches = tasks.sliding(10,10).map(b => Task.gather(b))
  // Sequencing batches, then flattening the final result
  val aggregate = Task.sequence(batches).map(_.flatten)
  // Converting into an observable, needed for flatMap
  Observable.fromTask(aggregate)
    .flatMap(i => Observable.fromIterator(i))
}

// Evaluation:
batched.toListL.foreach(println)
//=> List(0, 2, 4, 6, 8, 10, 12, 14, 16,...
```

Note the use of 
[bufferIntrospective]({{ site.api2x }}monix/reactive/Observable.html#bufferIntrospective(maxSize:Int):Self[List[A]]),
which buffers incoming events while the downstream is busy, after which
it emits the buffer as a single bundle. The
[bufferTumbling]({{ site.api2x }}monix/reactive/Observable.html#bufferTumbling(count:Int):Self[Seq[A]])
operator can be a more deterministic alternative.

## Observable.mapAsync

Another way to achieve parallelism is to use the 
[Observable.mapAsync]({{ site.api2x }}monix/reactive/Observable.html#mapAsync[B](parallelism:Int)(f:A=>monix.eval.Task[B]):Self[B])
operator:

```tut:silent
val source = Observable.range(0,1000)
// The parallelism factor needs to be specified
val processed = source.mapAsync(parallelism = 10) { i =>
  Task(i * 2)
}

// Evaluation:
processed.toListL.foreach(println)
//=> List(2, 10, 0, 4, 8, 6, 12...
```

Compared with using `Task.gather` as exemplified above, this operator
**does not maintain ordering** of results as signaled by the source.

This leads to a more efficient execution, because the source doesn't
get back-pressured for as long as there's at least one worker active,
whereas with the batched execution strategy exemplified above we can
have inefficiencies due to a single async task that takes too long to
complete.

## Observable.mergeMap

If `Observable.mapAsync` works with `Task`, then 
[Observable.mergeMap](https://monix.io/api/2.2/monix/reactive/Observable.html#mergeMap[B](f:A=%3Emonix.reactive.Observable[B])(implicitos:monix.reactive.OverflowStrategy[B]):Self[B])
works by merging `Observable` instances.

```tut:silent
val source = Observable.range(0,1000)
// The parallelism factor needs to be specified
val processed = source.mergeMap { i =>
  Observable.fork(Observable.eval(i * 2))
}

// Evaluation:
processed.toListL.foreach(println)
//=> List(0, 4, 6, 2, 8, 10, 12, 14...
```

Note that `mergeMap` is similar with `concatMap` (aliased by `flatMap`
in Monix), except that the observable streams emitted by the source
get subscribed in parallel and thus the result is non-deterministic.

Note that this `mergeMap` call, as exemplified above, does not have an
optional `parallelism` parameter, which means that if the source is
chatty, we can end up with *a lot* of observables subscribed in
parallel. The issue is that the `mergeMap` operator is not meant for
actual processing in parallel, but for joining active, concurrent
streams.

## Consumer.loadBalancer

We can apply a `mapAsync` like operation on the consumer side, as
exemplified in the [Consumer](../reactive/consumer.html) tutorial, by means of a
load-balanced consumer, being able to do a final aggregate of the
results of all workers:

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
task.runAsync.foreach(println)
//=> 4999950000
```

Read the [Consumer](../reactive/consumer.html) document for more details.
