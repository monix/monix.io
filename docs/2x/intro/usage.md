---
layout: docs
title: Usage in SBT and Maven
description: |
  Packages are published in Maven Central,
  cross-compiled for Scala 2.10, 2.11 and 
  for Scala.js
---

The packages are published on Maven Central, cross-compiled
for Scala 2.10 and 2.11, also cross-compiled to 
[Scala.js](http://www.scala-js.org/) {{ site.scalajs_full_version }}:

- Current 1.x release: `{{ site.version1x }}` 
  ([download source archive]({{ site.github.repo }}/archive/v{{ site.version1x }}.zip))
- Current 2.x release: `{{ site.version2x }}` 
  ([download source archive]({{ site.github.repo }}/archive/v{{ site.version2x }}.zip))

These install instructions are for Scala's
[SBT](http://www.scala-sbt.org/) (see the
[setup intructions](http://www.scala-sbt.org/0.13/docs/Setup.html))
and for [Apache Maven](https://maven.apache.org/) build tools.

## Everything in Monix Core

The main `monix` project contains everything in the Monix core, 
cross-compiled for:

- JVM: 
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix_2.11%7C{{ site.version2x }}%7C){:target="_blank"} /
  [Scala 2.10](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix_2.10%7C{{ site.version2x }}%7C){:target="_blank"}
- Javascript: 
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix_{{ site.scalajs_pack_version }}_2.11%7C{{ site.version2x }}%7C){:target="_blank"} /
  [Scala 2.10](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix_{{ site.scalajs_pack_version }}_2.10%7C{{ site.version2x }}%7C){:target="_blank"}
  
Depends on:

- [Sincron](https://sincron.org){:target="_blank"}
- [monix-execution](#sub-project-monix-execution)
- [monix-eval](#sub-project-monix-eval)
- [monix-reactive](#sub-project-monix-reactive)

### SBT

To use with insert the dependency in your `build.sbt` or `Build.scala`:

```scala
// for the JVM
libraryDependencies += "io.monix" %% "monix" % "{{ site.version2x }}"
```

Monix is cross-compiled with [Scala.js](http://www.scala-js.org/), 
so to target Javascript or mixed JVM/Javascript environments:

```scala
// for Scala.js/Javascript or cross-compilation
libraryDependencies += "io.monix" %%% "monix" % "{{ site.version2x }}"
```

### Maven

For Scala 2.11:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix_2.11</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

For Scala 2.10:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix_2.10</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

For Scala 2.11 and Scala.js {{ site.scalajs_full_version }}:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix_{{ site.scalajs_pack_version }}_2.11</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

For Scala 2.10 and Scala.js {{ site.scalajs_full_version }}:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix_{{ site.scalajs_pack_version }}_2.10</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

## Sub-project: monix-types

The `monix-types` subproject is like a kernel exposing Monix's
type-classes that are used for integration with
[Cats](http://typelevel.org/cats/) or other libraries. For the moment
this means shims for types such as `Monad`, `MonadError` or `Comonad`,
or `Evaluable`, a type-class meant to abstract over both `Task` or
`Coeval`.

Usage:

- JVM: 
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-types_2.11%7C{{ site.version2x }}%7C){:target="_blank"} /
  [Scala 2.10](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-types_2.10%7C{{ site.version2x }}%7C){:target="_blank"}
- Javascript: 
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-types_{{ site.scalajs_pack_version }}_2.11%7C{{ site.version2x }}%7C){:target="_blank"} /
  [Scala 2.10](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-types_{{ site.scalajs_pack_version }}_2.10%7C{{ site.version2x }}%7C){:target="_blank"}

Has no dependencies.

### SBT

```scala
// Targetting just the JVM
libraryDependencies += "io.monix" %% "monix-types" % "{{ site.version2x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-types" % "{{ site.version2x }}"
```

### Maven

For Scala 2.11:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-types_2.11</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

For Scala 2.10:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-types_2.10</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

For Scala 2.11 and Scala.js {{ site.scalajs_full_version }}:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-types_{{ site.scalajs_pack_version }}_2.11</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

For Scala 2.10 and Scala.js {{ site.scalajs_full_version }}:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-types_{{ site.scalajs_pack_version }}_2.10</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

## Sub-project: monix-execution

You can use just `monix-execution`, the lower level primitives for dealing
with asynchronous execution, thus exposing 
[Scheduler]({{ site.api2x }}#monix.execution.Scheduler) and
[Cancelable]({{ site.api2x }}#monix.execution.Cancelable):

- JVM: 
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-execution_2.11%7C{{ site.version2x }}%7C){:target="_blank"} /
  [Scala 2.10](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-execution_2.10%7C{{ site.version2x }}%7C){:target="_blank"}
- Javascript: 
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-execution_{{ site.scalajs_pack_version }}_2.11%7C{{ site.version2x }}%7C){:target="_blank"} /
  [Scala 2.10](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-execution_{{ site.scalajs_pack_version }}_2.10%7C{{ site.version2x }}%7C){:target="_blank"}

Depends on [Sincron](https://sincron.org){:target="_blank"}.

### SBT

```scala
// Targetting just the JVM
libraryDependencies += "io.monix" %% "monix-execution" % "{{ site.version2x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-execution" % "{{ site.version2x }}"
```

### Maven

For Scala 2.11:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-execution_2.11</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

For Scala 2.10:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-execution_2.10</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

For Scala 2.11 and Scala.js {{ site.scalajs_full_version }}:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-execution_{{ site.scalajs_pack_version }}_2.11</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

For Scala 2.10 and Scala.js {{ site.scalajs_full_version }}:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-execution_{{ site.scalajs_pack_version }}_2.10</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

## Sub-project: monix-eval

You can use just `monix-eval`, the sub-project that exposes
[Task]({{ site.api2x }}#monix.eval.Task) and
[Coeval]({{ site.api2x }}#monix.eval.Coeval):

- JVM: 
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-eval_2.11%7C{{ site.version2x }}%7C){:target="_blank"} /
  [Scala 2.10](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-eval_2.10%7C{{ site.version2x }}%7C){:target="_blank"}
- Javascript: 
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-eval_{{ site.scalajs_pack_version }}_2.11%7C{{ site.version2x }}%7C){:target="_blank"} /
  [Scala 2.10](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-eval_{{ site.scalajs_pack_version }}_2.10%7C{{ site.version2x }}%7C){:target="_blank"}

Depends on:

- [Sincron](https://sincron.org){:target="_blank"}
- [monix-types](#sub-project-monix-types)
- [monix-execution](#sub-project-monix-execution)

### SBT

```scala
// Targetting just the JVM
libraryDependencies += "io.monix" %% "monix-eval" % "{{ site.version2x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-eval" % "{{ site.version2x }}"
```

### Maven

For Scala 2.11:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-eval_2.11</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

For Scala 2.10:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-eval_2.10</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

For Scala 2.11 and Scala.js {{ site.scalajs_full_version }}:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-eval_{{ site.scalajs_pack_version }}_2.11</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

For Scala 2.10 and Scala.js {{ site.scalajs_full_version }}:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-eval_{{ site.scalajs_pack_version }}_2.10</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

## Sub-project: monix-reactive

You can use just `monix-reactive`, the sub-project that exposes
the [Observable]({{ site.api2x }}#monix.reactive.Observable) pattern:

- JVM: 
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-reactive_2.11%7C{{ site.version2x }}%7C){:target="_blank"} /
  [Scala 2.10](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-reactive_2.10%7C{{ site.version2x }}%7C){:target="_blank"}
- Javascript: 
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-reactive_{{ site.scalajs_pack_version }}_2.11%7C{{ site.version2x }}%7C){:target="_blank"} /
  [Scala 2.10](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-reactive_{{ site.scalajs_pack_version }}_2.10%7C{{ site.version2x }}%7C){:target="_blank"}

Depends on:

- [Sincron](https://sincron.org){:target="_blank"}
- [monix-types](#sub-project-monix-types)
- [monix-execution](#sub-project-monix-execution)
- [monix-eval](#sub-project-monix-eval)

### SBT

```scala
// Targetting just the JVM
libraryDependencies += "io.monix" %% "monix-reactive" % "{{ site.version2x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-reactive" % "{{ site.version2x }}"
```

### Maven

For Scala 2.11:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-reactive_2.11</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

For Scala 2.10:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-reactive_2.10</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

For Scala 2.11 and Scala.js {{ site.scalajs_full_version }}:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-reactive_{{ site.scalajs_pack_version }}_2.11</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

For Scala 2.10 and Scala.js {{ site.scalajs_full_version }}:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-reactive_{{ site.scalajs_pack_version }}_2.10</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

## Sub-project: monix-cats (Optional)

The `monix-cats` optional sub-projects is the integration 
with the [Cats](http://typelevel.org/cats/) library.

To import:

- JVM: 
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-cats_2.11%7C{{ site.version2x }}%7C){:target="_blank"} /
  [Scala 2.10](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-cats_2.10%7C{{ site.version2x }}%7C){:target="_blank"}
- Javascript: 
  [Scala 2.11](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-cats_{{ site.scalajs_pack_version }}_2.11%7C{{ site.version2x }}%7C){:target="_blank"} /
  [Scala 2.10](https://search.maven.org/#artifactdetails%7Cio.monix%7Cmonix-cats_{{ site.scalajs_pack_version }}_2.10%7C{{ site.version2x }}%7C){:target="_blank"}

Depends just on [monix-types](#sub-project-monix-types).

### SBT

```scala
// Targetting just the JVM
libraryDependencies += "io.monix" %% "monix-cats" % "{{ site.version2x }}"

// For Scala.js or cross-compiled projects
libraryDependencies += "io.monix" %%% "monix-cats" % "{{ site.version2x }}"
```

### Maven

For Scala 2.11:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-cats_2.11</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

For Scala 2.10:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-cats_2.10</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

For Scala 2.11 and Scala.js {{ site.scalajs_full_version }}:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-cats_{{ site.scalajs_pack_version }}_2.11</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```

For Scala 2.10 and Scala.js {{ site.scalajs_full_version }}:

```xml
<dependency>
    <groupId>io.monix</groupId>
    <artifactId>monix-cats_{{ site.scalajs_pack_version }}_2.10</artifactId>
    <version>{{ site.version2x }}</version>
</dependency>
```
