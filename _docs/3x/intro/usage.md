---
layout: docs3x
title: Usage in SBT
description: |
  Packages are published in Maven Central,
  cross-compiled for Scala 2.11, 2.12 and 
  for Scala.js
---

The packages are published on Maven Central, cross-compiled
for Scala 2.11, 2.12 and 2.13, also cross-compiled to 
[Scala.js](http://www.scala-js.org/) 0.6.x and 1.x:

- Current 3.x release: `{{ site.promoted.version3x }}` 
  ([download source archive]({{ site.github.repo }}/archive/v{{ site.promoted.version3x }}.zip))

These install instructions are for Scala's [SBT](http://www.scala-sbt.org/) (see the [setup instructions](https://www.scala-sbt.org/1.x/docs/)). Also see [versioning scheme](./versioning-scheme.md) for backwards-compatibility
guarantees.

## Everything in Monix

The main `monix` project contains everything in the Monix core.
  
Insert this line in `build.sbt` or `Build.scala`:

```scala
// for the JVM
libraryDependencies += "io.monix" %% "monix" % "{{ site.promoted.version3x }}"
```

Monix is cross-compiled with [Scala.js](http://www.scala-js.org/), 
so to target Javascript or mixed JVM/Javascript environments:

```scala
// for Scala.js/Javascript or cross-compilation
libraryDependencies += "io.monix" %%% "monix" % "{{ site.promoted.version3x }}"
```

Depends on:

- [cats](https://typelevel.org/cats/)
- [cats-effect](https://typelevel.org/cats-effect/)
- [jctools](https://github.com/JCTools/JCTools) (shaded)
- [reactive-streams-jvm](https://github.com/reactive-streams/reactive-streams-jvm)
- [monix-execution](#sub-project-monix-execution)
- [monix-catnap](#sub-project-monix-catnap)
- [monix-eval](#sub-project-monix-eval)
- [monix-reactive](#sub-project-monix-reactive)
- [monix-tail](#sub-project-monix-tail)

### Sub-modules &amp; Dependencies graph

<figure>
  <img src="{{ site.baseurl }}public/misc/dependencies.svg" alt="Dependencies graph" />
</figure>

## Sub-project: monix-execution

You can use just `monix-execution`, the lower level primitives for dealing
with asynchronous execution, thus exposing 
[Scheduler]({{ page.path | api_base_url }}monix/execution/Scheduler.html) and
[Cancelable]({{ page.path | api_base_url }}monix/execution/Cancelable.html).

```scala
// Targeting just the JVM
libraryDependencies += "io.monix" %% "monix-execution" % "{{ site.promoted.version3x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-execution" % "{{ site.promoted.version3x }}"
```

Depends on:
- [jctools](https://github.com/JCTools/JCTools) (shaded)
- [reactive-streams-jvm](https://github.com/reactive-streams/reactive-streams-jvm)
- [implicitbox](https://github.com/monix/implicitbox)

## Sub-project: monix-catnap

You can use just `monix-catnap` (see [API Docs]({{ page.path | api_base_url }}monix/catnap/index.html)), the high-level primitives building on top of [Cats Effect](https://typelevel.org/cats-effect/).

```scala
// Targeting just the JVM
libraryDependencies += "io.monix" %% "monix-catnap" % "{{ site.promoted.version3x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-catnap" % "{{ site.promoted.version3x }}"
```

Depends on:
- [monix-execution](#sub-project-monix-execution)
- [Cats Effect](https://typelevel.org/cats-effect/)
- [Cats](https://typelevel.org/cats/)

## Sub-project: monix-eval

You can use just `monix-eval`, the sub-project that exposes
[Task]({{ page.path | api_base_url }}monix/eval/Task$.html) and
[Coeval]({{ page.path | api_base_url }}monix/eval/Coeval$.html):

```scala
// Targeting just the JVM
libraryDependencies += "io.monix" %% "monix-eval" % "{{ site.promoted.version3x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-eval" % "{{ site.promoted.version3x }}"
```

Depends on:
- [monix-catnap](#sub-project-monix-catnap)

## Sub-project: monix-reactive

You can use just `monix-reactive`, the sub-project that exposes
the [Observable](../reactive/observable.md) pattern.

```scala
// Targeting just the JVM
libraryDependencies += "io.monix" %% "monix-reactive" % "{{ site.promoted.version3x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-reactive" % "{{ site.promoted.version3x }}"
```

Depends on:
- [monix-eval](#sub-project-monix-eval)

## Sub-project: monix-tail

You can use just `monix-tail`, the sub-project that exposes
[Iterant]({{ page.path | api_base_url }}monix/tail/Iterant.html) for pull based
streaming.

```scala
// Targeting just the JVM
libraryDependencies += "io.monix" %% "monix-tail" % "{{ site.promoted.version3x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-tail" % "{{ site.promoted.version3x }}"
```

Depends on:
- [monix-catnap](#sub-project-monix-catnap)
