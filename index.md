---
layout: page
title: Monix
---

Asynchronous Programming for Scala and [Scala.js](http://www.scala-js.org/).

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/monixio/monix)

## Overview

Monix is a high-performance Scala / Scala.js library for
composing asynchronous and event-based programs, exposing high-level
types, such as observable sequences that are exposed as asynchronous streams,
expanding on the [observer pattern](https://en.wikipedia.org/wiki/Observer_pattern),
strongly inspired by [ReactiveX](http://reactivex.io/) and by [Scalaz](http://scalaz.org/),
but designed from the ground up  for back-pressure and made to cleanly interact
with Scala's standard library, compatible out-of-the-box with the
[Reactive Streams](http://www.reactive-streams.org/) protocol.

Highlights:

- exposes the kick-ass `Observable`, `Task` and `Coeval`
- modular, only use what you need
- the base library has no third-party dependencies
- strives to be idiomatic Scala and encourages referential transparency,
  but is built to be faster than alternatives
- accepted in the [Typelevel incubator](http://typelevel.org/projects/)
- designed for true asynchronicity, running on both the
  JVM and [Scala.js](http://scala-js.org)
- really good test coverage and API documentation as a project policy

### Dependencies

The packages are published on Maven Central.

- Current stable release: `{{ site.version }}`
- Current beta release: `{{ site.beta_version }}`

For the stable release (use the `%%%` for Scala.js):

```scala
libraryDependencies += "org.monifu" %% "monifu" % "{{ site.version }}"
```

For the beta/preview release (use at your own risk):

```scala
libraryDependencies += "io.monix" %% "monix" % "{{ site.beta_version }}"
```

### Sub-projects

Monix 2.0 is modular by design, so you can pick and choose:

- `monix-execution` exposes the low-level execution environment, or more precisely
  `Scheduler`, `Cancelable` and `CancelableFuture`
- `monix-eval` exposes `Task`, `Coeval`, the base type-classes of `monix.types`
   and depends on `monix-execution`
- `monix-reactive` exposes `Observable` streams and depends on `monix-eval`
- `monix` provides all of the above
