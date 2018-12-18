---
layout: page
title: "Asynchronous Programming: Scala.concurrent and Monix! What?"
description: |
  Presentation from ReactSphere 2018, by Yifan Xing introducing Scala's Futures, Promises along with Monix.
comments: true
image: /public/images/thumbs/2018-async-programming-yifan-xing.png
author: yifan_xing_e
youtube: Wq9YbTeOkjA
---

Presentation from
[ReactSphere 2018](https://react.sphere.it/) by
[Yifan Xing](https://twitter.com/yifan_xing_e):

{% include youtube.html ratio=56.25 %}

Resources:

- [Slides (SpeakerDeck)](https://speakerdeck.com/xingyif/asynchronous-programming-scala-dot-concurrent-and-monix-what)

## Abstract

In the context of executing a task, a synchronous execution indicates
that the program waits for the result of the first task before moving
on to another task. In contrast, an asynchronous execution does not
wait for the result of the first task and it starts executing another
task immediately after the first task is executed. Both Futures &
Promises and Monix are Scala libraries that allow users to efficiently
perform asynchronous operations. 

This talk will introduce Futures and Promises library in
`scala.concurrent` and Monix. We will walk through several examples
that demonstrate how to use Futures and Promises and Monix. In
addition, the talk compares and contrasts the similarities and
differences between the two libraries. Furthermore, we will discuss
some best practices in debugging asynchronous systems.
