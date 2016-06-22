---
layout: docs
title: "Akka &amp; Monix: Controlling Power Plants"
description: |
  Presentation from Typelevel Summit, Oslo, 2016,
  about an experience in dealing with modeling behavior
  by processing asynchronous soft-real time signals.
---

Presentation from the
[Typelevel Summit, Oslo, 2016](http://typelevel.org/event/2016-05-summit-oslo/)
by [@alexelcu](https://twitter.com/alexelcu):

<iframe width="640" height="360" class="presentation"
    src="https://www.youtube.com/embed/CQxviYlAKaY" frameborder="0" allowfullscreen></iframe>

Media: 

- [Slides (PDF file)](/public/pdfs/Akka-Monix.pdf)
- [Video (YouTube)](https://www.youtube.com/watch?v=CQxviYlAKaY)

Also see
[Monix Task: Lazy, Async &amp; Awesome](./2016-task-flatmap-oslo.html)
(flatMap(Oslo), 2016).

## Abstract

This talk is about my experience in dealing with modeling behavior
by processing asynchronous soft realtime signals from different
sources using Akka, along with Monix, the library for building
asynchronous and event-based logic.

It's an experience report from my work in monitoring and controlling
power plants. We do this by gathering signals in real time and
modeling state machines that give us the state in which an asset is in.
The component I worked on is the one component in the project that
definitely adheres to FP principles, the business logic being
described with pure functions and data-structures and the communication
being handled by actors and by Observable streams. I want to show
how I pushed side effects at the edges, in a very pragmatic setup.

This presentation will focus on Akka best practices, the wiring
needed for describing purely functional state, along with a
presentation of Monix Observable and how that helps.
