---
layout: page
title: "Cancelable IO — Typelevel Summit, 2018, Berlin"
description: |
  Presentation from Typelevel Summit, 2018, on the evolution
  and design of the Cats-Effect IO.
vimeo: 300067490
comments: true
---

Presentation from the
[Typelevel Summit, 2018, Berlin](https://typelevel.org/event/2018-05-summit-berlin/)
by [@alexelcu](https://twitter.com/alexelcu).

{% include vimeo.html ratio=56.25 %}

Resources:

- [Slides (PDF file)](/public/pdfs/Cancelable-IO-2018.pdf)

## Abstract

Task / IO data types have been ported in Scala, inspired by Haskell's
monadic IO and are surging in popularity due to the need in functional
programming for referential transparency, but also because controlling
side effects by means of lawful, FP abstractions makes reasoning about
asynchrony and concurrency so much easier. But concurrency brings with
it race conditions, i.e. the ability to execute multiple tasks at the
same time, possibly interrupting the losers and cleaning up resources
afterwards and thus we end up reasoning about preemption. 

This talk describes the design of Monix's Task for cancelability and
preemption, a design that has slowly transpired in cats-effect, first
bringing serious performance benefits and now a sane design for
cancelation. Topics include how cancelable tasks can be described,
along with examples of race conditions that people can relate to,
highlighting the challenges faced when people use an IO/Task data type
that cannot be interrupted.
