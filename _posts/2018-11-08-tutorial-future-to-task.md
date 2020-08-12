---
layout: post
title: "Converting Scala's Future to Task (Video)"
author: alexelcu
excerpt_separator: <!--more-->
description: A video tutorial showing the convertion facilities available for wrapping Scala's Future APIs into Task.
vimeo: 299709216
tutorial: 3x
---

A video tutorial showing the convertion facilities available for
wrapping Scala's Future APIs into Task.

<!--more-->

{% include vimeo.html ratio=54.68 %}

## Getting Started

To quickly get started with a sample project to play with,
[use our template](https://github.com/monix/monix-jvm-app-template.g8):

```
sbt new monix/monix-jvm-app-template.g8
```

## Resources

- [FutureLift](https://monix.io/api/3.0/monix/catnap/FutureLift.html)
- [Task.deferAction](https://monix.io/api/3.0/monix/eval/Task$.html#deferAction[A](f:monix.execution.Scheduler=%3Emonix.eval.Task[A]):monix.eval.Task[A])
- [Task.deferFutureAction](https://monix.io/api/3.0/monix/eval/Task$.html#deferFutureAction[A](f:monix.execution.Scheduler=%3Escala.concurrent.Future[A]):monix.eval.Task[A])
- [Task.deferFuture](https://monix.io/api/3.0/monix/eval/Task$.html#deferFuture[A](fa:=%3Escala.concurrent.Future[A]):monix.eval.Task[A])
