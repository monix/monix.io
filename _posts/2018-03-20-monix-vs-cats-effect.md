---
layout: post
title: "Monix vs Cats-Effect"
author: alexelcu
excerpt_separator: <!--more-->
description: Monix and Cats-Effect are on a roll. How do they integrate and when to use them?
comments: true
---

I'm the author of Monix and a major contributor to 
[Cats-Effect](https://typelevel.org/cats-effect/).

This post describes the relationship between Monix and Cats-Effect,
their history and when each should be used.

<!--more-->

But to start with the most pressing concern ...

## Monix's Task vs cats.effect.IO

`cats.effect.IO` and `monix.eval.Task` are very similar, plus I ended up working on both, so design decisions I made for `Task` ended up in `IO` as well. That said `IO` is designed to be a simple, reliable, pure reference implementation, whereas `Task` is more advanced if you care about certain things.

For example `Task`'s run-loop is designed to provide certain fairness guarantees, depending on configuration. By default its run-loop does processing in batches, introducing thread forks once over a threshold. If you're using `Task` or `IO` as some sort of green threads, and you should because they are really good for that, then `Task` provides _scheduling fairness_ out of the box, whereas your logic via `IO` will have to include manual `IO.shift` calls.

```scala
// Cats-effect
def fib(n: Int, a: Long, b: Long): IO[Long] =
  IO.suspend {
    if (n <= 0) IO.pure(a) else {
      val next = fib(n - 1, b, a + b)
      if (n % 128 == 0) IO.shift *> next else next      
    }
  }

// Monix simply does it ;-)
def fib(n: Int, a: Long, b: Long): Task[Long] =
  Task.suspend {
    if (n <= 0) Task.pure(a) 
    else fib(n - 1, b, a + b)
  }
```

This I've released version `0.10` of cats-effect with the cancelable `IO`. Before this it was only Monix's `Task` that was cancelable and actually usable in concurrent scenarios (e.g. race conditions), so I imported the same underlying design that the other maintainers agreed to. That said the IO implementation is more conservative still. In both implementations the cancelability of a task has to be opt-in, however with `Task` you can opt into auto-cancelable `flatMap` loops, whereas with `IO` you need manual calls to `IO.cancelBoundary`. 

```scala
// Cats-effect
def fib(n: Int, a: Long, b: Long): IO[Long] =
  IO.suspend {
    if (n <= 0) IO.pure(a) else {
      val next = fib(n - 1, b, a + b)
      if (n % 128 == 0) IO.cancelBoundary *> next else next      
    }
  }

// Monix
def fib(n: Int, a: Long, b: Long): Task[Long] =
  Task.suspend {
    if (n <= 0) Task.pure(a) 
    else fib(n - 1, b, a + b)
  }

// Cancelability is still opt-in, but can be decided at 
// the call-site for this one:
fib(1000, 0, 1).cancelable
```

So IO's design is to be very explicit about forking or cancelability, whereas `Task` affords some smartness in it, doing the right thing out of the box, but with easy configuration options.

`Task` is also designed to interact better with the impure side and you'll have an easier time to get it adopted in hybrid projects. For example `Task#runAsync` calls return `CancelableFuture` results. And `Task.fromFuture` can recognize `CancelableFuture` too, which means that this equivalence really holds as you lose no information:

```scala
Task.deferFuture(task.runAsync) <-> task
```

With IO you have `unsafeToFuture`, but the above equivalence does not hold for `IO`. It's not the same thing, the same level of integration, because `IO` doesn't care about what happens after the "edge of the FP program".

`Task` also has a `memoize` that allows you to (lazily) cache executing tasks, but that in certain cases could break RT (since allocation of a mutable ref is a side effect), so you have to be a big boy when using it, but it's awesome if you've got interactions with the other side. See [issue #120](https://github.com/typelevel/cats-effect/issues/120) for why this won't happen for IO.

Task also requires a `Scheduler` in its `runAsync`, which gets injected everywhere internally, so you don't need an `ExecutionContext` for operations like `sleep` or `shift` or whatnot. We fixed this `ExecutionContext` dependency for `IO` recently by creating an indirection in `Timer`, but it's still there. This matters in certain scenarios - for example with `Task` you get a `Task.deferFutureAction` which gives you an `ExecutionContext` for wrapping `Future` enabled APIs, so you no longer have any need to carry around that `ExecutionContext`, which isn't possible with `IO`.

Also Monix has `TaskLocal`, which is like a `ThreadLocal`, but for `Task`. You can't implement such a thing for `IO` without modifying its run-loop, because you need to ensure that values get transported over async boundaries.

This will not be the end of it. Improvements in run-loop execution that we (the Monix developers) come up with, will make it into `Task` first and might not make it into `IO` if it is against its principles.

### What should I use?

`Task` is designed for smart control in concurrent scenarios, for fairness and for interoperability with the impure side of the project, design goals that go against `IO`'s design.  If you're a user of `monix.eval.Task`, then know that `Task` will always be the more advanced one. 

The `IO` in Cats-effect is designed for simplicity and the API follows the WYSIWYG principle.

Both have virtues. But that is why the `cats-effect` project also provides [type classes](https://typelevel.org/cats-effect/typeclasses/), such that projects that can provide polymorphic abstractions can work with both. At this point projects like Doobie, FS2, Http4s or Monix's own `Iterant` allow you to use both `Task` and `IO`.

So keep using `Task` if you already do, or look into it, because it's awesome ;-)
But if you decide to use `cats.effect.IO` instead, that's fine too.

## History of Monix

Monix started at the beginning of 2014 as a collection of concurrency tools, 
augmenting Scala's standard library, but then it evolved into providing a 
full fledged [ReactiveX](http://reactivex.io/) implementation that was
back-pressured with a protocol based on Scala's `Future` and with opinions
in order to keep an idiomatic Scala style, very unlike RxJava / RxScala. 
And note that back then, RxJava was not back-pressured and Akka Streams 
did not exist, so this was solving a very real problem for me.

In 2014 this project was named [Monifu]({{ site.api1x }}).

<a href="https://typelevel.org/"><img src="{{ site.baseurl }}public/images/typelevel.png" width="150" style="float:right;" align="right" /></a>

In 2016 something happened — it 
[joined Typelevel's incubator](https://github.com/typelevel/general/issues/4)
and its path was set by 
[this comment](https://github.com/typelevel/general/issues/4#issuecomment-165976262) 
by [Miles Sabin](https://github.com/milessabin):

> @alexandru would you be willing to explore evolving Monifu in a direction which makes a more statically enforced separation between effectful and non-effectful parts of the API? @tpolecat would that address your concerns, and would you be willing to help?
>
> More generally, do we think that activity around this would usefully feed into the design of a Cats alternative to Task/IO?

Being very enthusiastic about having my project being a member of a young and cool community, I accepted.

Then on Dec 30, due to an existential crisis that happens to me around New Year's Eve, I also [renamed the project](https://github.com/monix/monix/issues/91) to Monix. Not late after that, the first ([slow, shitty and broken]((https://github.com/monix/monix/blob/v2.0-M1/monix-async/shared/src/main/scala/monix/async/Task.scala))) version of `Task` was unleashed on the unsuspecting.

Well, from March until May, when I attended 
[flatMap(Oslo)](http://2016.flatmap.no/nedelcu.html#session), apparently 
I got my shit together and had a pretty awesome `Task` implementation on
my hands. And 
[the presentation](/presentations/2016-task-flatmap-oslo.html)
went great.

Since Monix has evolved other capabilities, now in version 3.0.0 also providing `Iterant`, an abstraction for purely functional pure based streaming. But being a modular library, the interesting thing to me is that some people only use it for `Task`, while others only use it for `Observable`, treating `Task` like RxJava's `Single`, aka an `Observable` of one event.

Monix is one of the very few libraries that happens to be at the 
intersection of two worlds: 

1. The world of Java and .NET developers that have bitten from the forbidden fruit of FP due to ReactiveX libraries, but never really making the jump
2. The world of [Cats](https://typelevel.org/cats/) developers that need abstractions for describing effects, that need `IO` monad

And I'm damn proud of it.

## History of Cats-Effect

Sometimes in Feb 2017 the situation looked like this:

1. Monix had `Task`
2. FS2 had their own `Task`
3. Scalaz 7 had its own `Task`

My `Task` implementation was cancelable, as you know. FS2's `Task`
and Scalaz's `Task` were not. The problem, especially for Cats' ecosystem 
was that we had two working `Task` implementations on our hands, but libraries 
like Http4s and Doobie, had to pick one.

I happened to attend [NEScala 2017](http://www.nescala.org/2017),
where I've heard of
[libraries standardizing on Cats](https://http4s.github.io/fp-ecosystem/)
at the expense of Scalaz. Up until this point Monix (series 2.x) had
support for both Cats and Scalaz via optional sub-projects, by 
providing _orphaned instances_. This was very far from ideal however,
pushing all of this complexity to the users, when an approach such
as [shims](https://github.com/djspiewak/shims) is far superior
for both Cats and Scalaz users.

At the same NEScala, at its "_Unconference_", I participated in a discussion 
with other members of Typelevel, to decide what to do next. I remember having
a heated argument with Daniel about cancelability. As you may or 
may not know, he wasn't agreeing to the idea of cancelable tasks,
due to the bad experience of Scalaz 7.

Anyway, it was decided that something had to be done. So given my
experience  and short involvement in the discussions for
[reactive-streams.org](http://reactivestreams.org/), which Monix's
`Observable` was already implementing, I felt the need for a 
similar interoperability protocol between Task-like data types.
I also thought of this for selfish reasons. I did not want my
`Task` implementation to wither away by de jure standardization, 
because I believed in my approach and wanted it to succeed, 
therefore if there was going to be a proposal, I wanted to be
a part of it.

Therefore I submitted a proposal for a project named
[Schrodinger](https://github.com/typelevel/general/issues/66)
(later renamed to `effects4s`), an interop project that was supposed to be
the "reactive streams" protocol, but for task data types and a neutral ground
between Cats and Scalaz.

Well, it did not go well. The [Scalaz issue](https://github.com/scalaz/scalaz/issues/1355)
received no interest and the 
[Typelevel issue](https://github.com/typelevel/general/issues/66) was eventually
eclipsed by [cats-effect by Daniel Spiewak](https://github.com/typelevel/cats/issues/1617).

And indeed, it did not go well because my proposal was deeply flawed.
I did not want a "reference IO" in it and even though I was eventually convinced,
the type classes themselves where modeled for _getting data out_, or in other
words the emphasis was on _converting_ between data types. I was wrong about my 
type class design and Daniel Spiewak was right.

Leaving pride aside, I warmed up to it and started contributing. 
And I've been the author of [41 PRs](https://github.com/typelevel/cats-effect/pulls?q=is%3Apr+author%3Aalexandru), because I may not be experienced in type class design, but I can sure optimize an `IO`'s run-loop and optimize I did, repeatedly.

<img src="{{ site.baseurl }}public/images/scalaz.png" width="150" style="float:right;" align="right" />

### Trouble on the Horizon

You see, I'm a very competitive individual, so for example when John DeGoes used benchmarks showing dramatic improvements over Cats-Effect in [his presentation](https://www.youtube.com/watch?v=wi_vLNULh9Y) (at ScalaIO and Scale by the Bay), well I couldn't help myself but to follow up with no less than [one](https://github.com/typelevel/cats-effect/pull/90), [two](https://github.com/typelevel/cats-effect/pull/91), [three](https://github.com/typelevel/cats-effect/pull/95) batches of optimizations.

Most of the changes I did were already in use by Monix's `Task`. Via these optimizations I effectively replaced the internals of `cats.effect.IO`. Daniel's first implementation was more elegant, but mine was more efficient.

I also had breakthroughs in Monix of course, with 
[one](https://github.com/monix/monix/pull/474),
[two](https://github.com/monix/monix/pull/492) batches. Less impact though, due 
to Monix's `Task` already reaching its potential for many use cases.

I then realized that my `monix.tail.Iterant` is effectively blocked by the
type class hierarchy not exposing type classes for IO/Task data types that
can be canceled.

So being on a roll, I followed with PRs for
[cancelable IO](https://github.com/typelevel/cats-effect/pull/121) proposal,
followed by [Timer](https://github.com/typelevel/cats-effect/pull/132) proposal,
[type class changes](https://github.com/typelevel/cats-effect/pull/134)
(preceded by a [failed attempt](https://github.com/typelevel/cats-effect/pull/126)),
and [race](https://github.com/typelevel/cats-effect/pull/137), culminating in the 
[v0.10 release](https://github.com/typelevel/cats-effect/releases/tag/v0.10).

All of this ties into Monix of course, `Task` along with the new `Iterant` data 
type making full use of [Timer and cancelability](https://github.com/monix/monix/pull/598).
So now you've got yourself a credible, pull-based competitor to FS2 as well 😉

"_This is Sparta!_"
