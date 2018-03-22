---
layout: docs3x
title: Iterant
type_api: monix.tail.Iterant
type_source: monix-tail/shared/src/main/scala/monix/tail/Iterant.scala
description: |
    Pull-based, compositional streaming data type that can describe lazy, possibly asynchronous events, a purely functional iterator.
    
tut:
  scala: 2.12.4
  binaryScala: "2.12"
  dependencies:
    - io.monix::monix:version3x
---

## Introduction

The `Iterant` is a data type that describes lazy, possibly asynchronous
streaming of elements using a pull-based protocol.

It is similar somewhat in spirit to Scala's own
`collection.immutable.Stream` and with Java's `Iterable`, except
that it is more composable and more flexible due to evaluation being
controlled by an `F[_]` data type that you have to supply
(like [Task](../eval/task.html), [Coeval](../eval/coeval.html)
or [IO](https://typelevel.org/cats-effect/datatypes/io.html)),
which will control the evaluation. In other words,
this `Iterant` type is capable of strict or lazy, synchronous or
asynchronous evaluation.

Thus an `Iterant` is a pure data structure that can be used for functional
programming, but that is lazily evaluated and that can also describe 
side effects.

### Quick Start

An `Iterant` resembles very much a standard Scala collection, having
many of the same operations available. 

First to get the imports out of the way:

```tut:silent
import monix.eval._
import monix.tail._
```

Example:

```tut:book
val list: List[Int] = {
  // Starts by taking all positive ints; nothing
  // bad happens because this is lazily evaluated
  Iterant[Coeval].range(0, Int.MaxValue)
    .filter(_ % 2 == 0)
    .map(_ * 2)
    .flatMap(x => Iterant[Coeval].of(x, x))
    .take(6)
    .toListL
    .value
}
```

Note this sample is powered by [Coeval](../eval/coeval.html), a data type 
that describes lazy, but synchronous computations, so we can get a Scala
`List` out of this `Iterant`.

### Constrained Parametric Polymorphism

The `Iterant` type accepts as type parameter an `F` monadic type
that is used to control how evaluation happens. For example you can
use [Task](../eval/task.html), in which case the streaming can have
asynchronous behavior, or you can use [Coeval](../eval/coeval.html),
in which case it can behave like a normal, synchronous `Iterable`.

As restriction, this `F[_]` type used should be stack safe in
`map` and `flatMap`, otherwise you might get stack-overflow
exceptions. This is why in general the type class required
for `F` in its various operations is 
[cats.effect.Sync](https://typelevel.org/cats-effect/typeclasses/sync.html), but
this depends on specific requirements of the operation described.
So no need to worry about it, because the compiler will trigger errors
in case your chosen `F` is not suitable for a specific operation.

When building instances, type `F[_]` which handles the evaluation
needs to be specified upfront. Example:

```tut:silent
import cats.effect.IO
import monix.eval.{Task, Coeval}
import monix.tail.Iterant

// Builds an Iterant powered by Monix's Task
Iterant[Task].of(1, 2, 3)

// Builds an Iterant powered by Monix's Coeval
Iterant[Coeval].of(1, 2, 3)

// Builds an Iterant powered by Cats's IO
Iterant[IO].of(1, 2, 3)
```

NOTE: see details below on this syntactic sugar for builders!

We call this `F` our "_effect type_".

You'll usually pick between `Task`, `Coeval` or `IO` for your
needs, although obviously you can also work with various
monad transformers (e.g. `EitherT`).

### Finite State Machine

The `Iterant` is a pure data structure, an ADT, whose encoding describes a 
state machine.

But first, lets keep in mind the encoding of Scala's `List`, which is 
actually a stack, so lets describe it:

```tut:silent
sealed trait MyList[+A]

case object Empty extends MyList[Nothing]
case class Cons[+A](head: A, tail: MyList[A])
```

This describes a simple
[finite-state machine](https://en.wikipedia.org/wiki/Finite-state_machine) (FSM):

<img src="{{ site.baseurl }}public/images/docs/list.png" width="400" class="border padded max" />

`Iterant` is very similar with `List`, however we need:

1. lazy or async processing, so the equivalent of "tail" needs to defer to the "effect type"
2. ability to suspend execution (via the provided effect type)
3. ability to stop the processing early and release any resources before reaching the end of the stream
4. ability signal an error if it happens, `Iterant` implementing `MonadError`
5. ability to signal events in batches, as an optimization

So the state machine described by `Iterant` is made of:

- [Next]({{ site.api3x }}monix/tail/Iterant$$Next.html) which signals a single strict element, 
  an `item` and a `rest` representing the rest of the stream
- [NextBatch]({{ site.api3x }}monix/tail/Iterant$$NextBatch.html)
  is a variation on `Next` for signaling a whole batch of elements by means of a
  `Batch`, a type that's similar with Scala's `Iterable`, 
  along with the `rest` of the stream
  (note this goes hand in hand with `NextCursor`)
- [NextCursor]({{ site.api3x }}monix/tail/Iterant$$NextCursor.html)
  is a variation on `Next` for signaling a whole strict batch of elements 
  as a traversable `BatchCursor`, a type that's similar
  with Scala's `Iterator`, along with the `rest` of the stream
  (note this goes hand in hand with `NextBatch`)
- [Suspend]({{ site.api3x }}monix/tail/Iterant$$Suspend.html)
  is for suspending the evaluation of a stream
- [Halt]({{ site.api3x }}monix/tail/Iterant$$Halt.html)
  signaling the end, either in success or in error
- [Last]({{ site.api3x }}monix/tail/Iterant$$Last.html)
  represents a one-element, where `Last(item)` as an optimisation on
  `Next(item, F.pure(Halt(None)), F.unit)`.

Consumption of an `Iterant` happens typically in a loop where the current step represents 
either a signal that the stream is over, or a `(item(s), rest, stop)` tuple, very similar
in spirit to Scala's standard `List` or `Iterator`.

Here's the diagram of this FSM:

<img src="{{ site.baseurl }}public/images/docs/iterant.png" width="500" class="border padded max" />

"_Early Stop_" is a special state as it is not represented by `Iterant`'s ADT definition, but
it happens whenever the "_stop_" reference is followed, instead of "_rest_". 

### Interruption, the "Early Stop"

So the protocol, as described by Iterant's data constructors, is that when 
processing an `Iterant` in a loop, the users are given a choice:

1. follow `rest`, in which case the next element or batch of elements will get delivered, or
2. follow `stop`, in which case the early interruption logic will get triggered

This ensures that any resources get released.
Consider this example that builds a stream out of lines from a given text file:

```tut:silent
import java.io._

def readLines(file: File): Iterant[Coeval, String] =
  Iterant[Coeval].suspend {
    def loop(in: BufferedReader): Coeval[Iterant[Coeval, String]] =
      Coeval {
        // For resource cleanup
        val stop = Coeval(in.close())
        in.readLine() match {
          case null =>
            Iterant.suspendS(stop.map(_ => Iterant.empty), stop)
          case line =>
            Iterant.nextS(line, loop(in), stop)
        }
      }

    val in = Coeval {
      // Freaking Java
      new BufferedReader(
        new InputStreamReader(new FileInputStream("file"), 
        "utf-8"))
    }
    // Go, go, go
    in.flatMap(loop)
  }
```

Now consider what would happen in an example like this:

```scala
readLines(new File("big-file.txt")).take(10)
```

Such a `take` operation will take only the first lines and ignore the rest.
But the underlying implementation has to close the file handle regardless of
our decision to not read the file until EOF.

This is usually a big gotcha when using Scala's or Java's `Iterator`, but 
not with `Iterant`. With `Iterant` we can specify logic to be triggered in
case of an early interruption, an "early stop" as we call it, releasing
any resources early.

## Building Iterants

### Syntactic Sugar for Builders

As an example the normal operation to build an `Iterant` out of a sequence
of elements would be this:

```tut:silent
Iterant.fromSeq[IO, Int](Seq(1, 2, 3))
```

And note that the type parameters need to be explicit, as otherwise
the compiler doesn't know what "effect type" you want to use and it will
either yield an error or an unexpected type:

```tut:book
Iterant.fromSeq(Seq(1, 2, 3))
```

The [Iterant companion object]({{ site.api3x }}monix/tail/Iterant$.html)
has a little helper described via its `apply` for doing "currying" of the
"effect type" parameter. So it allows you to do this:

```tut:silent
Iterant[Task].fromSeq(Seq(1, 2, 3))

Iterant[Coeval].fromSeq(Seq(1, 2, 3))

Iterant[IO].fromSeq(Seq(1, 2, 3))
```

Note how we need to specify only the "effect type", but not `Int`, leaving
that to the compiler to infer it. So when you see a construct like this:

```tut:silent
Iterant[Task].pure(1)
```

Don't worry, that's just `Iterant.apply` doing its magic, being equivalent to:

```tut:silent
Iterant.pure[Task, Int](1)
```
