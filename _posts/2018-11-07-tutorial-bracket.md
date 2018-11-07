---
layout: post
title: "Task's Bracket, Cats-Effect's Resource and Streaming"
author: alexelcu
excerpt_separator: <!--more-->
description: A video tutorial.
video: https://player.vimeo.com/video/299313903?title=0&byline=0&portrait=0
---

A tutorial on usage of Task.bracket, Cats-Effect's Resource and how
that can be used in combination with streaming data types like
Observable and Iterant.

<!--more-->

<iframe src="https://player.vimeo.com/video/299313903?title=0&byline=0&portrait=0" width="640" height="415" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>

## Getting Started

To quickly get started with a sample project to play with:

```
sbt new monix/monix-3x.g8
```

## Resources

- [Task.bracket]({{ site.api3x }}monix/eval/Task.html#bracket[B](use:A=%3Emonix.eval.Task[B])(release:A=%3Emonix.eval.Task[Unit]):monix.eval.Task[B])
- [Coeval.bracket]({{ site.api3x }}monix/eval/Coeval.html#bracket[B](use:A=%3Emonix.eval.Coeval[B])(release:A=%3Emonix.eval.Coeval[Unit]):monix.eval.Coeval[B])
- [cats.effect.Bracket](https://typelevel.org/cats-effect/typeclasses/bracket.html)
- [cats.effect.Resource](https://typelevel.org/cats-effect/datatypes/resource.html)
