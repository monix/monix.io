---
layout: docs3x
title: Usage in SBT
description: |
  Packages are published in Maven Central,
  cross-compiled for Scala 2.11, 2.12 and 
  for Scala.js
---

The packages are published on Maven Central, cross-compiled
for Scala 2.11 and 2.12, also cross-compiled to 
[Scala.js](http://www.scala-js.org/) {{ site.scalajs_full_version }}:

- Current 3.x release: `{{ site.version3x }}` 
  ([download source archive]({{ site.github.repo }}/archive/v{{ site.version3x }}.zip))

These install instructions are for Scala's
[SBT](http://www.scala-sbt.org/) (see the
[setup instructions](http://www.scala-sbt.org/0.13/docs/Setup.html))
and for [Apache Maven](https://maven.apache.org/) build tools.

## Everything in Monix Core

The main `monix` project contains everything in the Monix core, 
cross-compiled for:

- JVM: 
  [Scala 2.12](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix_2.12%7C{{ site.version3x }}%7C) /
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix_2.11%7C{{ site.version3x }}%7C)
- Javascript: 
  [Scala 2.12](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix_{{ site.scalajs_pack_version }}_2.12%7C{{ site.version3x }}%7C) /
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix_{{ site.scalajs_pack_version }}_2.11%7C{{ site.version3x }}%7C)
  
Depends on:

- [cats](https://typelevel.org/cats/)
- [cats-effect](https://typelevel.org/cats-effect/)
- [monix-execution](#sub-project-monix-execution)
- [monix-eval](#sub-project-monix-eval)
- [monix-reactive](#sub-project-monix-reactive)
- [monix-tail](#sub-project-monix-tail)

To use with insert the dependency in your `build.sbt` or `Build.scala`:

```scala
// for the JVM
libraryDependencies += "io.monix" %% "monix" % "{{ site.version3x }}"
```

Monix is cross-compiled with [Scala.js](http://www.scala-js.org/), 
so to target Javascript or mixed JVM/Javascript environments:

```scala
// for Scala.js/Javascript or cross-compilation
libraryDependencies += "io.monix" %%% "monix" % "{{ site.version3x }}"
```

## Sub-project: monix-execution

You can use just `monix-execution`, the lower level primitives for dealing
with asynchronous execution, thus exposing 
[Scheduler]({{ site.api3x }}monix/execution/Scheduler.html) and
[Cancelable]({{ site.api3x }}monix/execution/Cancelable.html):

- JVM: 
  [Scala 2.12](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-execution_2.12%7C{{ site.version3x }}%7C) /
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-execution_2.11%7C{{ site.version3x }}%7C)
- Javascript: 
  [Scala 2.12](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-execution_{{ site.scalajs_pack_version }}_2.12%7C{{ site.version3x }}%7C) /
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-execution_{{ site.scalajs_pack_version }}_2.11%7C{{ site.version3x }}%7C)


```scala
// Targeting just the JVM
libraryDependencies += "io.monix" %% "monix-execution" % "{{ site.version3x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-execution" % "{{ site.version3x }}"
```

## Sub-project: monix-eval

You can use just `monix-eval`, the sub-project that exposes
[Task]({{ site.api3x }}monix/eval/Task$.html) and
[Coeval]({{ site.api3x }}monix/eval/Coeval$.html):

- JVM: 
  [Scala 2.12](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-eval_2.12%7C{{ site.version3x }}%7C) /
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-eval_2.11%7C{{ site.version3x }}%7C)
- Javascript: 
  [Scala 2.12](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-eval_{{ site.scalajs_pack_version }}_2.12%7C{{ site.version3x }}%7C) /
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-eval_{{ site.scalajs_pack_version }}_2.11%7C{{ site.version3x }}%7C)

Depends on:

- [cats](https://typelevel.org/cats/)
- [cats-effect](https://typelevel.org/cats-effect/)
- [monix-execution](#sub-project-monix-execution)

```scala
// Targeting just the JVM
libraryDependencies += "io.monix" %% "monix-eval" % "{{ site.version3x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-eval" % "{{ site.version3x }}"
```

## Sub-project: monix-reactive

You can use just `monix-reactive`, the sub-project that exposes
the [Observable]({{ site.api3x }}monix/reactive/Observable.html) pattern:

- JVM: 
  [Scala 2.12](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-reactive_2.12%7C{{ site.version3x }}%7C) /
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-reactive_2.11%7C{{ site.version3x }}%7C) /
  [Scala 2.10](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-reactive_2.10%7C{{ site.version3x }}%7C)
- Javascript: 
  [Scala 2.12](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-reactive_{{ site.scalajs_pack_version }}_2.12%7C{{ site.version3x }}%7C) /
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-reactive_{{ site.scalajs_pack_version }}_2.11%7C{{ site.version3x }}%7C) /
  [Scala 2.10](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-reactive_{{ site.scalajs_pack_version }}_2.10%7C{{ site.version3x }}%7C)

Depends on:

- [cats](https://typelevel.org/cats/)
- [cats-effect](https://typelevel.org/cats-effect/)
- [jctools](https://github.com/JCTools/JCTools)
- [monix-execution](#sub-project-monix-execution)
- [monix-eval](#sub-project-monix-eval)

```scala
// Targeting just the JVM
libraryDependencies += "io.monix" %% "monix-reactive" % "{{ site.version3x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-reactive" % "{{ site.version3x }}"
```

## Sub-project: monix-tail

You can use just `monix-tail`, the sub-project that exposes
[Iterant]({{ site.api3x }}monix/tail/Iterant.html) for pull based
streaming:

- JVM: 
  [Scala 2.12](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-tail_2.12%7C{{ site.version3x }}%7C) /
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-tail_2.11%7C{{ site.version3x }}%7C)
- Javascript: 
  [Scala 2.12](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-tail_{{ site.scalajs_pack_version }}_2.12%7C{{ site.version3x }}%7C) /
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-tail_{{ site.scalajs_pack_version }}_2.11%7C{{ site.version3x }}%7C)

Depends on:

- [cats](https://typelevel.org/cats/)
- [cats-effect](https://typelevel.org/cats-effect/)
- [monix-execution](#sub-project-monix-execution)
- [monix-eval](#sub-project-monix-eval)

```scala
// Targeting just the JVM
libraryDependencies += "io.monix" %% "monix-tail" % "{{ site.version3x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-tail" % "{{ site.version3x }}"
```
