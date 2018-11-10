---
layout: page
title: "ScalaDays 2017 — Monix Task: Lazy, Async &amp; Awesome"
description: |
  Presentation from ScalaDays 2017, about the Monix Task, a data type 
  for dealing with asynchronous processing and side-effects.
comments: true
---

Presentation from [ScalaDays 2017](https://scaladays.org/archive/copenhagen2017.html)
by [@alexelcu](https://twitter.com/alexelcu):

{% include youtube.html link="https://www.youtube-nocookie.com/embed/wi97X8_JQUk" ratio=56.25 %}

Media: 

- [Slides (PDF file)](/public/pdfs/Monix-Task-ScalaDays2017.pdf)

## Abstract

Scala's `Future` from the standard library is great, but sometimes we need more. A `Future` strives to be a value, one detached from time and for this reason its capabilities are restricted and for some use-cases its behavior ends up being unintuitive. Therefore, while the Future/Promise pattern is great for representing asynchronous results of processes that may or may not be started yet, it cannot be used as a specification for an asynchronous computation.

The Monix `Task` is in essence about dealing with asynchronous computations and non-determinism, being inspired by the Scalaz Task and designed from the ground up for performance and to be compatible with Scala.js/JavaScript runtimes and with the Cats library. It also makes use of Scala's Future to represent results, the two being complementary.

In this talk I'll show you its design, when you should use it and why in dealing with asynchrony it's better to work with Task instead of blocking threads.
