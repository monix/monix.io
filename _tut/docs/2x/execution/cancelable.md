---
layout: docs
title: Cancelable
type_api: monix.execution.Cancelable
type_source: monix-execution/shared/src/main/scala/monix/execution/Cancelable.scala
description: |
  A one-time idempotent action that can be used to cancel async computations, or to release resources that active data-sources are holding.

tut:
  scala: 2.11.8
  binaryScala: "2.11"
  dependencies:
    - io.monix::monix-execution:version2x
---

A one-time idempotent action that can be used to cancel async
computations, or to release resources that active data-sources are
holding, it is the equivalent of `java.io.Closable`, but without the
I/O focus, or to `IDisposable` from .NET, or to
`akka.actor.Cancellable` (but with an `l` dropped :)).

## Base Cancelable

```scala
package monix.execution

trait Cancelable {
  def cancel(): Unit
}
```

The contract for well-behaved `Cancelable` instances:

1. `cancel` is idempotent, calling it multiple times has
   the same effect as calling it once
2. `cancel` is thread-safe, otherwise you cannot guarantee
   idempotency

In order to quickly build `Cancelable` instances:

```tut:reset:silent
import monix.execution.Cancelable

// A cancelable that doesn't do anything on cancel
val c = Cancelable.empty

// Same thing, a cancelable that doesn't do anything
val c = Cancelable()

// Specify a callback to be called on cancel
val c = Cancelable(() => println("Canceled!"))
```

## BooleanCancelable

The `BooleanCancelable` represents a cancelable that can be queried
for its canceled status, adding the necessary `isCanceled` query for
when we need it.

See the
[API Documentation]({{ site.api2x }}monix/execution/cancelables/BooleanCancelable.html).

```scala
package monix.execution.cancelables

trait BooleanCancelable extends Cancelable {
  def isCanceled: Boolean
}
```

To have a reusable (immutable) `BooleanCancelable` instance that's
already canceled:

```tut:reset:silent
import monix.execution.cancelables._

// Building an instance that's already canceled
val c = BooleanCancelable.alreadyCanceled

// Doesn't do anything
c.cancel()

// Always returns true
c.isCanceled
```

To build an instance without a callback, but that can be
used to check for `isCanceled`:


```tut:book
val c = BooleanCancelable()

c.isCanceled

c.cancel()

c.isCanceled
```

To build an instance out of a callback that behaves like
you'd expect it to:

```tut:silent
val c = BooleanCancelable(() => println("Effect!"))
```

## CompositeCancelable

The `CompositeCancelable` is an aggregate of cancelable
references (to which you can add new references or remove existing ones)
and that are handled in aggregate when doing a `cancel()`.

See the
[API Documentation]({{ site.api2x }}monix/execution/cancelables/CompositeCancelable.html).

The contract for `CompositeCancelable`:

- adding and removing cancelables from the composite
  is thread-safe
- if the composite was already canceled, then adding new
  references to it will trigger their cancelation
- upon cancelation all references are released

Usage:

```tut:silent
import monix.execution.Cancelable

val c = CompositeCancelable()

c += Cancelable(() => println("Canceled #1"))
c += Cancelable(() => println("Canceled #2"))
c += Cancelable(() => println("Canceled #3"))

// Cancelling will trigger all 3 of them
c.cancel()

// Appending a new cancelable to it after cancel
// will trigger its cancelation immediately
c += Cancelable(() => println("Canceled #4"))
// => Canceled #4
```

We can add cancelables references to our composite, but
we can also remove them, maybe because they are no longer
relevant, for GC purposes, etc:

```tut:silent
val composite = CompositeCancelable()

val c1 = Cancelable(() => println("Canceled #1"))
val c2 = Cancelable(() => println("Canceled #2"))

composite += c1
composite += c2

// Removing from our composite
composite -= c1

// Canceling will now only cancel c2
composite.cancel()
// => Canceled #2
```

## MultiAssignmentCancelable

The `MultiAssignmentCancelable` is a cancelable that behaves like a
variable, referencing another cancelable reference that can be
swapped as needed.

See the
[API Documentation]({{ site.api2x }}monix/execution/cancelables/MultiAssignmentCancelable.html).

Contract:

- assignment is thread-safe
- cancelation will trigger the cancelation of the underlying
  cancelable and releasing it in order to be free for GC
- if our assignable cancelable was canceled, then upon
  subsequent assignments, the references will be canceled
  immediately

Usage:

```tut:silent
val multiAssignment = MultiAssignmentCancelable()

val c1 = Cancelable(() => println("Canceled #1"))
multiAssignment := c1

val c2 = Cancelable(() => println("Canceled #2"))
multiAssignment := c2

// Canceling it will only cancel the last assignee
multiAssignment.cancel()
// => Cancelled #2

// Subsequent assignments are canceled immediately
val c3 = Cancelable(() => println("Canceled #3"))
multiAssignment := c3
// => Canceled #3
```

In a multi-threading environment sometimes we cannot guarantee an
ordering on assignment and obviously ordering is important.  There's a
second assignment operation that takes an `order` numeric argument and
in case the update was made with an `order` that's strictly bigger
than the current one you're trying to make, then the assignment gets
ignored, so:

```tut:silent
// Let's simulate a race condition
import monix.execution.Scheduler.{global => scheduler}

val c = MultiAssignmentCancelable()

scheduler.execute(new Runnable {
  def run(): Unit =
    c.orderedUpdate(
      Cancelable(() => println("Number #2")),
      order = 2)
})

c.orderedUpdate(
  Cancelable(() => println("Number #1")),
  order = 1)
```

In the example above, there is no happens-before relationship between
the two `orderedUpdate` attempts, so we have non-determinism, because
a parallel thread might be faster than our current one and trigger the
number 2 update before number 1. But this will leave us with a result
that we might not want, because then the last update would be
number 1.  So here is where `orderedUpdate` comes in handy. We
explicitly specify an `order` argument and thus force an ordering to
it.

The example above is obvious, right? But the following one isn't and
it's in fact a pretty common pattern. Let's build a function that
executes things with a delay, tasks that can be canceled. To
add a delay, we'd use a `Scheduler` and we want to return
a `Cancelable` that can cancel either the delay or the result
of our passed function argument, like:

```tut:silent
// INCORRECT EXAMPLE
import concurrent.duration._
import monix.execution._

def delayedExecution(cb: () => Cancelable)
  (implicit s: Scheduler): Cancelable = {

  val ref = MultiAssignmentCancelable()

  ref := s.scheduleOnce(5.seconds) {
    ref := cb()
  }

  ref
}
```

You may not notice it, but this is a race condition that can
yield non-deterministic behavior. Lets say the garbage collector
has problems and freezes the whole process for 5 whole seconds.
This can mean that `ref := cb()` might execute before the result
of `s.scheduleOnce` returns, as the call to `:=` does not have
a happens-before relationship with the actual delayed scheduling.
Which means the cancelable returned by our function will be incorrect.

Lets fix it:

```tut:silent
import concurrent.duration._
import monix.execution._

def delayedExecution(cb: () => Cancelable)
  (implicit s: Scheduler): Cancelable = {

  val ref = MultiAssignmentCancelable()
  val delay = s.scheduleOnce(5.seconds) {
    ref.orderedUpdate(cb(), 2)
  }

  // This should be the first update, but
  // if not, then it is ignored!
  ref.orderedUpdate(delay, 1)
  ref
}
```

## SingleAssignmentCancelable

The `SingleAssignmentCancelable` is similar to the
`MultiAssignmentCancelable`, except that it can be assigned once and
only once.

See the
[API Documentation]({{ site.api2x }}monix/execution/cancelables/SingleAssignmentCancelable.html).

The contract:

- it is thread-safe
- if canceled after assignment, the underlying cancelable gets
  canceled and the reference released for GC purposes
- if canceled while empty, then the assigned cancelable will be
  canceled immediately on assignment
- if assignment happens a second time, then the operation
  will throw an `IllegalStateException`, so don't do that

It is useful in cases you need a forward reference, like:

```tut:silent
val ref = SingleAssignmentCancelable()

ref := scheduler.scheduleAtFixedRate(0.seconds, 5.seconds) {
  // This wouldn't be correct without having an already
  // initialized value, as it would be a forward reference
  // (e.g. a reference used before it's initialized), which
  // could lead to a NullPointerException, not cool!
  ref.cancel()
}
```

It is also possible to specify at construction time an extra
cancelable reference to cancel, in addition to the assigned reference:

```tut:silent
val c = {
  val guest = Cancelable(() => println("extra canceled"))
  SingleAssignmentCancelable.plusOne(guest)
}

c := Cancelable(() => println("primary canceled"))

c.cancel()
//=> extra canceled
//=> primary canceled
```

You can use `MultiAssignmentCancelable` for the same purpose of
course, but the implementation of `SingleAssignmentCancelable` is more
efficient (e.g. using `getAndSet`, cheaper than `compareAndSet`) and
the `IllegalStateException` is nice when dealing with concurrent code
that isn't doing what it's supposed to do.

## SerialCancelable

The `SerialCancelable` is also similar to `MultiAssignmentCancelable`,
being a cancelable whose underlying reference can be swapped by another
cancelable, causing the previous cancelable to be canceled on assignment.

See the
[API Documentation]({{ site.api2x }}monix/execution/cancelables/SerialCancelable.html).

Contract:

- assignment is thread-safe
- cancelation will trigger the cancelation of the underlying
  cancelable and releasing it in order to be free for GC
- if our assignable cancelable was canceled, then upon
  subsequent assignments, the references will be canceled
  immediately
- an assignment of a new cancelable will cause the previously
  stored cancelable to be canceled

Usage:

```tut:silent
val ref = SerialCancelable()

ref := Cancelable(() => println("Canceled #1"))

// Will cancel the previous one
ref := Cancelable(() => println("Canceled #2"))
// => Canceled #1

// Will cancel the previous one
ref := Cancelable(() => println("Canceled #3"))
// => Canceled #2

// Will cancel the current one
ref.cancel()
// => Canceled #3

// Afterwards everything gets canceled on assignment
ref := Cancelable(() => println("Canceled #4"))
// => Canceled #4
```

## RefCountCancelable

Represents a `Cancelable` that can create dependent cancelable objects
and that only executes the canceling logic when all dependent
cancelable objects have been canceled.

Contract:

- thread-safe
- if we create a total number of `N` children cancelables with
  `acquire()`, in order for cancelation to occur we need to cancel all
  `N` cancelables, in addition to triggering a cancelation on the
  parent
- the ordering of cancelation doesn't matter (e.g. parent first,
  children next, or vice-versa)
- on creating cancelables with `acquire()`, if our parent is already
  canceled, then the returned reference will be a `Cancelable.empty`

Sample:

```tut:silent
val refs = RefCountCancelable { () =>
  println("Everything was canceled")
}

// acquiring two cancelable references
val ref1 = refs.acquire()
val ref2 = refs.acquire()

// Starting the cancelation process
refs.cancel()

// This is now true, but the callback hasn't been invoked yet
refs.isCanceled
// res: Boolean = true

// After our RefCountCancelable was canceled, this will return
// a Cancelable.empty
val ref3 = refs.acquire()

ref3 == Cancelable.empty
// res: Boolean = true

// Release reference 1, nothing happens here
ref1.cancel()

// Release reference 2, now we can execute
ref2.cancel()
// => Everything was canceled
```

This cancelable type is actually used in `Observable.merge` (•‿•)
