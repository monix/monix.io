---
layout: page
title: "Functional Concurrency in Scala 101"
description: |
  Presentation from ScalaMatsuri 2019, by Piotr Gawryś, on concurrency basics in FP.
comments: true
author: p_gawrys
youtube: MeOs9SeO8-c
---

Presentation from
[ScalaMatsuri 2019](https://2019.scalamatsuri.org/index_en.html) by
[Piotr Gawryś](https://twitter.com/p_gawrys):

{% include youtube.html ratio=56.25 %}

## Abstract

Using IO Monads for concurrency gains a lot of recognition lately. However, Functional Programming and JVM’s concurrency model are challenging to learn separately, let alone together. There are many terms like green threads, thread shifting, brackets or fairness thrown around but what are those and why should we keep them in mind?

Starting from the basics, this talk aims to cover all of these. I hope any listener can walk out with enough knowledge to make a confident jump into FP libraries like Cats-Effect, Monix or ZIO.
