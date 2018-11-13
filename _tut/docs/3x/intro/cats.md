---
layout: docs3x
title: Typelevel Cats Integration
type_api: monix.cats.package
type_source: monix-cats/shared/src/main/scala/monix/cats/
description: |
  Integration and usage of the type-classes described in the Typelevel Cats library.
  
tut:
  scala: 2.12.7
  binaryScala: "2.12"
  dependencies:
    - io.monix::monix-reactive:version3x
---

Since series 3.x Monix depends on and provides direct integration
with [Typelevel's Cats](http://typelevel.org/cats/) library,
implementing useful type-classes for the data types exposed and
making use of the vast ecosystem.

## Sample

We can verify that `Task` is indeed a `cats.Monad`:

```tut:book
import cats.Monad
import monix.eval.Task

implicitly[Monad[Task]]
```
