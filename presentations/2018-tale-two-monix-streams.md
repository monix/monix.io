---
layout: page
title: "A Tale of Two Monix Streams — Scala Days, 2018, Berlin"
description: |
  Presentation from Scala Days, 2018, on Monix's Observable,
  compared with Iterant and a lesson in FP design
youtube: Ki4JvV66EbE
comments: true
---

Presentation from [Scala Days 2018](https://eu.scaladays.org/)
by [@alexelcu](https://twitter.com/alexelcu).

This is the second time this presentation was given. The first time it
was delivered at [Scala World in 2017](./2017-tale-two-monix-streams.html).

{% include youtube.html ratio=56.25 %}

Resources:

- [Slides (PDF file)](/public/pdfs/ScalaDays2018-Tale-TwoStreams.pdf)

## Abstract

Monix started as a project exposing an idiomatic, opinionated and
back-pressured ReactiveX implementation for Scala, but has grown
beyond those boundaries to fully incorporate the lessons of functional
programming.

I'm presenting a contrast between the Observable data type, which
works with an underlying push-based and very efficient protocol and
the new Iterant data type, a generic, purely functional, pull-based
streaming alternative coming in Monix 3.0.

Besides outlining the direction of where Monix is going, the
presentation is a lesson in functional programming design for FP
enthusiasts.

_Required knowledge:_ Attendees need to have a good grasp of the Scala
language. Some knowledge about FP libraries such as Cats is nice to
have, but not required.

_Learning objectives_ are to present where the Monix library is going
and to teach real-world techniques for functional programming design
that have helped in building the next iteration of the library.
