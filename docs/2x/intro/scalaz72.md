---
layout: docs
title: Scalaz 7.2 Integration
type_api: monix.scalaz.package
type_source: monix-scalaz/series-7.2/shared/src/main/scala/monix/scalaz
description: |
  Integration and usage of the type-classes described in the Scalaz library, version 7.2.
---

Monix provides integration with [Scalaz](http://scalaz.org/) library,
implementing useful type-classes for the data types exposed.

## Adding the Optional Dependency

The `monix-scalaz-72` is an optional dependency that you can add alongside
the main Monix dependency. So in `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "io.monix" %% "monix" % "{{ site.version2x }}",
  "io.monix" %% "monix-scalaz-72" % "{{ site.version2x }}"
)
```

Or maybe you want to use just `Task` along with this integration:

```scala
libraryDependencies ++= Seq(
  "io.monix" %% "monix-eval" % "{{ site.version2x }}",
  "io.monix" %% "monix-scalaz-72" % "{{ site.version2x }}"
)
```

For more details, see
[Usage in SBT and Maven](./usage.html#sub-project-monix-scalaz-72-optional).

## Usage

You just need to import the defined implicits:

```scala
import monix.scalaz._
```

This will bring Monix's type-class instances in scope.

```scala
import scalaz.Monad
import monix.eval.Task

implicitly[Monad[Task]]
//=> res1: scalaz.Monad[monix.eval.Task] = 
//     monix.scalaz.EvaluableInstances$ConvertMonixEvaluableToScalaz@49a2bc10
```
