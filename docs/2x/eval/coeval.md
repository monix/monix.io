---
layout: docs
title: Coeval
type_api: monix.eval.Coeval
type_source: monix-eval/shared/src/main/scala/monix/eval/Coeval.scala
description: |
  A data type for controlling immediate (synchronous), possibly lazy evaluation, useful for controlling side-effects, the sidekick of Task.
---

## Introduction

Coeval is a data type for controlling synchronous, possibly lazy
evaluation, useful for controlling side-effects. It is the sidekick of
[Task](./task.html), being meant for computations that are guaranteed
to execute immediately (synchronously).

Vocabulary definition:

> 1) *Having the same age or date of origin; contemporary*
> 
> 2) *Something of the same era*
>
> 3) *Synchronous*

```scala
import monix.eval.Coeval

val sum = Coeval { 
  println("Effect!")
  "Hello!"
}

// Nothing happens until being evaluated:
sum.value
//=> Effect!
// res1: String = Hello!

// And we can handle errors explicitly:
import scala.util.{Success, Failure}

sum.run match {
 case Success(value) =>
   println(value)
 case Failure(ex) =>
   System.err.println(ex)
}
```

### Design Summary

In summary the Monix `Coeval`:

- is just like [Task](./task.html), but only for immediate evaluation
- replacement for `lazy val` and by-name parameters
- doesn’t trigger the execution, or any effects until `value` or `run`
- allows for controlling of side-effects
- handles errors
  
A visual representation of where `Coeval` sits in the design space:

|                    |        Eager        |           Lazy           |
|:------------------:|:-------------------:|:------------------------:|
| **Synchronous**    |          A          |          () => A         |
|                    |                     |         Coeval[A]        |
| **Asynchronous**   | (A => Unit) => Unit |    (A => Unit) => Unit   |
|                    |      Future[A]      |          Task[A]         |

### Comparison with Cats Eval

...