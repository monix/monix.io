---
layout: docs
title: Typelevel Cats Integration
type_api: monix.cats.package
type_source: monix-cats/shared/src/main/scala/monix/cats/
description: |
  Integration and usage of the type-classes described in the Typelevel Cats library.
  
tut:
  scala: 2.11.8
  binaryScala: "2.11"
  dependencies:
    - io.monix::monix-reactive:version2x
    - io.monix::monix-cats:version2x
---

Monix provides integration
with [Typelevel's Cats](http://typelevel.org/cats/) library,
implementing useful type-classes for the data types exposed.

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

```tut:silent
import monix.cats._
```

This will bring Monix's type-class instances in scope.
And now we can verify that `Task` is indeed a `cats.Monad`:

```tut
import cats.Monad
import monix.eval.Task

implicitly[Monad[Task]]
```

In case you want a more à la carte importing experience, that's
possible as well:

```tut:silent
import monix.cats.monixToCatsMonad
```

What these imports are doing is to convert the types defined 
in `monix.types` to Cats' type-classes. However the convertion
can also work in reverse:

```tut:reset:silent
import monix.cats.reverse._
```

Here is a reverse conversion in action, pulling a
`monix.types.Monad[cats.Eval]`:

```tut:book
implicitly[monix.types.Monad[cats.Eval]]
```

Note that having both of these wildcard imports in scope generates
problems, so avoid doing it:

```scala
// DO NOT DO THIS!
import monix.cats._
import monix.cats.reverse._
```

That's it, now you can use the Cats awesome type-classes along
with Monix's abstractions, such as `Coeval`, `Task` and `Observable`.