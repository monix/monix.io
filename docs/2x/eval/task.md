---
layout: docs
title: Task
type_api: monix.eval.Task
type_source: monix-eval/shared/src/main/scala/monix/eval/Task.scala
description: |
  A specification for a lazy and possibly asynchronous computation, useful for avoiding nondeterminism and the callback hell effect.
---

## Introduction

`Task` is a specification for a lazy and possibly asynchronous
computation, useful for avoiding nondeterminism and the callback hell
effect.

```scala
// In order to evaluate tasks, we'll need a Scheduler
import monix.execution.Scheduler.Implicits.global
// Task and CancelableFuture are in monix.eval
import monix.eval._

// Executing a sum, which (due to the semantics of apply)
// will happen on another thread. Nothing happens here though,
// this expression is pure!
val task = Task { 1 + 1 }

// Tasks get evaluated only on runAsync!
// Here we convert it into a Future, though you can
// supply an on-complete callback instead.
val future: CancelableFuture[Int] = 
  task.runAsync

// Printing the result asynchronously
future.foreach(println)
//=> 2
```

### Comparison with Scala's Future

`Task` sounds similar with Scala's
[Future](http://docs.scala-lang.org/overviews/core/futures.html), but
has a different character and the two types as you'll see are actually
complementary. A visual representation of where they sit in the design
space:

|                    |        Eager        |           Lazy           |
|:------------------:|:-------------------:|:------------------------:|
| **Synchronous**    |          A          |          () => A         |
|                    |                     | Function0[A] / Coeval[A] |
| **Asynchronous**   | (A => Unit) => Unit |    (A => Unit) => Unit   |
|                    |      Future[A]      |          Task[A]         |

A wise man once said:

> "*A Future represents a value, detached from time*" &mdash; Viktor Klang

That's certainly a poetic notion, making one think about what values
are and how they incorporate time. But more importantly, while we
cannot say that a `Future` is a
[value](https://en.wikipedia.org/wiki/Value_(computer_science)){:target="_blank"},
we can certainly say that it's a *value-wannabe*, meaning that when
users receive a `Future` reference, they know that whatever process
that's going to evaluate it has probably already started and it might
have even finished already. This makes the behavior of Scala's
`Future` to be about *eager evaluation* and certainly its design helps
with that, if you think about how it takes that implicit execution
context whenever you call its operators, like `map` and `flatMap`.

But `Task` is different. `Task` is about lazy evaluation. Well, not
always lazy, in fact `Task` allows for fine tuning the evaluation
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
whatever you're doing.

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
   [Javascript](http://www.scala-js.org/), where "cooperative
   multi-threading" is required.
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
   dealing with asynchronous execution. This is bad for portabilty
   reasons, as this API is not supported on top of
   [Scala.js](http://www.scala-js.org/).
   
## Builders

...