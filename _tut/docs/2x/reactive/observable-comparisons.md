---
layout: docs
title: "Comparisons with Other Solutions"
type_api: monix.reactive.Observable
type_source: monix-reactive/shared/src/main/scala/monix/reactive/Observable.scala
description: |
  Comparing the Monix Observable with Akka Actors, Akka Streams and FS2.
---

Comparing the Monix `Observable` with other solutions is no easy task.
Yet we can try.

**WARNING:** This document contains opinions which are highly
subjective! You won't find unbiased comparisons from the authors of
libraries. So take this with a grain of salt.

## Akka Actors

[Akka actors](http://akka.io/) are a sort of a *de
facto* solution for modeling message passing with Scala and the JVM.
What they are good at:

- they make bidirectional communications over asynchronous boundaries
  easy - for example WebSocket is a prime candidate for actors
- with Akka's Actors you can easily model state machines (see `context.become`)
- the processing of messages has a strong concurrency guarantee -
  messages are processed one by one, so there's no need to worry about
  RAM concurrency issues while in the context of an actor  
- instead of implementing a half-assed in-memory queue for processing
  of things, you could just use an actor, since queuing of messages
  and acting on those messages is what they do
  
But Akka actors aren't suitable in many cases because:

- they are fairly low-level - the high-level protocol of communication
  between actors is still something that you need to design, so for
  example if you want things like back-pressure, you have to implement
  it yourself
- it's extremely easy to model actors that keep a lot of state, ending
  up with a system that can't be horizontally scaled
- because of the bidirectional communications capability, it's
  extremely easy to end up with data flows that are so complex as to
  be unmanageable
- the model in general is actor A sending a message to actor B - but
  if you need to model a stream of events, this tight coupling between
  A and B is not acceptable
- actors, as used in practice with Akka, tend to be inducing
  uncontrolled side effects and that's error prone, the opposite of
  functional programming and not idiomatic Scala

By contrast the Monix `Observable`:

- easily models unidirectional communications between producers and
  consumers
- events usually flow in one direction and so you much easily
  transform and compose these streams
- they address back-pressure concerns by default
- even in case you can't pause the data source, this back-pressure
  handling is relevant because you get adjustable buffering policies
  (as in, what to do on overflow, drop events like they are hot?)
- just as in the case of `Future`, because of the limitations, the
  model is simple to use and much more reasonable and composable than
  actors, with the exposed operators being awesome
  
Reactive streaming libraries in general and the Monix `Observable` in
particular is bad because they are good for modeling unidirectional
communications, but it gets complicated if you want bidirectional
communications (possible, but complicated), with actors being better
at having dialogs.

## Akka Streams

The [Akka Streams](http://doc.akka.io/docs/akka/current/scala/stream/index.html)
project is also an implementation of 
[reactive streams](www.reactive-streams.org), but
one that is based on Akka actors and that has its own design 
and opinions.

Why Monix is better:

- Monix is easier to use
- The inner workings of Monix are easier to understand
- Monix is lighter, as it doesn't depend on an actor framework
- Monix is basically the
  [Observer pattern (from GoF)](https://en.wikipedia.org/wiki/Observer_pattern)
  on steroids and this has practical benefits
- Monix works on [Scala.js](http://www.scala-js.org/)
  and pretty soon on Scala Native as well
  
Why Akka Streams might be better, depending on your preferences and needs:

- When building data flows, Akka Streams retains the description
  (think abstract syntax trees) and by means of the injected
  `Materializer` you can get different runtimes
- Akka Streams models the sharing of streams very explicitly
- If you love Akka actors, then you might prefer Akka Streams, being
  meant as an extension of actors

HIGHLY OPINIONATED WARNING: The author of this document finds Akka
Streams to be much more complicated.

The Monix `Observable` is basically the
[Observer pattern (from GoF)](https://en.wikipedia.org/wiki/Observer_pattern)
on steroids. But the Akka Streams implementation is not that and this
has consequences of usability.

A stream of information is like a river. Does the river care who
observes it or who drinks from it? It doesn’t. And indeed, sometimes
you need to share the source between multiple listeners, sometimes you
want to create new sources for each listener. But the listener
shouldn’t care what sort of producer it has on its hands or
vice-versa. And people are really not good at reasoning about graphs.
And making those graphs explicit doesn’t make it better, it makes it
worse.

In Monix you’ve got hot observables (hot data sources shared between
an unlimited number of subscribers) and cold observables (each
subscriber gets its very own private data source). You can also
convert any cold data source into a hot one by using the `multicast`
operator, in combination with `Subjects` that dictate behavior
(e.g. `Publish`, `Behavior`, `Async` or `Replay`). And there's no
reason for the downstream subscribers to know the peculiarities of
the upstream data-source. The philosophy of Monix is that this
encapsulation is better.

In Akka Streams the sources have a "single output" port and what you
do is you build "*flow graphs*" and sinks. Akka Streams is thus all
about modeling how streams are split. They call it "*explicit
fan-out*" and it's a design choice. However this can be seen as an
encapsulation leak that makes things more complicated and sometimes it
defeats the purpose of using a library for streams manipulation in the
first place. In ReactiveX and Monix terms, this is like having
single-subscriber observable and then working with Subjects (which is
both a listener and a producer) and people that have used ReactiveX
know that working with Subjects is to be avoided, as that introduces
complexity and extra worries in the system and when you do, you
usually encapsulate it really, really well.

Akka Streams depends on Akka, the library. You suddenly worry about
having an "*Actor System*" and a `Materializer` and a
`MessageFlowTransformer`, with the tasks being executed by
actors. This is way more heavy. One reason for why Scala’s `Future`
and `ExecutionContext` are great is precisely because they model only
asynchronous computations, but are completely oblivious to how the
required asynchronous execution happens. This is why `Future` works on
top of `Scala.js` without problems.

To its credit, the Akka Streams implementation does something smart.
When you build a source and a flow, the goal has been to build a
transformation flow and pass it around as a properly typed and
inspectable object. Think abstract syntax trees (ASTs).

They have made this choice in order to encourage reuse of the actual
stream blueprints instead of creating them again for every use, and it
also nicely allows the addition of different Materializers that
translate such a blueprint into its execution engine. In other words,
it needs a `Materializer` to work, because the engine can thus
transform those blueprints for execution on something other than
actors. And if you don't want actors, it's theoretically possible to
build a `Materializer` that executes the needed tasks on top of
`RxJava` or on top of plain threads.

By comparison in the ReactiveX model (applicable to Monix as well)
building observables happens by transforming one observable into
another in functions that cannot be inspected. Which approach is
better is a matter of opinion.

For Akka Streams, in practice, the only used materializer will be the
`ActorFlowMaterializer`. And this extra flexibility has led to an
opaque implementation. With Monix on the other hand, you can reason
about what observables and subjects and subscribers are, you can
reason about their implementation, whereas the inner workings of Akka
Streams are harder to understand, confining developers using it to be
plain users.

## FS2 (the new Scalaz-Stream)

[FS2](https://github.com/functional-streams-for-scala/fs2)
is a solid library for "streaming I/O" with a flourishing ecosystem.

Where FS2 is better:

- the model of communication between producers and consumers is
  *pull-based*, sometimes making it easier to implement new operators,
  with the internals being rather elegant and minimal, in true fashion
  of functional programming
- it makes a greater effort in reasoning about side-effects by means
  of the type system, you can see that in its `Stream[F,O]`, where `O`
  is the output, but `F` represent the side-effects - Monix doesn't do
  this, because in case you want purity, it expects the side-effects
  to happen at the edges, much like the `IO` monad, and freely 
  composable

It's certainly a new and interesting approach, compatible with
functional programming ideals. Where Monix is better:

- the model of communication between producers and consumers is
  *push-based* (with back-pressure) and this makes it inherently more
  efficient
- the API exposed is clean, but the internals are more pragmatic - in
  Monix, even if some operators could be expressed in terms of other
  operators, many times we implement it from scratch using low level
  concurrency techniques, if that means we achieve a performance boost
- Monix is better for
  [Functional Reactive Programming (FRP)](https://en.wikipedia.org/wiki/Functional_reactive_programming),
  exposing operators that deal with time, along with `Behavior` and
  `Replay` subjects that can model the concept of "*reactive
  variables*"
- Monix is better for shared data-sources; even if the default is for
  observables to be cold (i.e. each subscriber gets its own data-source),
  you can easily share data-sources between multiple subscribers without
  much overhead
- Monix is better for converting push-based data-sources. Monix is
  push-based itself, whereas with `FS2` what you do is to turn their
  pull-based model into something that is push-based, obviously by
  means of a buffer. This is akin to turning Monix into a pull-based
  model - it can be done, but it's awkward and inefficient.


