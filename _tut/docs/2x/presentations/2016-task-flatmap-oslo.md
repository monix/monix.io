---
layout: docs
title: "Monix Task: Lazy, Async &amp; Awesome"
description: |
  Presentation from flatMap(Oslo) 2016,
  about the Monix Task, a new type for dealing
  with asynchronous processing and side-effects.
---

Presentation from
[flatMap(Oslo) 2016](http://2016.flatmap.no/nedelcu.html#session)
by [@alexelcu](https://twitter.com/alexelcu):

<iframe src="https://player.vimeo.com/video/165922572" 
  width="640" height="360" class="presentation"
  frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen>
</iframe>

Media: 

- [Slides (PDF file)](/public/pdfs/Monix-Task.pdf)
- [Video (Vimeo)](https://vimeo.com/channels/flatmap2016/165922572)

Also see
[Akka &amp; Monix: Controlling Power Plants](./2016-akka-monix-typelevel.html)
(Typelevel Summit, Oslo, 2016).

## Abstract

Scala’s Future from the standard library is great, but sometimes we need more.

A Future strives to be a value, one detached from time and for
this reason its capabilities are restricted and for some use-cases
its behavior ends up being unintuitive. Hence, while the Future/Promise
pattern is great for representing asynchronous results of processes that
may or may not be started yet, it cannot be used as a specification
for an asynchronous computation.

The Monix Task is in essence about dealing with asynchronous
computations and non-determinism, being inspired by the Scalaz Task
and designed from the ground up for performance and to be compatible with
Scala.js/Javascript runtimes and with the Cats library. It also makes use of
Scala’s Future to represent results, the two being complementary.

In this talk I’ll show you its design, when you should use it and
why in dealing with asynchronicity it’s better to work with Task
instead of blocking threads.
