---
layout: docs
title: Coeval
type_api: monix.eval.Coeval
type_source: monix-eval/shared/src/main/scala/monix/eval/Coeval.scala
description: |
  A data type for controlling immediate (synchronous), possibly lazy evaluation, or for controlling side-effects, the sidekick of Task.
  
tut:
  scala: 2.11.8
  binaryScala: "2.11"
  dependencies:
    - io.monix::monix-eval:version2x
---

## Introduction

Coeval is a data type for controlling synchronous, possibly lazy
evaluation, useful for describing lazy expressions and for controlling
side-effects. It is the sidekick of [Task](./task.html), being meant
for computations that are guaranteed to execute immediately
(synchronously).

Vocabulary definition:

> 1) *Having the same age or date of origin; contemporary*
>
> 2) *Something of the same era*
>
> 3) *Synchronous*

Yes, the name was chosen because it is sort of a synonym for
synchronous, though it must be admitted it's also because of a
fascination of FP developers for co-things ♥◡♥

Sample:

```tut:silent
import monix.eval.Coeval

val coeval = Coeval {
  println("Effect!")
  "Hello!"
}

// Coeval has lazy behavior, so nothing
// happens until being evaluated:
coeval.value
//=> Effect!
// res1: String = Hello!

// And we can handle errors explicitly:
import scala.util.{Success, Failure}

coeval.runTry match {
 case Success(value) =>
   println(value)
 case Failure(ex) =>
   System.err.println(ex)
}
```

### Design Summary

In summary the Monix `Coeval`:

- resembles [Task](./task.html), but works only for immediate,
  synchronous evaluation
- can be a replacement for `lazy val` and by-name parameters
- doesn’t trigger the execution, or any effects until `value` or `run`
- allows for controlling of side-effects
- handles errors

A visual representation of where `Coeval` sits in the design space:

|                    |        Eager        |           Lazy           |
|:------------------:|:-------------------:|:------------------------:|
| **Synchronous**    |          A          |          () => A         |
|                    |                     |         Coeval[A]        |
| **Asynchronous**   | (A => Unit) => Unit |    (A => Unit) => Unit   |
|                    |      Future[A]      |   [Task[A]](./task.html) |

So what problems are we solving?

- `lazy val` cannot be expressed by developers as a type, you cannot
  take a `lazy val` parameter or return a `lazy val` from a function
- ditto for by-name parameters, being just syntactic sugar that the
  compiler understand, but a proper type isn't properly exposed by
  Scala
- Scala has `@tailrec` as a compiler workaround to the JVM not
  supporting tail-calls elimination, but it only works for
  mutually tail recursive calls and thus limited
- The `scala.util.Try` type is overlapping in scope, given the
  `Coeval` focus on error handling, but doesn't have lazy behavior

`Coeval` can replace both `lazy val` and by-name parameters, allowing
one to control evaluation and to do error handling. It's also stack
safe and with it you can describe mutually tail-recursive algorithms,
which are incredibly important in FP.

### Comparison with Cats Eval

The whole Monix library stands on the shoulders of giants and `Coeval`
is definitely inspired by the `Eval` data-type in
[Cats](http://typelevel.org/cats/), hence credit should be given where
credit is due.

The Cats `Eval` is a very light type that's concerned just with
controlling evaluation. It's more limited and that's not a bad
thing. People using `Eval` are not using it as a replacement for an
[I/O Monad](https://apocalisp.wordpress.com/2011/12/19/towards-an-effect-system-in-scala-part-2-io-monad/).

But the Monix `Coeval` works as a side-kick to `Task`, being for those
instances where you don't want the asynchronous nature of `Task`. This
means that `Coeval` scales from delaying simple arithmetic operations
up to controlling side-effects, and if you want, it can also function
as a replacement for an I/O Monad. And because it's the legitimate
sibling of [Task](./task.html), conversion back and forth is smooth
(within limits).

Or in more concrete terms, at the moment of writing this, the Monix
`Coeval` takes care of error handling, while the Cats `Eval` does not,
providing operations for recovery, thus also working well as a
replacement for Scala's `Try` type.

## Evaluation

To evaluate a `Coeval` instance you can invoke its `value` command:

```tut:silent
val coeval = Coeval {
  println("Effect!")
  1 + 1
}

// Nothing happens until this point:
coeval.value
//=> Effect!
// res: Int = 2
```

But `value` might trigger exceptions, if somewhere in the evaluation
chain exceptions have happened. Instead of `value` we can expose
errors by means of `runTry`:

```tut:silent
import scala.util.{Failure, Success}

val coeval = Coeval[Int] {
  throw new RuntimeException("Hello!")
}

coeval.runTry match {
  case Success(value) =>
    println(s"Success: $value")
  case Failure(ex) =>
    println(s"Error: $ex")
}

// Will print:
//=> Error: java.lang.RuntimeException: Hello!
```

### Attempt, the replacement for scala.util.Try

The `runTry` method returns a `scala.util.Try`, but if you looked at
the source code, the implementation of `Coeval` uses two states called
`Now(value)` and `Error(ex)`, inheriting from `Coeval` and that are
perfect equivalents for the `scala.util.Try` states called `Success`
and `Failure`. And in fact an `Attempt` sub-type of `Coeval` is
exposed as an ADT that you can use instead of `scala.util.Try`:

```tut:silent
import monix.eval.Coeval
import monix.eval.Coeval.{Attempt, Now, Error}

val coeval1 = Coeval(1 + 1)

val result1: Attempt[Int] = coeval1.runAttempt
// result1 = Now(2)

val coeval2 = Coeval.raiseError[Int](new RuntimeException("Hello!"))

val result2: Attempt[Int] = coeval2.runAttempt
// result = Error(java.lang.RuntimeException: Hello!)
```

Hence the `Coeval` type, or more precisely `Coeval.Attempt`, can work
as a replacement for `scala.util.Try`, although note that even if the
values boxed by `Now` and `Error` are already evaluated, when invoking
operators on them, like `flatMap`, the behavior is still lazy, which
is the main difference between `Attempt` and `Try`.

### Convert any Coeval into a Task

For converting any `Coeval` into a [Task](./task.html):

```tut:silent
val coeval = Coeval.eval(1 + 1)

val task = coeval.task
// task: Task[Int] = Always(<function0>)
```

`Task` and `Coeval` being siblings, they have similar internal states
and conversion from a `Coeval` into a `Task` is direct and efficient.

## Builders

`Coeval` can replace functions accepting zero arguments, Scala by-name
params, `lazy val` or `scala.util.Try`. Here's how you can build
instances:

### Coeval.now

`Coeval.now` lifts an already known value in the `Coeval` context,
the equivalent of `Applicative.pure`:

```tut:silent
import monix.eval.Coeval

val coeval = Coeval.now { println("Effect"); "Hello!" }
//=> Effect
// coeval: monix.eval.Coeval[String] = Now(Hello!)
```

### Coeval.eval

`Coeval.eval` is the equivalent of `Function0`, taking a
function that will always be evaluated on invocation of `value`:

```tut:silent
val coeval = Coeval.eval { println("Effect"); "Hello!" }
// coeval: monix.eval.Coeval[String] = Once(<function0>)

coeval.value
//=> Effect
//=> Hello!

// The evaluation (and thus all contained side effects)
// gets triggered every time
coeval.value
//=> Effect
//=> Hello!
```

### Coeval.evalOnce

`Coeval.evalOnce` is the equivalent of a `lazy val`, a type that cannot
be precisely expressed in Scala. The `evalOnce` builder does
memoization on the first run, such that the result of the evaluation
will be available for subsequent runs. It also has guaranteed
idempotency and thread-safety:

```tut:silent
val coeval = Coeval.evalOnce { println("Effect"); "Hello!" }
// coeval: monix.eval.Coeval[String] = Once(<function0>)

coeval.value
//=> Effect
//=> Hello!

// Result was memoized on the first run!
coeval.value
//=> Hello!
```

### Coeval.defer

`Coeval.defer` is about building a factory of coevals. For example
this will behave approximately like `Coeval.eval`:

```tut:silent
val coeval = Coeval.defer {
  Coeval.now { println("Effect"); "Hello!" }
}
// coeval: monix.eval.Coeval[String] = Suspend(<function0>)

coeval.value
//=> Effect
//=> Hello!

coeval.value
//=> Effect
//=> Hello!
```

### Coeval.raiseError

`Coeval.raiseError` can lift errors in the monadic context of `Coeval`:

```tut:silent
val error = Coeval.raiseError[Int](new IllegalStateException)
// error: monix.eval.Coeval[Int] =
//   Error(java.util.concurrent.TimeoutException)

error.runTry
//=> Failure(java.lang.IllegalStateException)
```

### Coeval.unit

`Coeval.unit` is returning an already completed `Coeval[Unit]` instance,
provided as an utility, to spare you creating new instances with
`Coeval.now(())`:

```tut:silent
val coeval = Coeval.unit
// coeval: monix.eval.Coeval[Unit] = Now(())
```

This instance is shared, so that can relieve some stress from the
garbage collector.

## Memoization

The
[Coeval#memoize]({{ site.api2x }}#monix.eval.Coeval@memoize:monix.eval.Coeval[A])
operator can take any `Coeval` and apply memoization on the first evaluation
(such as `value`, `runTry`) such that:

1. you have guaranteed idempotency, calling `value` multiple times
   will have the same effect as calling it once
2. subsequent evaluations will reuse the result computed by the
   first evaluation

So `memoize` effectively caches the result of the first `value` or
`runTry` call. In fact we can say that:

```scala
Coeval.evalOnce(f) <-> Coeval.eval(f).memoize
```

They are effectively the same. And at the moment of writing, the
implementation of `memoize` actually pattern matches on the source to
see if we are dealing with an `Always` transforming it into an
`Once`. You shouldn't rely on this behavior, but this gives you an
idea of the properties involved: for the layman, you can say that
`memoize` turns your `Coeval` into a `lazy val`.

And `memoize` works with any coeval reference:

```tut:silent
import monix.eval.Coeval

// Has async execution, to do the .apply semantics
val coeval = Coeval { println("Effect"); "Hello!" }

val memoized = coeval.memoize

memoized.value
//=> Effect
//=> Hello!

memoized.value
//=> Hello!
```

## Operations

### FlatMap and Tail-Recursive Loops

So lets start with a stupid example that calculates the N-th number in
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
`Coeval`, one possible implementation would be:

```tut:silent
def fib(cycles: Int, a: BigInt, b: BigInt): Coeval[BigInt] = {
  if (cycles > 0)
    Coeval.defer(fib(cycles-1, b, a+b))
  else
    Coeval.now(b)
}
```

And now there are already differences. This is lazy, as the N-th
Fibonacci number won't get calculated until we evaluate it. The
`@tailrec` annotation is also not needed, as this is stack (and heap)
safe.

`Coeval` has `flatMap`, which is the monadic `bind` operation, that
for things like `Coeval`, `Task` or `Future` is the operation that
describes recursivity or that forces ordering (e.g. execute this, then
that, then that). And we can use it to describe recursive calls:

```tut:silent
def fib(cycles: Int, a: BigInt, b: BigInt): Coeval[BigInt] =
  Coeval.eval(cycles > 0).flatMap {
    case true =>
      fib(cycles-1, b, a+b)
    case false =>
      Coeval.now(b)
  }
```

Again, this is stack safe and uses a constant amount of memory, so no
`@tailrec` annotation is needed or wanted. And it has lazy behavior,
as nothing will get triggered until evaluation happens.

But we can also have **mutually tail-recursive calls**, w00t!

```tut:silent
// Mutual Tail Recursion, ftw!!!
{
  def odd(n: Int): Coeval[Boolean] =
    Coeval.eval(n == 0).flatMap {
      case true => Coeval.now(false)
      case false => even(n - 1)
    }

  def even(n: Int): Coeval[Boolean] =
    Coeval.eval(n == 0).flatMap {
      case true => Coeval.now(true)
      case false => odd(n - 1)
    }

  even(1000000)
}
```

Again, this is stack safe and uses a constant amount of memory.

### The Applicative: zip2, zip3, ... zip6

When using `flatMap`, we often end up with this:

```tut:silent
val locationTask: Coeval[String] = Coeval.eval(???)
val phoneTask: Coeval[String] = Coeval.eval(???)
val addressTask: Coeval[String] = Coeval.eval(???)

// Ordered operations based on flatMap ...
val aggregate = for {
  location <- locationTask
  phone <- phoneTask
  address <- addressTask
} yield {
  "Gotcha!"
}
```

This gets transformed by the compiler into a batch of `flatMap` calls.
But `Coeval` is also an `Applicative` and hence it has utilities, such
as `zip2`, `zip3`, up until `zip6` (at the moment of writing) and also
`zipList`. The example above could be written as:

```tut:silent
val locationCoeval: Coeval[String] = Coeval.eval(???)
val phoneCoeval: Coeval[String] = Coeval.eval(???)
val addressCoeval: Coeval[String] = Coeval.eval(???)

val aggregate =
  Coeval.zip3(locationCoeval, phoneCoeval, addressCoeval).map {
    case (location, phone, address) => "Gotcha!"
  }
```

In order to avoid boxing into tuples, you can also use `zipMap2`,
`zipMap3` ... `zip6`:

```tut:silent
Coeval.zipMap3(locationCoeval, phoneCoeval, addressCoeval) { 
  (location, phone, address) => "Gotcha!"
}
```

### Gather results from a Seq of Coevals

`Coeval.sequence`, takes a `Seq[Coeval[A]]` and returns a
`Coeval[Seq[A]]`, thus transforming any sequence of coevals into a
coeval with a sequence of results.

```tut:silent
val ca = Coeval(1)
val cb = Coeval(2)

val list: Coeval[Seq[Int]] =
  Coeval.sequence(Seq(ca, cb))

list.value
//=> List(1, 2)
```

The results are ordered in the order of the initial sequence.

### Restart Until Predicate is True

The `Coeval` being a spec, we can restart it at will.  And
`restartUntil(predicate)` does that, executing the source over and
over again, until the given predicate is true:

```tut:silent
import scala.util.Random

val randomEven = {
  Coeval.eval(Random.nextInt())
    .restartUntil(_ % 2 == 0)
}

randomEven.value
//=> -2097793116
randomEven.value
//=> 1246761488
randomEven.value
//=> 1053678416
```

### Clean-up Resources on Finish

`Coeval.doOnFinish` executes the supplied
`Option[Throwable] => Coeval[Unit]` function when the source finishes,
being meant for cleaning up resources or executing
some scheduled side-effect:

```tut:silent
val coeval = Coeval(1)

val withFinishCb = coeval.doOnFinish {
  case None =>
    println("Was success!")
    Coeval.unit
  case Some(ex) =>
    println(s"Had failure: $ex")
    Coeval.unit
}

withFinishCb.value
//=> Was success!
// res: Int = 1
```

## Error Handling

`Coeval` does error handling. Being the side-kick of `Task` means it
gets mostly the same facilities for recovering from error.

First off, even though Monix expects for the arguments given to its
operators, like `flatMap`, to be pure or at least protected from
errors, it still catches errors, signaling them on `runTry` or
`runAttempt`:

```tut:silent
import monix.eval.Coeval
import scala.util.Random

val coeval = Coeval(Random.nextInt).flatMap {
  case even if even % 2 == 0 =>
    Coeval.now(even)
  case odd =>
    throw new IllegalStateException(odd.toString)
}

coeval.runTry
// res1: Try[Int] = Success(624170708)

coeval.runTry
// res2: Try[Int] = Failure(IllegalStateException: -814066173)
```

### Recovering from Error

`Coeval.onErrorHandleWith` is an operation that takes a function mapping
possible exceptions to a desired fallback outcome, so we could do
this:

```tut:silent
import scala.concurrent.duration._
import scala.concurrent.TimeoutException

val source = Coeval.raiseError[String](new IllegalStateException)

val recovered = source.onErrorHandleWith {
  case _: IllegalStateException =>
    // Oh, we know about illegal states, recover it
    Coeval.now("Recovered!")
  case other =>
    // We have no idea what happened, raise error!
    Coeval.raiseError(other)
}

recovered.runTry
// res1: Try[String] = Success(Recovered!)
```

There's also `Coeval.onErrorRecoverWith` that takes a partial function
instead, so we can omit the "other" branch:

```tut:silent
val recovered = source.onErrorRecoverWith {
  case _: IllegalStateException =>
    // Oh, we know about illegal states, recover it
    Coeval.now("Recovered!")
}

recovered.runTry
// res: Try[String] = Success(Recovered!)
```

`Coeval.onErrorHandleWith` and `Coeval.onErrorRecoverWith` are the
equivalent of `flatMap`, only for errors. In case we know or can
evaluate a fallback result eagerly, we could use the shortcut
operation `Coeval.onErrorHandle` like:

```tut:silent
val recovered = source.onErrorHandle {
  case _: IllegalStateException =>
    // Oh, we know about illegal states, recover it
    "Recovered!"
  case other =>
    throw other // Rethrowing
}
```

Or the partial function version with `onErrorRecover`:

```tut:silent
val recovered = source.onErrorRecover {
  case _: IllegalStateException =>
    // Oh, we know about illegal states, recover it
    "Recovered!"
}
```

### Restart On Error

The `Coeval` type, being just a specification, it can usually restart
whatever process is supposed to deliver the final result and we can
restart the source on error, for how many times are needed:

```tut:silent
import scala.util.Random

val source = Coeval(Random.nextInt).flatMap {
  case even if even % 2 == 0 =>
    Coeval.now(even)
  case other =>
    Coeval.raiseError(new IllegalStateException(other.toString))
}

// Will retry 4 times for a random even number,
// or fail if the maxRetries is reached!
val randomEven = source.onErrorRestart(maxRetries = 4)
```

We can also restart with a given predicate:

```tut:silent
import scala.util.Random

val source = Coeval(Random.nextInt).flatMap {
  case even if even % 2 == 0 =>
    Coeval.now(even)
  case other =>
    Coeval.raiseError(new IllegalStateException(other.toString))
}

// Will keep retrying for as long as the source fails
// with an IllegalStateException
val randomEven = source.onErrorRestartIf {
  case _: IllegalStateException => true
  case _ => false
}
```

### Expose Errors

The `Coeval` monadic context is hiding errors that happen, much like
Scala's `Try` or `Future`. But sometimes we want to expose those
errors such that we can recover more efficiently:

```tut:silent
import scala.util.{Try, Success, Failure}

val source = Coeval.raiseError[Int](new IllegalStateException)
val materialized: Coeval[Try[Int]] =
  coeval.materialize

// Now we can flatMap over both success and failure:
val recovered = materialized.flatMap {
  case Success(value) => Coeval.now(value)
  case Failure(_) => Coeval.now(0)
}

recovered.value
// res: Int = 0
```

There's also the reverse of materialize, which is
`Coeval.dematerialize`:

```tut:silent
import scala.util.Try

val source = Coeval.raiseError[Int](new IllegalStateException)

// Exposing errors
val materialized = coeval.materialize
// materialize: Coeval[Try[Int]] = ???

// Hiding errors again
val dematerialized = materialized.dematerialize
// dematerialized: Coeval[Int] = ???
```

We can also convert any `Coeval` into a `Coeval[Throwable]` that will
expose any errors that happen and will also terminate with an
`NoSuchElementException` in case the source completes with success:

```tut:silent
val source = Coeval.raiseError[Int](new IllegalStateException)

val throwable = source.failed
// throwable: Coeval[Throwable] = ???

throwable.runTry
// res: Try[Throwable] = Success(java.lang.IllegalStateException)
```
