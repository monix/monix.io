---
layout: page
title: Monix
---

Asynchronous Programming for Scala and [Scala.js](http://www.scala-js.org/).

{% include github-stars.html %}

## Overview

Monix is a high-performance Scala / Scala.js library for composing asynchronous,
event-based programs.

<a href="https://typelevel.org/"><img src="{{ site.baseurl }}public/images/typelevel.png" width="150" style="float:right;" align="right" /></a>

A [Typelevel project](http://typelevel.org/projects/), Monix proudly
exemplifies pure, typeful, functional programming in Scala, while making no
compromise on performance.

Highlights:

- exposes the kick-ass `Observable`, `Iterant`, `Task` and `Coeval` data types,
  along with all the support they need
- modular, only use what you need
- designed for true asynchronicity, running on both the
  JVM and [Scala.js](http://scala-js.org)
- excellent test coverage, code quality and API documentation
  as a primary project policy

The project started as a proper implementation of [ReactiveX](http://reactivex.io/),
with stronger functional programming influences and designed from the ground up
for  back-pressure and made to cleanly interact with Scala's standard library,
compatible out-of-the-box with the [Reactive Streams](https://www.reactive-streams.org/)
protocol. It then expanded to include abstractions for suspending side effects
and for resource handling, being one of the parents and implementors of
[Cats Effect](https://typelevel.org/cats-effect/).

### Presentations

{% include presentations.html %}

### Download and Usage

The packages are published on Maven Central.

- 3.x release (latest): `{{ site.promoted.version3x }}`
  ([download source archive]({{ site.github.repo }}/archive/v{{ site.promoted.version3x }}.zip))
- 2.x release (older): `{{ site.promoted.version2x }}`
  ([download source archive]({{ site.github.repo }}/archive/v{{ site.promoted.version2x }}.zip))

In SBT for the latest 3.x release that integrates with
[Typelevel Cats](https://typelevel.org/cats/) out of the box
(use the `%%%` for Scala.js):

```scala
libraryDependencies += "io.monix" %% "monix" % "{{ site.promoted.version3x }}"
```

Monix is modular by design, so you can have an à la carte experience,
the project being divided in multiple sub-projects.

See [Usage in SBT](./docs/current/intro/usage.md), along with the [sub-modules graph](./docs/current/intro/usage.md#sub-modules--dependencies-graph), and don't miss the [versioning scheme](./docs/current/intro/versioning-scheme.md) for binary-backwards compatibility guarantees.

### Documentation

Documentation and tutorials:

- [Current](/docs/current/) ([3.x series](/docs/3x/))
- [2.x series](/docs/2x/)

API ScalaDoc:

- [Current]({{ site.apiCurrent }}) ([3.x]({{ site.api3x }}))
- [2.x]({{ site.api2x }})
- [1.x]({{ site.api1x }})

Relevant links to dependencies:

- [Cats](https://typelevel.org/cats/)
- [Cats Effect](https://typelevel.org/cats-effect/)

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

## Adopters

Here's a (non-exhaustive) list of companies that use Monix in production. Don't see yours? [You can add it in a PR!](https://github.com/monix/monix/blob/series/4.x/README.md)

- [Abacus](https://abacusfi.com)
- [Agoda](https://www.agoda.com)
- [commercetools](https://commercetools.com)
- [Coya](https://www.coya.com/)
- [E.ON Connecting Energies](https://www.eon.com/)
- [eBay Inc.](https://www.ebay.com)
- [Eloquentix](http://eloquentix.com/)
- [Hypefactors](https://www.hypefactors.com)
- [Iterators](https://www.iteratorshq.com)
- [Sony Electronics](https://www.sony.com)
- [Tinkoff](https://tinkoff.ru)
- [Zalando](https://www.zalando.com)
- [Zendesk](https://www.zendesk.com)

## License

Copyright (c) 2014-{{ site.time | date: '%Y' }} by The Monix Project Developers.
Some rights reserved.

[See LICENSE]({% link LICENSE.md %})
