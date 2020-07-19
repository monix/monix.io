---
layout: docs2x
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
  "io.monix" %% "monix" % "{{ site.promoted.version2x }}",
  "io.monix" %% "monix-scalaz-72" % "{{ site.promoted.version2x }}"
)
```

Or maybe you want to use just `Task` along with this integration:

```scala
libraryDependencies ++= Seq(
  "io.monix" %% "monix-eval" % "{{ site.promoted.version2x }}",
  "io.monix" %% "monix-scalaz-72" % "{{ site.promoted.version2x }}"
)
```

For more details, see
[Usage in SBT and Maven](./usage.md#sub-project-monix-scalaz-72-optional).

## Usage

You just need to import the defined implicits:

```scala mdoc:silent
import monix.scalaz._
```

This will bring Monix's type-class instances in scope.
And now we can verify that `Task` is indeed a `scalaz.Monad`:

```scala mdoc
import scalaz.Monad
import monix.eval.Task

implicitly[Monad[Task]]
```

In case you want a more à la carte importing experience, that's
possible as well:

```scala mdoc:silent
import monix.scalaz.monixToScalazMonad
```

What these imports are doing is to convert the types defined 
in `monix.types` to the Scalaz type-classes. However the convertion
can also work in reverse:

```scala mdoc:reset:silent
import monix.scalaz.reverse._

import scalaz._
import scalaz.std.AllInstances._
```

Here is a reverse conversion in action, pulling a
`monix.types.Monad[List]` out of the instances defined for Scalaz:

```scala mdoc
implicitly[monix.types.Monad[List]]
```

Note that having both of these wildcard imports in scope generates
problems, so avoid doing it:

```scala
// DO NOT DO THIS!
import monix.scalaz._
import monix.scalaz.reverse._
```

That's it, now you can use the Scalaz awesome type-classes along
with Monix's abstractions, such as `Coeval`, `Task` and `Observable`.
