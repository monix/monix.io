---
layout: docs
title: Typelevel Cats Integration
description: |
  Integration and usage of the type-classes described in the Typelevel Cats library.
---

Monix provides integration with
[Typelevel's Cats](http://typelevel.org/cats/) library, implementing
useful type-classes for the data types exposed.

## Adding the Optional Dependency

The `monix-cats` is an optional dependency that you can add alongside
the main Monix dependency. So in `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "io.monix" %% "monix" % "{{ site.version2x }}",
  "io.monix" %% "monix-cats" % "{{ site.version2x }}"
)
```

Or maybe you want to use just `Task` along with this integration:

```scala
libraryDependencies ++= Seq(
  "io.monix" %% "monix-eval" % "{{ site.version2x }}",
  "io.monix" %% "monix-cats" % "{{ site.version2x }}"
)
```

For more details, see
[Usage in SBT and Maven](./usage.html#sub-project-monix-cats-optional).

## Usage

You just need to import the defined implicits:

```scala
import monix.cats._
```

This will bring Monix's type-class instances in scope.

```scala
import cats.Monad
import monix.eval.Task

implicitly[Monad[Task]]
//=> res1: cats.Monad[monix.eval.Task] = 
//     monix.cats.EvaluableInstances$ConvertMonixEvaluableToCats@49a2bc10
```
