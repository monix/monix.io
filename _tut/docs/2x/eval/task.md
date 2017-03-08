---
layout: docs
title: Task
type_api: monix.eval.Task
type_source: monix-eval/shared/src/main/scala/monix/eval/Task.scala
description: |
  A data type for controlling possibly lazy &amp; asynchronous computations, useful for controlling side-effects, avoiding nondeterminism and callback-hell.

tut:
  scala: 2.11.8
  binaryScala: "2.11"
  dependencies:
    - io.monix::monix-eval:version2x
    - org.slf4j:slf4j-api:1.7.21
---

## Introduction

Task is a data type for controlling possibly lazy &amp; asynchronous
computations, useful for controlling side-effects, avoiding
nondeterminism and callback-hell.

To get the imports out of the way:

```tut:silent
// In order to evaluate tasks, we'll need a Scheduler
import monix.execution.Scheduler.Implicits.global

// A Future type that is also Cancelable
import monix.execution.CancelableFuture

// Task is in monix.eval
import monix.eval.Task
import scala.util.{Success, Failure}
```

```tut:reset:invisible
import monix.execution.CancelableFuture
import monix.eval.Task
import scala.util.{Success, Failure}
import monix.execution.schedulers.TestScheduler
implicit val global = TestScheduler()
```

```tut:silent
// Executing a sum, which (due to the semantics of apply)
// will happen on another thread. Nothing happens on building
// this instance though, this expression is pure, being
// just a spec! Task by default has lazy behavior ;-)
val task = Task { 1 + 1 }

// Tasks get evaluated only on runAsync!
// Callback style:
val cancelable = task.runAsync { result =>
  result match {
    case Success(value) =>
      println(value)
    case Failure(ex) =>
      System.out.println(s"ERROR: ${ex.getMessage}")
  }
}
//=> 2

// Or you can convert it into a Future
val future: CancelableFuture[Int] =
  task.runAsync

// Printing the result asynchronously
future.foreach(println)
//=> 2
```

### Design Summary

In summary the Monix `Task`:

- models lazy &amp; asynchronous evaluation
- models a producer pushing only one value to one or multiple consumers
- allows fine-grained control over the [execution model](../execution/scheduler.html#execution-model)
- doesn’t trigger the execution, or any effects until `runAsync`
- doesn’t necessarily execute on another logical thread
- allows for cancelling of a running computation
- allows for controlling of side-effects, being just as
  potent as Haskell's I/O ;-)
- never blocks any threads in its implementation
- does not expose any API calls that can block threads

A visual representation of where they sit in the design
space:

|                    |        Eager        |            Lazy            |
|:------------------:|:-------------------:|:--------------------------:|
| **Synchronous**    |          A          |           () => A          |
|                    |                     | [Coeval[A]](./coeval.html) |
| **Asynchronous**   | (A => Unit) => Unit |    (A => Unit) => Unit     |
|                    |      Future[A]      |          Task[A]           |

### Comparison with Scala's Future

`Task` sounds similar with Scala's
[Future](http://docs.scala-lang.org/overviews/core/futures.html), but
has a different character and the two types as you'll see are actually
complementary. A wise man once said:

> "*A Future represents a value, detached from time*" &mdash; Viktor Klang

That's certainly a poetic notion, making one think about what values
are and how they incorporate time. But more importantly, while we
cannot say that a `Future` is a
[value](https://en.wikipedia.org/wiki/Value_(computer_science)),
we can certainly say that it's a *value-wannabe*, meaning that when
users receive a `Future` reference, they know that whatever process
that's going to evaluate it has probably already started and it might
have even finished already. This makes the behavior of Scala's
`Future` to be about *eager evaluation* and certainly its design helps
with that, if you think about how it takes that implicit execution
context whenever you call its operators, like `map` and `flatMap`.

But `Task` is different. `Task` is about lazy evaluation. Well, not
always lazy, in fact `Task` allows for fine-tuning the execution
model, as you'll see, but that's the primary distinction between
them. If `Future` is like a value, then `Task` is like a function. And
in fact `Task` can function as a "factory" of `Future` instances.

Another distinction is that `Future` is "*memoized*" by default,
meaning that its result is going to be shared between multiple
consumers if needed. But the evaluation of a `Task` is not memoized by
default. No, you have to want memoization to happen, you have to
specify it explicitly, as you'll see.

In terms of efficiency, `Future` having eager behavior, happens to be
less efficient because whatever operation you're doing on it, the
implementation will end up sending `Runnable` instances in the
thread-pool and because the result is always memoized on each step,
invoking that machinery (e.g. going into compare-and-set loops)
whatever you're doing. On the other hand `Task` can do execution in
synchronous batches.

### Comparison with the Scalaz Task

It's no secret that the Monix Task was inspired by the
[Scalaz](https://github.com/scalaz/scalaz) Task, an otherwise solid
implementation. The whole Monix library stands on the shoulders of
giants. But where the Monix Task implementation disagrees:

1. The Scalaz Task is leaking implementation details. This is because
   the Scalaz Task is first and foremost about *trampolined*
   execution, but asynchronous execution is about jumping over
   asynchronous and thus trampoline boundaries. So the API is limited
   by what the trampoline can do and for example in order to not block
   the current thread in a big loop, you have to *manually insert*
   async boundaries yourself by means of `Task.fork`. The Monix Task
   on the other hand manages to do that automatically by default,
   which is very useful when running on top of
   [Javascript](http://www.scala-js.org/), where
   [cooperative multitasking](https://en.wikipedia.org/wiki/Cooperative_multitasking)
   is not only nice to have, but required.
2. The Scalaz Task has a dual synchronous / asynchronous
   personality. That is fine for optimization purposes as far as the
   producer is concerned (i.e. why fork a thread when you don't have
   to), but from the consumer's point of view having a `def run: A`
   means that the API cannot be fully supported on top of Javascript
   and on top of the JVM it means that the `Task` ends up faking
   synchronous evaluation and blocking threads. And [blocking threads
   is very unsafe](../best-practices/blocking.html).
3. The Scalaz Task cannot cancel running computations. This is
   important for nondeterministic operations. For example when you
   create a race condition with a `chooseFirstOf`, you may want to
   cancel the slower task that didn't finish in time, because
   unfortunately, if we don't release resources soon enough, we can
   end up with serious leakage that can crash our process.
4. The Scalaz Task piggybacks on top of Java's standard library for
   dealing with asynchronous execution. This is bad for portability
   reasons, as this API is not supported on top of
   [Scala.js](http://www.scala-js.org/).

## Execution (runAsync & foreach)

`Task` instances won't do anything until they are executed by means
of `runAsync`. And there are multiple overloads of it.

`Task.runAsync` also wants an implicit
[Scheduler](../execution/scheduler.html) in scope, that can supplant
your `ExecutionContext` (since it inherits from it). But this is where
the design of `Task` diverges from Scala's own `Future`. The `Task`
being lazy, it only wants this `Scheduler` on execution with
`runAsync`, instead of wanting it on every operation (like `map` or
`flatMap`), the way that Scala's `Future` does.

So first things first, we need a `Scheduler` in scope. The `global` is
piggybacking on Scala's own `global`, so now you can do this:

```tut:silent
import monix.execution.Scheduler.Implicits.global
```
```tut:invisible
import monix.execution.schedulers.TestScheduler
implicit val global = TestScheduler()
```

**NOTE:** The [Scheduler](../execution/scheduler.html) can inject a
configurable
[execution model](../execution/scheduler.html#execution-model) which
determines how asynchronous boundaries get forced (or not). Read up on
it.

The most straightforward and idiomatic way would be to execute
tasks and get a
[CancelableFuture]({{ site.api2x }}#monix.execution.CancelableFuture)
in return, which is a standard `Future` paired with a
[Cancelable](../execution/cancelable.html):

```tut:silent
import monix.eval.Task
import monix.execution.CancelableFuture
import concurrent.duration._

val task = Task(1 + 1).delayExecution(1.second)

val result: CancelableFuture[Int] =
  task.runAsync

// If we change our mind
result.cancel()
```

Returning a `Future` might be too heavy for your needs, you might want
to provide a simple callback. We can also `runAsync` with a `Try[T] =>
Unit` callback, just like the standard `Future.onComplete`.

```tut:silent
import scala.util.{Success, Failure}

val task = Task(1 + 1).delayExecution(1.second)

val cancelable = task.runAsync { result =>
  result match {
    case Success(value) =>
      println(value)
    case Failure(ex) =>
      System.err.println(s"ERROR: ${ex.getMessage}")
  }
}

// If we change our mind...
cancelable.cancel()
```

We can also `runAsync` with a [Callback](./callback.html) instance.
This is like a Java-ish API, useful in case, for any reason whatsoever,
you want to keep state. `Callback` is also used internally, because it
allows us to guard against contract violations and to avoid the boxing
specific to `Try[T]`. Sample:

```tut:silent
import monix.eval.Callback

val task = Task(1 + 1).delayExecution(1.second)

val cancelable = task.runAsync(
  new Callback[Int] {
    def onSuccess(value: Int): Unit =
      println(value)
    def onError(ex: Throwable): Unit =
      System.err.println(s"ERROR: ${ex.getMessage}")
  })

// If we change our mind...
cancelable.cancel()
```

But if you just want to trigger some side-effects quickly, you can
just use `foreach` directly:

```tut:silent
val task = Task { println("Effect!"); "Result" }

task.foreach { result => println(result) }
//=> Effect!
//=> Result

// Or we can use for-comprehensions
for (result <- task) {
  println(result)
}
```

NOTE: `foreach` on `Task` does not block, but returns a
`CancelableFuture[Unit]` that can be used to block on the execution,
or for cancellation.

### Blocking for a Result

Monix is [against blocking](../best-practices/blocking.html) as a
matter of philosophy, therefore `Task` doesn't have any API calls that
blocks threads, none!

However, on top of the JVM sometimes we have to block. And if we have
to block, Monix doesn't try to outsmart Scala's standard library,
because the standard `Await.result` and `Await.ready` have two healthy
design choices:

1. These calls use Scala's `BlockContext` in their implementation,
   signaling to the underlying thread-pool that a blocking operation
   is being executed, allowing the thread-pool to act on it. For
   example it might decide to add more threads in the pool, like
   Scala's `ForkJoinPool` is doing.
2. These calls require a very explicit timeout parameter, specified as
   a `FiniteDuration`, triggering a `TimeoutException` in case that
   specified timespan is exceeded without the source being ready.

Therefore in order to block on a result, you have to first convert it
into a `Future` by means of `runAsync` and then you can block on it:

```tut:silent
import monix.execution.Scheduler.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._

val task = Task.fork(Task.eval("Hello!"))
val future = task.runAsync

Await.result(future, 3.seconds)
//=> Hello!

// Or by using foreach
val completed = for (r <- task) { println("Completed!") }

Await.result(completed, 3.seconds)
//=> Hello!
//=> Completed!
```
```tut:invisible
import monix.execution.schedulers.TestScheduler
implicit val global = TestScheduler()
```

**NOTE:** There is [no blocking](https://github.com/scala-js/scala-js/issues/186)
on Scala.js by design.

### Try Immediate Execution (Coeval)

Monix is against blocking, we've established that. But clearly some
`Task` instances can be evaluated immediately on the current logical
thread, if allowed by the execution model. And for *optimization
purposes*, we might want to act immediately on their results, avoiding
dealing with callbacks.

To do that, we can convert a `Task` into a `Coeval`:

```tut:silent
val task = Task.eval("Hello!")

val tryingNow = task.coeval
// tryingNow: Coeval[Either[CancelableFuture[String],String]] = ???

tryingNow.value match {
  case Left(future) =>
    // No luck, this Task really wants async execution
    future.foreach(r => println(s"Async: $r"))
  case Right(result) =>
    println(s"Got lucky: $result")
}
```

**NOTE:** as it happens, by default the `eval` builder is
executing things on the current thread, unless an async boundary is
forced by the underlying loop. So this code will always print "*Got
Lucky*" ;-)

## Simple Builders

If you can accept its possibly asynchronous nature, `Task` can replace
functions accepting zero arguments, Scala by-name params and `lazy
val`. And any Scala `Future` is convertible to `Task`.

### Task.now


`Task.now`
lifts an already known value in the `Task` context,
the equivalent of `Future.successful` or of `Applicative.pure`:

```tut:silent
val task = Task.now { println("Effect"); "Hello!" }
//=> Effect
// task: monix.eval.Task[String] = Delay(Now(Hello!))
```

### Task.eval (delay)

`Task.eval`
is the equivalent of `Function0`, taking a function
that will always be evaluated on `runAsync`, possibly on the same
thread (depending on the chosen
[execution model](../execution/scheduler.html#execution-model)):

```tut:silent
val task = Task.eval { println("Effect"); "Hello!" }
// task: monix.eval.Task[String] = Delay(Always(<function0>))

task.runAsync.foreach(println)
//=> Effect
//=> Hello!

// The evaluation (and thus all contained side effects)
// gets triggered on each runAsync:
task.runAsync.foreach(println)
//=> Effect
//=> Hello!
```

NOTE: for Scalaz converts, this function is also aliased as `Task.delay`.

### Task.evalOnce

`Task.evalOnce`
is the equivalent of a `lazy val`, a type that cannot
be precisely expressed in Scala. The `evalOnce` builder does
memoization on the first run, such that the result of the evaluation
will be available for subsequent runs. It also has guaranteed
idempotency and thread-safety:

```tut:silent
val task = Task.evalOnce { println("Effect"); "Hello!" }
// task: monix.eval.Task[String] = EvalOnce(<function0>)

task.runAsync.foreach(println)
//=> Effect
//=> Hello!

// Result was memoized on the first run!
task.runAsync.foreach(println)
//=> Hello!
```

NOTE: this operation is effectively `Task.eval(f).memoize`.

### Task.defer (suspend)

`Task.defer`
is about building a factory of tasks. For example this
will behave approximately like `Task.eval`:

```tut:silent
val task = Task.defer {
  Task.now { println("Effect"); "Hello!" }
}
// task: monix.eval.Task[String] = Suspend(<function0>)

task.runAsync.foreach(println)
//=> Effect
//=> Hello!

task.runAsync.foreach(println)
//=> Effect
//=> Hello!
```

NOTE: for Scalaz converts, this function is also aliased as `Task.suspend`.

### Task.fromFuture

`Task.fromFuture` can convert any Scala `Future` instance into a `Task`:

```tut:silent
import scala.concurrent.Future

val future = Future { println("Effect"); "Hello!" }
val task = Task.fromFuture(future)
//=> Effect

task.runAsync.foreach(println)
//=> Hello!
task.runAsync.foreach(println)
//=> Hello!
```

Note that `fromFuture` takes a strict argument and that may not be
what you want. You might want a factory of `Future`. The design of
`Task` however is to have fine-grained control over the evaluation
model, so in case you want a factory, you need to combine it with
`Task.defer`:

```tut:silent
val task = Task.defer {
  val future = Future { println("Effect"); "Hello!" }
  Task.fromFuture(future)
}
//=> task: monix.eval.Task[Int] = Suspend(<function0>)

task.runAsync.foreach(println)
//=> Effect
//=> Hello!
task.runAsync.foreach(println)
//=> Effect
//=> Hello!
```

### Task.deferFuture

A `Future` reference is like a strict value, meaning that when you receive one,
whatever process that's supposed to complete it has probably started already.

Therefore it makes sense to defer the evaluation of futures when building tasks:

```tut:silent
val task = Task.defer {
  val future = Future { println("Effect"); "Hello!" }
  Task.fromFuture(future)
}
```

As a shortcut, you can also use the `deferFuture` builder, which is equivalent
with the above:

```tut:silent
val task = Task.deferFuture {
  Future { println("Effect"); "Hello!" }
}
```

### Task.deferFutureAction

Wraps calls that generate `Future` results into `Task`, provided a
callback with an injected `Scheduler` to act as the necessary
`ExecutionContext`.

This builder helps with wrapping `Future`-enabled APIs that need an
implicit `ExecutionContext` to work. Consider this example:

```tut:silent
import scala.concurrent.{ExecutionContext, Future}

def sumFuture(list: Seq[Int])(implicit ec: ExecutionContext): Future[Int] =
  Future(list.sum)
```

We'd like to wrap this function into one that returns a lazy `Task`
that evaluates this sum every time it is called, because that's how
tasks work best. However in order to invoke this function an
`ExecutionContext` is needed:

```tut:silent
def sumTask(list: Seq[Int])(implicit ec: ExecutionContext): Task[Int] =
  Task.deferFuture(sumFuture(list))
```

But this is not only superfluous, but against the best practices of
using `Task`. The difference is that `Task` takes a `Scheduler`
(inheriting from `ExecutionContext`) only when `runAsync` gets called,
but we don't need it just for building a `Task` reference.  With
`deferFutureAction` we get to have an injected `Scheduler` in the
passed callback:

```tut:silent
def sumTask(list: Seq[Int]): Task[Int] =
  Task.deferFutureAction { implicit scheduler =>
    sumFuture(list)
  }
```

Voilà! No more implicit `ExecutionContext` passed around.

### Task.fork && Task.asyncBoundary

`Task.fork` ensures an asynchronous boundary, forcing the fork of a
(logical) thread on execution. Sometimes we are doing something really
wasteful and we want to guarantee that an asynchronous boundary
happens, given that by default
the [execution model](../execution/scheduler.html#execution-model)
prefers to execute things on the current thread, at first.

So this guarantees that our task will get executed asynchronously:

```tut:silent
val task = Task.fork(Task.eval("Hello!"))
```

In fact that's how `apply` is defined:

```scala
object Task {
  def apply[A](f: => A): Task[A] =
    fork(eval(f))
  //...
}
```

Fork also allows us to specify an alternative `Scheduler` to use.
You see, the run-loop of `Task` always has a `Scheduler` available, but
for certain operations you might want to divert the processing to an alternative
scheduler. For example you might want to execute blocking I/O operations
on an alternative thread-pool.

Lets assume we have 2 thread-pools:

```tut:silent
// The default scheduler
import monix.execution.Scheduler.Implicits.global

// Creating a special scheduler meant for I/O
import monix.execution.Scheduler
lazy val io = Scheduler.io(name="my-io")
```
```tut:reset:invisible
import monix.eval._
import monix.execution.schedulers.TestScheduler
implicit val global = TestScheduler()
lazy val io = TestScheduler()
```

Then we can manage what executes on which:

```tut:silent
// Override the default Scheduler by fork:
val source = Task(println(s"Running on thread: ${Thread.currentThread.getName}"))
val forked = Task.fork(source, io)

source.runAsync
//=> Running on thread: ForkJoinPool-1-worker-1
forked.runAsync
//=> Running on thread: my-io-4
```

Note that, unless another asynchronous boundary is scheduled on the
default `Scheduler`, execution remains on the last scheduler (thread-pool)
used. Notice what happens in this combination:

```tut:silent
val onFinish = Task.eval(
  println(s"Ends on thread: ${Thread.currentThread.getName}")
)

val cancelable = {
  source.flatMap(_ => forked)
    .doOnFinish(_ => onFinish)
    .runAsync
}

//=> Running on thread: ForkJoinPool-1-worker-7
//=> Running on thread: my-io-1
//=> Ends on thread: my-io-1
```

But if we insert another async boundary, then it switches back
to the default:

```tut:silent
val asyncBoundary = Task.fork(Task.unit)
val onFinish = Task.eval(
  println(s"Ends on thread: ${Thread.currentThread.getName}"))

val cancelable = {
  source // executes on global
    .flatMap(_ => forked) // executes on io
    .flatMap(_ => asyncBoundary) // switch back to global
    .doOnFinish(_ => onFinish) // executes on global
    .runAsync
}

//=> Running on thread: ForkJoinPool-1-worker-5
//=> Running on thread: my-io-2
//=> Ends on thread: ForkJoinPool-1-worker-5
```

But `Task` also provides a convenient operator for introducing an
asynchronous boundary without having to manually do this trick, called
`Task.asyncBoundary`:

```tut:silent
val task = {
  source // executes on global
    .flatMap(_ => forked) // executes on io
    .asyncBoundary // switch back to global
    .doOnFinish(_ => onFinish) // executes on global
    .runAsync
}
```

Note that overriding of the scheduler can only happen once, as
`Task` instances are immutable, so the following does not work,
because for the `forked` instance the `Scheduler` was already
set in stone and we only have flexibility to override the
default if it hasn't been overridden already:

```tut:silent
// Trying to execute on global
Task.fork(forked, global).runAsync
//=> Running on thread: my-io-4
```

There are also two `Task` methods specified as aliases for `Task.fork`,
called `executeOn` and `executeWithFork` respectively. So you can do:

```tut:silent
val task = {
  source.executeOn(io)
    .asyncBoundary
}
```

**General advice:** unless you're doing blocking I/O, keep using
the default thread-pool, with `global` being a good default.
For blocking I/O it is OK to have a second thread-pool,
but isolate those I/O operations and only override the scheduler
for actual I/O operations.

### Task.raiseError

`Task.raiseError` can lift errors in the monadic context of `Task`:

```tut:silent
import scala.concurrent.TimeoutException

val error = Task.raiseError[Int](new TimeoutException)
// error: monix.eval.Task[Int] =
//   Delay(Error(java.util.concurrent.TimeoutException))

error.runAsync(result => println(result))
//=> Failure(java.util.concurrent.TimeoutException)
```

### Task.never

`Task.never` returns a `Task` instance that never completes:

```tut:silent
import scala.concurrent.duration._
import scala.concurrent.TimeoutException

// A Task instance that never completes
val never = Task.never[Int]

val timedOut = never.timeoutTo(3.seconds,
  Task.raiseError(new TimeoutException))

timedOut.runAsync(r => println(r))
// After 3 seconds:
// => Failure(java.util.concurrent.TimeoutException)
```

This instance is shared, so that can relieve some stress from the
garbage collector.

### Task.unit

`Task.unit` is returning an already completed `Task[Unit]` instance,
provided as a utility, to spare you creating new instances with
`Task.now(())`:

```tut:silent
val task = Task.unit
// task: monix.eval.Task[Unit] = Delay(Now(()))
```

This instance is shared, so that can relieve some stress from the
garbage collector.

## Asynchronous Builders

You can use any async API to build a `Task`. There's an unsafe
version, for people knowing what they are doing and a safe version, that
handles some of the nitty-gritty automatically.

### Task.create

Also known as `Task.async` (for Scalaz refugees), the `Task.create`
function allows for creating an asynchronous `Task` using a
callback-based API. For example, let's create a utility that evaluates
expressions with a given delay:

```tut:silent
import scala.util.Try
import concurrent.duration._

def evalDelayed[A](delay: FiniteDuration)
  (f: => A): Task[A] = {

  // On execution, we have the scheduler and
  // the callback injected ;-)
  Task.create { (scheduler, callback) =>
    val cancelable =
      scheduler.scheduleOnce(delay) {
        callback(Try(f))
      }

    // We must return something that can
    // cancel the async computation
    cancelable
  }
}
```

And here's a possible implementation of
[Task.fromFuture](#taskfromfuture), in case you choose to implement it
yourself:

```tut:silent
import monix.execution.Cancelable
import scala.concurrent.Future
import scala.util.{Success, Failure}

def fromFuture[A](f: Future[A]): Task[A] =
  Task.create { (scheduler, callback) =>
    f.onComplete({
      case Success(value) =>
        callback.onSuccess(value)
      case Failure(ex) =>
        callback.onError(ex)
    })(scheduler)

    // Scala Futures are not cancelable, so
    // we shouldn't pretend that they are!
    Cancelable.empty
  }
```

Some notes:

- Tasks created with this builder are guaranteed to execute
  asynchronously (on another logical thread)
- The [Scheduler](../execution/scheduler.html) gets injected and with
  it we can schedule things for async execution, we can delay,
  etc...
- But as said, this callback will already execute asynchronously, so
  you don't need to explicitly schedule things to run on the provided
  `Scheduler`, unless you really need to do it.
- [The Callback](./callback.html) gets injected on execution and that
  callback has a contract. In particular you need to execute
  `onSuccess` or `onError` or `apply` only once. The implementation
  does a reasonably good job to protect against contract violations,
  but if you do call it multiple times, then you're doing it risking
  undefined and nondeterministic behavior.
- It's OK to return a `Cancelable.empty` in case the executed
  process really can't be canceled in time, but you should strive to
  return a cancelable that does cancel your execution, if possible.

### Task.unsafeCreate

`Task.unsafeCreate` has the same purpose and function as
[Task.create](#taskcreate), only this is for people knowing what they
are doing, being the *unsafe version*. In the development of Monix
there were doubts whether this should be exposed or not. It gets
exposed because otherwise there's no way to replace its functionality
for certain use-cases.

**WARNING:** this isn't for normal usage. Prefer [Task.create](#taskcreate).

The callback that needs to be passed to `unsafeCreate` this time has
this type:

```scala
object Task {
  // ...

  type OnFinish[+A] = (Context, Callback[A]) => Unit

  final case class Context(
    scheduler: Scheduler,
    connection: StackedCancelable,
    frameRef: ThreadLocal[FrameIndex],
    options: Options
  )
}
```

So instead of returning a
simple [Cancelable](../execution/cancelable.html) we get to deal with
an injected
[StackedCancelable]({{ site.api2x }} #monix.execution.cancelables.StackedCancelable),
along with something called a `FrameIndex` that's a `ThreadLocal` and
some special `Options` instead.

This is because you

Let implement our own version of the `delayExecution` operator, just
for the kicks:

```tut:silent
import monix.execution.cancelables._

def delayExecution[A](
  source: Task[A], timespan: FiniteDuration): Task[A] = {

  Task.unsafeCreate { (context, cb) =>
    implicit val s = context.scheduler
    // A stack that keeps track of what we need to cancel
    val conn = context.connection
    // We need the forward reference, because otherwise `conn.pop`
    // below can happen before pushing that reference in `conn`
    val c = SingleAssignmentCancelable()
    conn push c

    c := s.scheduleOnce(timespan.length, timespan.unit, new Runnable {
      def run(): Unit = {
        // Releasing our cancelable because our scheduled
        // task is done and we need to let the GC collect it
        conn.pop()

        // We had an async boundary, so we must reset the frame
        // index in order to let Monix know we've had a real
        // async boundary - on the JVM this is not needed, since
        // this is a thread-local, but on JS we don't have threads
        context.frameRef.reset()

        // We can now resume execution, by finally starting
        // our source. As you can see, we just inject our
        // StackedCancelable, there's no need to create another
        // Cancelable reference, so at this point it's as if
        // the source is being executed without any overhead!
        Task.unsafeStartNow(source, context, Callback.async(cb))
      }
    })
  }
}
```

As you can see, this really is unsafe and actually unneeded in most
cases. So don't use it, or if you think you need it, maybe ask for
help.

¯＼(º_o)/¯

## Memoization

The
[Task#memoize]({{ site.api2x }}#monix.eval.Task@memoize:monix.eval.Task[A])
operator can take any `Task` and apply memoization on the first `runAsync`,
such that:

1. you have guaranteed idempotency, calling `runAsync` multiple times
   will have the same effect as calling it once
2. subsequent `runAsync` calls will reuse the result computed by the
   first `runAsync`

So `memoize` effectively caches the result of the first `runAsync`.
In fact we can say that:

```scala
Task.evalOnce(f) <-> Task.eval(f).memoize
```

They are effectively the same.  And `memoize` works
with any task reference:

```tut:silent
// Has async execution, to do the .apply semantics
val task = Task { println("Effect"); "Hello!" }

val memoized = task.memoize

memoized.runAsync.foreach(println)
//=> Effect
//=> Hello!

memoized.runAsync.foreach(println)
//=> Hello!
```

### Memoize Only on Success

Sometimes you just want memoization, along with idempotency
guarantees, only for successful values. For failures you might want to
keep retrying until a successful value is available.

This is where the `memoizeOnSuccess` operator comes in handy:

```tut:silent
var effect = 0

val source = Task.eval { 
  effect += 1
  if (effect < 3) throw new RuntimeException("dummy") else effect
}

val cached = source.memoizeOnSuccess

val f1 = cached.runAsync // yields RuntimeException
val f2 = cached.runAsync // yields RuntimeException
val f3 = cached.runAsync // yields 3
val f4 = cached.runAsync // yields 3
```

### Memoize versus runAsync

You can say that when we do this:

```tut:silent
val task = Task { println("Effect"); "Hello!" }
val future = task.runAsync
```

That `future` instance is also going to be a memoized value of the
first `runAsync` execution, which can be reused for other `onComplete`
subscribers.

The difference is the same as the difference between `Task` and
`Future`. The `memoize` operation is lazy, evaluation only being
triggered on the first `runAsync`, whereas the result of `runAsync` is
eager.

## Operations

### FlatMap and Tail-Recursive Loops

So let's start with a stupid example that calculates the N-th number in
the Fibonacci sequence:

```tut:silent
import scala.annotation.tailrec

@tailrec
def fib(cycles: Int, a: BigInt, b: BigInt): BigInt = {
 if (cycles > 0)
   fib(cycles-1, b, a + b)
 else
   b
}
```

We need this to be tail-recursive, hence the use of the
[@tailrec](http://www.scala-lang.org/api/current/index.html#scala.annotation.tailrec)
annotation from Scala's standard library. And if we'd describe it with
`Task`, one possible implementation would be:

```tut:silent
def fib(cycles: Int, a: BigInt, b: BigInt): Task[BigInt] = {
 if (cycles > 0)
   Task.defer(fib(cycles-1, b, a+b))
 else
   Task.now(b)
}
```

And now there are already differences. This is lazy, as the N-th
Fibonacci number won't get calculated until we `runAsync`. The
`@tailrec` annotation is also not needed, as this is stack (and heap)
safe.

`Task` has `flatMap`, which is the monadic `bind` operation, that for
things like `Task` and `Future` is the operation that describes
recursivity or that forces ordering (e.g. execute this, then that,
then that). And we can use it to describe recursive calls:

```tut:silent
def fib(cycles: Int, a: BigInt, b: BigInt): Task[BigInt] =
  Task.eval(cycles > 0).flatMap {
    case true =>
      fib(cycles-1, b, a+b)
    case false =>
      Task.now(b)
  }
```

Again, this is stack safe and uses a constant amount of memory, so no
`@tailrec` annotation is needed or wanted. And it has lazy behavior,
as nothing will get triggered until `runAsync` happens.

But we can also have **mutually tail-recursive calls**, w00t!

```tut:silent
// Mutual Tail Recursion, ftw!!!
{
  def odd(n: Int): Task[Boolean] =
    Task.eval(n == 0).flatMap {
      case true => Task.now(false)
      case false => even(n - 1)
    }

  def even(n: Int): Task[Boolean] =
    Task.eval(n == 0).flatMap {
      case true => Task.now(true)
      case false => odd(n - 1)
    }

  even(1000000)
}
```

Again, this is stack safe and uses a constant amount of memory.  And
best of all, because of the
[execution model](../execution/scheduler.html#execution-model), by
default these loops won't block the current thread forever, preferring to
execute things in batches.

### The Applicative: zip2, zip3, ... zip6

When using `flatMap`, we often end up with this:

```tut:silent
val locationTask: Task[String] = Task.eval(???)
val phoneTask: Task[String] = Task.eval(???)
val addressTask: Task[String] = Task.eval(???)

// Ordered operations based on flatMap ...
val aggregate = for {
  location <- locationTask
  phone <- phoneTask
  address <- addressTask
} yield {
  "Gotcha!"
}
```

For one the problem here is that these operations are executed in
order. This also happens with Scala's standard `Future`, being
sometimes an unwanted effect, but because `Task` is lazily evaluated,
this effect is even more pronounced with `Task`.

But `Task` is also an `Applicative` and hence it has utilities, such
as `zip2`, `zip3`, up until `zip6` (at the moment of writing) and also
`zipList`. The example above could be written as:

```tut:silent
val locationTask: Task[String] = Task.eval(???)
val phoneTask: Task[String] = Task.eval(???)
val addressTask: Task[String] = Task.eval(???)

// Potentially executed in parallel
val aggregate =
  Task.zip3(locationTask, phoneTask, addressTask).map {
    case (location, phone, address) => "Gotcha!"
  }
```

In order to avoid boxing into tuples, you can also use `zipMap2`,
`zipMap3` ... `zip6`:

```tut:silent
Task.zipMap3(locationTask, phoneTask, addressTask) {
  (location, phone, address) => "Gotcha!"
}
```

### Gather results from a Seq of Tasks

`Task.sequence`, takes a `Seq[Task[A]]` and returns a `Task[Seq[A]]`,
thus transforming any sequence of tasks into a task with a sequence of
results and with ordered effects and results. This means that the
tasks WILL NOT execute in parallel.

```tut:silent
val ta = Task { println("Effect1"); 1 }
val tb = Task { println("Effect2"); 2 }

val list: Task[Seq[Int]] =
  Task.sequence(Seq(ta, tb))

// We always get this ordering:
list.runAsync.foreach(println)
//=> Effect1
//=> Effect2
//=> List(1, 2)
```

The results are ordered in the order of the initial sequence, so that
means in the example above we are guaranteed in the result to first
get the result of `ta` (the first task) and then the result of `tb`
(the second task). The execution itself is also ordered, so `ta`
executes and completes before `tb`.

`Task.gather`, also known as `Task.zipList`, is the nondeterministic
version of `Task.sequence`.  It also takes a `Seq[Task[A]]` and
returns a `Task[Seq[A]]`, thus transforming any sequence of tasks into
a task with a sequence of ordered results. But the effects are not
ordered, meaning that there's potential for parallel execution:

```tut:silent
val ta = {
  Task { println("Effect1"); 1 }
    .delayExecution(1.second)
}

val tb = {
  Task { println("Effect2"); 2 }
    .delayExecution(1.second)
}

val list: Task[Seq[Int]] = Task.gather(Seq(ta, tb))

list.runAsync.foreach(println)
//=> Effect1
//=> Effect2
//=> List(1, 2)

list.runAsync.foreach(println)
//=> Effect2
//=> Effect1
//=> List(1, 2)
```

`Task.gatherUnordered` is like `gather`, except that you don't get
ordering for results or effects. The result is thus highly nondeterministic,
but yields better performance than `gather`:

```tut:silent
val ta = {
  Task { println("Effect1"); 1 }
    .delayExecution(1.second)
}

val tb = {
  Task { println("Effect2"); 2 }
    .delayExecution(1.second)
}

val list: Task[Seq[Int]] =
  Task.gatherUnordered(Seq(ta, tb))

list.runAsync.foreach(println)
//=> Effect2
//=> Effect1
//=> Seq(2,1)

list.runAsync.foreach(println)
//=> Effect1
//=> Effect2
//=> Seq(1,2)
```

### Choose First Of Two Tasks

The `chooseFirstOf` operation will choose the winner between two
`Task` that will potentially run in parallel:

```tut:silent
val ta = Task(1 + 1).delayExecution(1.second)
val tb = Task(10).delayExecution(1.second)

val race = Task.chooseFirstOf(ta, tb).runAsync.foreach {
  case Left((a, futureB)) =>
    futureB.cancel()
    println(s"A succeeded: $a")
  case Right((futureA, b)) =>
    futureA.cancel()
    println(s"B succeeded: $b")
}
```

The result generated will be an `Either` of tuples, giving you the
opportunity to do something with the other task that lost the race.
You can cancel it, or you can use its result somehow, or you can
simply ignore it, your choice depending on use-case.

### Choose First Of List

The `chooseFirstOfList` operation takes as input a list of tasks,
and upon execution will generate the result of the first task
that completes and wins the race:

```tut:silent
val ta = Task(1 + 1).delayExecution(1.second)
val tb = Task(10).delayExecution(1.second)

{
  Task.chooseFirstOfList(Seq(ta, tb))
    .runAsync
    .foreach(r => println(s"Winner: $r"))
}
```

It is similar to Scala's
[Future.firstCompletedOf](http://www.scala-lang.org/api/current/index.html#scala.concurrent.Future$@firstCompletedOf[T](futures:TraversableOnce[scala.concurrent.Future[T]])(implicitexecutor:scala.concurrent.ExecutionContext):scala.concurrent.Future[T])
operation, except that it operates on `Task` and upon execution it has
a better model, as when a task wins the race the other tasks get
immediately canceled.

### Delay Execution

`Task.delayExecution`, as the name says, delays the execution of a
given task by the given timespan.

In this example we are delaying the execution of the source by 3
seconds:

```tut:silent
import scala.concurrent.duration._

val source = Task {
  println("Side-effect!")
  "Hello, world!"
}

val delayed = source.delayExecution(3.seconds)
delayed.runAsync.foreach(println)
```

Or, instead of a delay we might want to use another `Task` as the
signal for starting the execution, so the following example is
equivalent to the one above:

```tut:silent
val trigger = Task.unit.delayExecution(3.seconds)

val source = Task {
  println("Side-effect!")
  "Hello, world!"
}

val delayed = source.delayExecutionWith(trigger)
delayed.runAsync.foreach(println)
```

### Delay Signaling of the Result

`Task.delayResult` delays the signaling of the result, but not the
execution of the `Task`. Consider this example:

```tut:silent
import scala.concurrent.duration._

val source = Task {
  println("Side-effect!")
  "Hello, world!"
}

val delayed = {
  source
    .delayExecution(1.second)
    .delayResult(5.seconds)
}

delayed.runAsync.foreach(println)
```

Here, you'll see the "side-effect happening after only 1 second, but
the signaling of the result will happen after another 5 seconds.

There's also another variant called `delayResultBySelector`, where you
can have another task signal the right moment when to signal the
result downstream. This allows to customize the delay based on the
result signaled by the source:

```tut:silent
import scala.concurrent.duration._
import scala.util.Random

val source = Task {
  println("Side-effect!")
  Random.nextInt(10)
}

def selector(x: Int): Task[Unit] =
  Task.unit.delayExecution(x.seconds)

val delayed = {
  source
    .delayExecution(1.second)
    .delayResultBySelector(x => selector(x))
}

delayed.runAsync.foreach { x =>
  println(
    s"Result: $x " +
    s"(signaled after at least ${x+1} seconds)")
}
```

### Restart Until Predicate is True

The `Task` being a spec, we can restart it at will.
`Task.restartUntil(predicate)` does just that, executing the source
over and over again, until the given predicate is true:

```tut:silent
import scala.util.Random

val randomEven = {
  Task.eval(Random.nextInt())
    .restartUntil(_ % 2 == 0)
}

randomEven.runAsync.foreach(println)
//=> -2097793116
randomEven.runAsync.foreach(println)
//=> 1246761488
randomEven.runAsync.foreach(println)
//=> 1053678416
```

### Clean-up Resources on Finish

`Task.doOnFinish` executes the supplied
`Option[Throwable] => Task[Unit]` function when the source finishes,
being meant for cleaning up resources or executing
some scheduled side-effect:

```tut:silent
val task = Task(1)

val withFinishCb = task.doOnFinish {
  case None =>
    println("Was success!")
    Task.unit
  case Some(ex) =>
    println(s"Had failure: $ex")
    Task.unit
}

withFinishCb.runAsync.foreach(println)
//=> Was success!
//=> 1
```

### Convert to Reactive Publisher

Did you know that Monix integrates with the
[Reactive Streams](http://www.reactive-streams.org/)
specification?

Well, `Task` can be seen as an `org.reactivestreams.Publisher` that
emits exactly one event upon subscription and then stops. And we can
convert any `Task` to such a publisher directly:

```tut:silent
val task = Task.eval(Random.nextInt())

val publisher: org.reactivestreams.Publisher[Int] =
  task.toReactivePublisher
```

This is meant for interoperability purposes with other libraries, but
if you're inclined to use it directly, it's a little lower level,
but doable:

```tut:silent
import org.reactivestreams._

publisher.subscribe(new Subscriber[Int] {
  def onSubscribe(s: Subscription): Unit =
    s.request(Long.MaxValue)

  def onNext(e: Int): Unit =
    println(s"OnNext: $e")

  def onComplete(): Unit =
    println("OnComplete")

  def onError(ex: Throwable): Unit =
    System.err.println(s"ERROR: $ex")
})

// Will print:
//=> OnNext: -228329246
//=> OnComplete
```

Awesome, isn't it?

(◑‿◐)

## Error Handling

`Task` takes error handling very seriously. You see, there's this famous
[thought experiment](https://en.wikipedia.org/wiki/If_a_tree_falls_in_a_forest)
regarding *observation*:

> "*If a tree falls in a forest and no one is around to hear it, does
> it make a sound?*"

Now this applies very well to error handling, because if an error is
triggered by an asynchronous process and there's nobody to hear it, no
handler to catch it and log it or recover from it, then it didn't
happen. And what you'll get is
[nondeterminism](https://en.wikipedia.org/wiki/Nondeterministic_algorithm)
without any hints of the error involved.

This is why Monix will always attempt to catch and signal or at least
log any errors that happen. In case signaling is not possible for
whatever reason (like the callback was already called), then the
logging is done by means of the provided `Scheduler.reportFailure`,
which defaults to `System.err`, unless you provide something more
concrete, like going through SLF4J or whatever.

Even though Monix expects for the arguments given to its operators,
like `flatMap`, to be pure or at least protected from errors, it still
catches errors, signaling them on `runAsync`:

```tut:silent
val task = Task(Random.nextInt).flatMap {
  case even if even % 2 == 0 =>
    Task.now(even)
  case odd =>
    throw new IllegalStateException(odd.toString)
}

task.runAsync(r => println(r))
//=> Success(-924040280)

task.runAsync(r => println(r))
//=> Failure(java.lang.IllegalStateException: 834919637)
```

In case an error happens in the callback provided to `runAsync`, then
Monix can no longer signal an `onError`, because it would be a
contract violation (see [Callback](./callback.html)). But it still
logs the error:

```tut:silent
import scala.concurrent.duration._

// Ensures asynchronous execution, just to show
// that the action doesn't happen on the
// current thread
val task = Task(2).delayExecution(1.second)

task.runAsync { r =>
  throw new IllegalStateException(r.toString)
}

// After 1 second, this will log the whole stack trace:
//=> java.lang.IllegalStateException: Success(2)
//=>    ...
//=>	at monix.eval.Task$$anon$3.onSuccess(Task.scala:78)
//=>	at monix.eval.Callback$SafeCallback.onSuccess(Callback.scala:66)
//=>	at monix.eval.Task$.trampoline$1(Task.scala:1248)
//=>	at monix.eval.Task$.monix$eval$Task$$resume(Task.scala:1304)
//=>	at monix.eval.Task$AsyncStateRunnable$$anon$20.onSuccess(Task.scala:1432)
//=>    ....
```

Similarly, when using `Task.create`, Monix attempts to catch any
uncaught errors, but because we did not know what happened in the
provided callback, we cannot signal the error as it would be a
contract violation (see [Callback](./callback.html)), but Monix does
log the error:

```tut:silent
val task = Task.create[Int] { (scheduler, callback) =>
  throw new IllegalStateException("FTW!")
}

val future = task.runAsync

// Logs the following to System.err:
//=> java.lang.IllegalStateException: FTW!
//=>    ...
//=> 	at monix.eval.Task$$anonfun$create$1.apply(Task.scala:576)
//=> 	at monix.eval.Task$$anonfun$create$1.apply(Task.scala:571)
//=> 	at monix.eval.Task$AsyncStateRunnable.run(Task.scala:1429)
//=>    ...

// The Future NEVER COMPLETES, OOPS!
future.onComplete(r => println(r))
```

**WARNING:** In this case the consumer side never gets a completion
signal. The moral of the story is: even if Monix makes a best effort
to do the right thing, you should protect your freaking code against
unwanted exceptions, especially in `Task.create`!!!

### Overriding the Error Logging

The article on [Scheduler](../execution/scheduler.html) has recipes
for building your own `Scheduler` instances, with your own logic. But
here's a quick snippet for building such a `Scheduler` that could do
logging by means of a library, such as the standard
[SLF4J](http://www.slf4j.org/):

```tut:silent
import monix.execution.Scheduler
import monix.execution.Scheduler.{global => default}
import monix.execution.UncaughtExceptionReporter
import org.slf4j.LoggerFactory

val reporter = UncaughtExceptionReporter { ex =>
  val logger = LoggerFactory.getLogger("monix")
  logger.error("Uncaught exception", ex)
}

implicit val global: Scheduler =
  Scheduler(default, reporter)
```

### Trigger a Timeout

In case a `Task` is too slow to execute, we can cancel it and trigger
a `TimeoutException` using `Task.timeout`:

```tut:silent
import scala.concurrent.duration._
import scala.concurrent.TimeoutException

val source =
  Task("Hello!").delayExecution(10.seconds)

// Triggers error if the source does not
// complete in 3 seconds after runAsync
val timedOut = source.timeout(3.seconds)

timedOut.runAsync(r => println(r))
//=> Failure(TimeoutException)
```

On timeout the source gets canceled (if it's a source that supports
cancelation). And instead of an error, we can timeout to a `fallback`
task. The following example is equivalent to the above one:

```tut:silent
import scala.concurrent.duration._
import scala.concurrent.TimeoutException

val source =
  Task("Hello!").delayExecution(10.seconds)

val timedOut = source.timeoutTo(
  3.seconds,
  Task.raiseError(new TimeoutException)
)

timedOut.runAsync(r => println(r))
//=> Failure(TimeoutException)
```

### Recovering from Error

`Task.onErrorHandleWith` is an operation that takes a function mapping
possible exceptions to a desired fallback outcome, so we could do
this:

```tut:silent
import scala.concurrent.duration._
import scala.concurrent.TimeoutException

val source = {
  Task("Hello!")
    .delayExecution(10.seconds)
    .timeout(3.seconds)
}

val recovered = source.onErrorHandleWith {
  case _: TimeoutException =>
    // Oh, we know about timeouts, recover it
    Task.now("Recovered!")
  case other =>
    // We have no idea what happened, raise error!
    Task.raiseError(other)
}

recovered.runAsync.foreach(println)
//=> Recovered!
```

There's also `Task.onErrorRecoverWith` that takes a partial function
instead, so we can omit the "other" branch:

```tut:silent
val recovered = source.onErrorRecoverWith {
  case _: TimeoutException =>
    // Oh, we know about timeouts, recover it
    Task.now("Recovered!")
}

recovered.runAsync.foreach(println)
//=> Recovered!
```

`Task.onErrorHandleWith` and `Task.onErrorRecoverWith` are the
equivalent of `flatMap`, only for errors. In case we know or can
evaluate a fallback result eagerly, we could use the shortcut
operation `Task.onErrorHandle` like:

```tut:silent
val recovered = source.onErrorHandle {
  case _: TimeoutException =>
    // Oh, we know about timeouts, recover it
    "Recovered!"
  case other =>
    throw other // Rethrowing
}
```

Or the partial function version with `onErrorRecover`:

```tut:silent
val recovered = source.onErrorRecover {
  case _: TimeoutException =>
    // Oh, we know about timeouts, recover it
    "Recovered!"
}
```

### Restart On Error

The `Task` type, being just a specification, it can usually restart
whatever process is supposed to deliver the final result and we can
restart the source on error, for how many times are needed:

```tut:silent
import scala.util.Random

val source = Task(Random.nextInt).flatMap {
  case even if even % 2 == 0 =>
    Task.now(even)
  case other =>
    Task.raiseError(new IllegalStateException(other.toString))
}

// Will retry 4 times for a random even number,
// or fail if the maxRetries is reached!
val randomEven = source.onErrorRestart(maxRetries = 4)
```

We can also restart with a given predicate:

```tut:silent
import scala.util.Random

val source = Task(Random.nextInt).flatMap {
  case even if even % 2 == 0 =>
    Task.now(even)
  case other =>
    Task.raiseError(new IllegalStateException(other.toString))
}

// Will keep retrying for as long as the source fails
// with an IllegalStateException
val randomEven = source.onErrorRestartIf {
  case _: IllegalStateException => true
  case _ => false
}
```

Or we could implement our own retry with exponential backoff, because
it's cool doing so:

```tut:silent
def retryBackoff[A](source: Task[A],
  maxRetries: Int, firstDelay: FiniteDuration): Task[A] = {

  source.onErrorHandleWith {
    case ex: Exception =>
      if (maxRetries > 0)
        // Recursive call, it's OK as Monix is stack-safe
        retryBackoff(source, maxRetries-1, firstDelay*2)
          .delayExecution(firstDelay)
      else
        Task.raiseError(ex)
  }
}
```

### Expose Errors

The `Task` monadic context is hiding errors that happen, much like
Scala's `Try` or `Future`. But sometimes we want to expose those
errors such that we can recover more efficiently:

```tut:silent
import scala.util.{Try, Success, Failure}

val source = Task.raiseError[Int](new IllegalStateException)
val materialized: Task[Try[Int]] =
  source.materialize

// Now we can flatMap over both success and failure:
val recovered = materialized.flatMap {
  case Success(value) => Task.now(value)
  case Failure(_) => Task.now(0)
}

recovered.runAsync.foreach(println)
//=> 0
```

There's also the reverse of materialize, which is `Task.dematerialize`:

```tut:silent
import scala.util.Try

val source = Task.raiseError[Int](new IllegalStateException)

// Exposing errors
val materialized = source.materialize
// materialize: Task[Try[Int]] = ???

// Hiding errors again
val dematerialized = materialized.dematerialize
// dematerialized: Task[Int] = ???
```

We can also convert any `Task` into a `Task[Throwable]` that will
expose any errors that happen and will also terminate with an
`NoSuchElementException` in case the source completes with success:

```tut:silent
val source = Task.raiseError[Int](new IllegalStateException)

val throwable = source.failed
// throwable: Task[Throwable] = ???

throwable.runAsync.foreach(println)
//=> java.lang.IllegalStateException
```
