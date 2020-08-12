---
layout: docs2x
title: Usage in SBT
description: |
  Packages are published in Maven Central,
  cross-compiled for Scala 2.10, 2.11, 2.12 and 
  for Scala.js
---

The packages are published on Maven Central, cross-compiled
for Scala 2.10, 2.11 and 2.12, also cross-compiled to 
[Scala.js](http://www.scala-js.org/) 0.6.x:

- Current 1.x release: `{{ site.promoted.version1x }}` 
  ([download source archive]({{ site.github.repo }}/archive/v{{ site.promoted.version1x }}.zip))
- Current 2.x release: `{{ site.promoted.version2x }}` 
  ([download source archive]({{ site.github.repo }}/archive/v{{ site.promoted.version2x }}.zip))

These install instructions are for Scala's
[SBT](http://www.scala-sbt.org/) (see the
[setup instructions](http://www.scala-sbt.org/0.13/docs/Setup.html))
and for [Apache Maven](https://maven.apache.org/) build tools.

## Everything in Monix Core

The main `monix` project contains everything in the Monix core.
  
Depends on:

- [Sincron](https://sincron.org)
- [monix-execution](#sub-project-monix-execution)
- [monix-eval](#sub-project-monix-eval)
- [monix-reactive](#sub-project-monix-reactive)

To use with insert the dependency in your `build.sbt` or `Build.scala`:

```scala
// for the JVM
libraryDependencies += "io.monix" %% "monix" % "{{ site.promoted.version2x }}"
```

Monix is cross-compiled with [Scala.js](http://www.scala-js.org/), 
so to target Javascript or mixed JVM/Javascript environments:

```scala
// for Scala.js/Javascript or cross-compilation
libraryDependencies += "io.monix" %%% "monix" % "{{ site.promoted.version2x }}"
```

## Sub-project: monix-types

The `monix-types` subproject is like a kernel exposing Monix's
type-classes that are used for integration with
[Cats](http://typelevel.org/cats/) or other libraries. For the moment
this means shims for types such as `Monad`, `MonadError` or `Comonad`,
or `Evaluable`, a type-class meant to abstract over both `Task` or
`Coeval`.

Usage:


```scala
// Targetting just the JVM
libraryDependencies += "io.monix" %% "monix-types" % "{{ site.promoted.version2x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-types" % "{{ site.promoted.version2x }}"
```

## Sub-project: monix-execution

You can use just `monix-execution`, the lower level primitives for dealing
with asynchronous execution, thus exposing 
[Scheduler]({{ site.api2x }}monix/execution/Scheduler.html) and
[Cancelable]({{ site.api2x }}monix/execution/Cancelable.html):

```scala
// Targetting just the JVM
libraryDependencies += "io.monix" %% "monix-execution" % "{{ site.promoted.version2x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-execution" % "{{ site.promoted.version2x }}"
```

## Sub-project: monix-eval

You can use just `monix-eval`, the sub-project that exposes
[Task]({{ site.api2x }}monix/eval/Task$.html) and
[Coeval]({{ site.api2x }}monix/eval/Coeval$.html):

```scala
// Targetting just the JVM
libraryDependencies += "io.monix" %% "monix-eval" % "{{ site.promoted.version2x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-eval" % "{{ site.promoted.version2x }}"
```

## Sub-project: monix-reactive

You can use just `monix-reactive`, the sub-project that exposes
the [Observable]({{ site.api2x }}monix/reactive/Observable.html) pattern:

- [Sincron](https://sincron.org)
- [monix-types](#sub-project-monix-types)
- [monix-execution](#sub-project-monix-execution)
- [monix-eval](#sub-project-monix-eval)

```scala
// Targetting just the JVM
libraryDependencies += "io.monix" %% "monix-reactive" % "{{ site.promoted.version2x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-reactive" % "{{ site.promoted.version2x }}"
```

## Sub-project: monix-cats (Optional)

The `monix-cats` optional sub-projects is the integration 
with the [Cats](http://typelevel.org/cats/) library.

```scala
// Targetting just the JVM
libraryDependencies += "io.monix" %% "monix-cats" % "{{ site.promoted.version2x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-cats" % "{{ site.promoted.version2x }}"
```

## Sub-project: monix-scalaz-72 (Optional)

The `monix-scalaz-72` optional sub-projects is the integration 
with the [Scalaz](http://scalaz.org/) library.

```scala
// Targetting just the JVM
libraryDependencies += "io.monix" %% "monix-scalaz-72" % "{{ site.promoted.version2x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-scalaz-72" % "{{ site.promoted.version2x }}"
```
