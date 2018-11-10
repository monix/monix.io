---
layout: docs3x
title: Monix Documentation
---

## API Documentation

[ScalaDoc for Monix 3.x]({{ site.api3x }})

Related:

- [Cats ScalaDoc](https://typelevel.org/cats/api/){:target=>"_blank"}
- [Cats-Effect ScalaDoc](https://typelevel.org/cats-effect/api/){:target=>"_blank"}

## Getting Started

- [Usage with SBT](./intro/usage.html)
- [Hello World!](./intro/hello-world.html)
- [Typelevel Cats Integration](./intro/cats.html)

Quick-start template:

```
sbt new monix/monix-3x.g8
```

## Sub-projects

### monix-execution

The `monix-execution` sub-project provides low level, side effectful
utilities for dealing with concurrency, exposing the JVM's primitives
and building on top of the `scala.concurrent` package.

<img src="{{ site.baseurl }}public/images/logos/java.png" alt="Java's Duke" title="Java's Duke"
     class="doc-icon" />

- [API Documentation]({{ site.api3x }}monix/execution/index.html)
- [Scheduler](./execution/scheduler.html)
- [Cancelable](./execution/cancelable.html)
- [Callback](./execution/callback.html)
- [Future Utils](./execution/future-utils.html)
- [Atomic](./execution/atomic.html)

### monix-catnap

The `monix-catnap` sub-project is for generic, purely functional
utilities for managing concurrency, building on top of the
[Cats-Effect](https://typelevel.org/cats-effect/) type classes:

<img src="{{ site.baseurl }}public/images/logos/cats.png" alt="Cats" title="Cats"
     class="doc-icon" />

- [API Documentation]({{ site.api3x }}monix/catnap/index.html)
- [Circuit Breaker](./catnap/circuit-breaker.html)
- [MVar](./catnap/mvar.html)

### monix-eval

The `monix-eval` sub-project exposes the `Task` and `Coeval` data
types, for dealing with purely functional effects in a principled way:

- [API Documentation]({{ site.api3x }}monix/eval/index.html)
- [Task](./eval/task.html)
- [Coeval](./eval/coeval.html)

### monix-reactive

The `monix-reactive` sub-project exposes the `Observable` data type,
along with adjacent utilities, a high performance streaming abstraction
that's an idiomatic implementation of ReactiveX for Scala:

<img src="{{ site.baseurl }}public/images/logos/reactivex.png" alt="ReactiveX" title="ReactiveX"
     class="doc-icon" />

- [API Documentation]({{ site.api3x }}monix/reactive/index.html)
- [Observable](./reactive/observable.html)
- [Comparisons with Other Solutions](./reactive/observable-comparisons.html)
- [Observers and Subscribers](./reactive/observers.html)
- [Consumer](./reactive/consumer.html)
- [Javascript Event Listeners](./reactive/javascript.html)

### monix-tail

<img src="{{ site.baseurl }}public/images/logos/many-cats.png" alt="Cats friendly" title="Cats friendly"
     class="doc-icon2x" />

The `monix-tail` sub-projects exposes `Iterant`, a generic, purely
functional, principled, pull-based streaming data type:

- [API Documentation]({{ site.api3x }}monix/tail/index.html)

## Tutorials
  
- [Parallel Processing](./tutorials/parallelism.html)
{% for post in site.posts -%}{% if post.tutorial == "3x" %}
- [{{ post.title }}]({{ post.url }})
{% endif %}{% endfor %}
  
## Best Practices
  
- [Should Not Block Threads](./best-practices/blocking.html)

## Samples

- [Client/Server Communications](https://github.com/monixio/monix-sample/):
  a mixed [Play Framework](https://www.playframework.com/) /
  [Scala.js](http://www.scala-js.org/) app

## Presentations

{% include presentations.html %}

Work in progress! Please help!
