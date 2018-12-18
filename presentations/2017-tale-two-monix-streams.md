---
layout: page
title: "Scala World 2017 — A Tale of Two Monix Streams"
description: |
  Presentation from Scala World 2017, on Monix's Observable, 
  compared with Iterant and a lesson in FP design
vimeo: 300015010
comments: true
---

Presentation from [Scala World 2017](https://scala.world/)
by [@alexelcu](https://twitter.com/alexelcu):

{% include vimeo.html ratio=56.25 %}

Resources:

- [Slides (PDF file)](/public/pdfs/ScalaWorld2017-Tale-TwoStreams.pdf)

## Abstract

Monix started as a project exposing an idiomatic, opinionated 
and back-pressured ReactiveX implementation for Scala, but has 
grown beyond those boundaries to fully incorporate the lessons of 
functional programming.

I'm presenting a contrast between the `Observable` data type, which works 
with an underlying push-based and very efficient protocol and the new 
`Iterant` data type, a generic, purely functional, pull-based streaming 
alternative coming in Monix 3.0.

Besides outlining the direction of where Monix is going, the presentation 
is a lesson in functional programming design for FP enthusiasts.
