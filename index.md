---
layout: page
title: Monix
---

Asynchronous Programming for Scala and [Scala.js](http://www.scala-js.org/).

[![Gitter]({{ site.baseurl }}public/images/gitter.svg)]({{ site.github.chat }})

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
- is a [Typelevel project](http://typelevel.org/projects/)
- designed for true asynchronicity, running on both the
  JVM and [Scala.js](http://scala-js.org)
- really good test coverage and API documentation as a project policy

Featured presentation:

<iframe src="https://player.vimeo.com/video/165922572" 
  width="640" height="360" class="presentation"
  frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen>
</iframe>
**[flatMap(Oslo), 2016](/docs/2x/presentations/2016-task-flatmap-oslo.html)**

### Download and Usage

The packages are published on Maven Central.

- Current 2.x release: `{{ site.version2x }}` 
  ([download source archive]({{ site.github.repo }}/archive/v{{ site.version2x }}.zip))


In SBT (use the `%%%` for Scala.js):

```scala
libraryDependencies += "io.monix" %% "monix" % "{{ site.version2x }}"
```

Monix 2.0 is modular by design, so you can pick and choose:

- **monix-execution**: the low-level execution environment, or more precisely
  `Scheduler`, `Cancelable`, `CancelableFuture` and `Atomic`
- **monix-eval**: for controlling evaluation by means of `Task`,
  `Coeval` and primitives built on them
- **monix-reactive**: async `Observable` streams
- **monix**: provides all of the above

Optional integrations:

- **monix-cats**: exposes Typelevel [Cats](http://typelevel.org/cats/)
  type-class instances, see [the intro](/docs/2x/intro/cats.html)
- **monix-scalaz-72**: exposes [Scalaz](https://github.com/scalaz/scalaz)
  type-class instances, see [the intro](/docs/2x/intro/scalaz72.html)

Head over to **[Usage in SBT and Maven](/docs/2x/intro/usage.html)** for 
more details.

### Documentation

Links: 

- [Latest 1.x ScalaDoc (old Monifu)]({{ site.api1x }})
- [Latest 2.x ScalaDoc]({{ site.api2x }})
- [2.x documentation and tutorials](/docs/2x/)

Please contribute!

## Latest News

<ol class="news-summary">
  {% for post in site.posts limit: 5 %}
  <li>
    <time itemprop="dateCreated"
      datetime="{{ post.date | date: "%Y-%m-%d" }}">
      {{ post.date | date_to_string }} »
    </time>
    <a href="{{ post.url }}">{{ post.title }}</a>
  </li>
  {% endfor %}
</ol>

## Acknowledgements

<img src="{{ site.baseurl }}public/images/logos/yklogo.png"
align="right" /> YourKit supports the Monix project with its
full-featured Java Profiler.  YourKit, LLC is the creator
[YourKit Java Profiler](http://www.yourkit.com/java/profiler/index.jsp)
and
[YourKit .NET Profiler](http://www.yourkit.com/.net/profiler/index.jsp),
innovative and intelligent tools for profiling Java and .NET
applications.

<img src="{{ site.baseurl }}public/images/logos/logo-eloquentix@2x.png" align="right" width="130" />
Development of Monix has been initiated by
[Eloquentix](http://eloquentix.com/) engineers, with
Monix being introduced at E.ON Connecting Energies, powering the next
generation energy grid solutions.
