---
layout: docs3x
title: A Decision Tree of Monix Observable Operators
type_api: monix.reactive.Observable
type_source: monix-reactive/shared/src/main/scala/monix/reactive/Observable.scala
description: |
    Helps you find the right observable operator
---

This work is a derivative of [A Decision Tree of Observable Operators](http://reactivex.io/documentation/operators.html#tree), used under [CC BY](https://creativecommons.org/licenses/by/3.0/). Instead of using names of ReactiveX methods, it uses names of Monix methods.

# General Advice
## Operators that return one value
There are several observable operators which return a single value:
- `head` returns the first element
- `last` returns the last element
- `forall` checks a predicate against all values in the stream.
- etc

These operators have a variant that returns an `Observable` (`headF`, `lastF`, `forallF`) and a variant that returns `Task` (`headL`, `lastL`, `forallL`).

Since `Task[A]` cannot represent "no values of A", `headL` and `lastL` throw exceptions when applied to empty observables. Use `headOptionL` and `lastOptionL`
which give you `Task[Option[A]]` instead.


# I want to create a new Observable...
- that emits a particular item: `pure` aka `now`

# I want to create an Observable by combining other Observables...
- and emitting all of the items from all of the Observables in whatever order they are received: `merge`

# I want to emit the items from an Observable after transforming them...
- one at a time with a function: `map`

# I want to re-emit only certain items from an Observable...
- by filtering out those that do not match a predicate: `filter`
- take only the first item: `headF`
