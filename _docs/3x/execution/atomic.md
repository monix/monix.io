---
layout: docs3x
title: Atomic
type_api: monix.execution.atomic.Atomic
type_source: monix-execution/jvm/src/main/scala/monix/execution/atomic/Atomic.scala
description: |
  References that can be updated atomically, for lock-free thread-safe programming, resembling Java's AtomicReference, but better.
---

Scala is awesome at handling concurrency and parallelism, providing
high-level tools for handling it, however sometimes you need to go
lower level. Java's library provides all the multi-threading
primitives required, however the interfaces of these primitives
sometime leave something to be desired.

One such example are the atomic references provided in
[java.util.concurrent.atomic](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/package-summary.html)
package. This project is an attempt at improving these types for daily
usage.

## Providing a Common interface

So you have
[j.u.c.a.AtomicReference&lt;V&gt;](http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/AtomicReference.html),
[j.u.c.a.AtomicInteger](http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/AtomicInteger.html),
[j.u.c.a.AtomicLong](http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/AtomicLong.html)
and
[j.u.c.a.AtomicBoolean](http://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/AtomicLong.html).
The reason why `AtomicReference<V>` does not suffice is because
compare-and-set works with reference equality, not structural equality
like it happens with primitives. So you cannot simply box an integer
and use it safely, plus you've got the whole boxing/unboxing overhead.

One problem is that all of these classes do not share a common
interface and there's no reason for why they shouldn't.

```scala mdoc:silent:nest
import monix.execution.atomic._

val refInt1: Atomic[Int] = Atomic(0)
val refInt2: AtomicInt = Atomic(0)

val refLong1: Atomic[Long] = Atomic(0L)
val refLong2: AtomicLong = Atomic(0L)

val refString1: Atomic[String] = Atomic("hello")
val refString2: AtomicAny[String] = Atomic("hello")
```

### Working with Numbers

One really common use-case for atomic references are for numbers to
which you need to add or subtract. To this purpose
`j.u.c.a.AtomicInteger` and `j.u.c.a.AtomicLong` have an
`incrementAndGet` helper. However Ints and Longs aren't the only types
you normally need. How about `Float` and `Double` and `Short`? How about
`BigDecimal` and `BigInt`?

In Scala, thanks to the
[Numeric[T]](http://www.scala-lang.org/api/current/index.html#scala.math.Numeric)
type-class, we can do this:

```scala mdoc:nest
val ref = Atomic(BigInt(1))

// now we can increment a BigInt
ref.incrementAndGet()

// or adding to it another value
ref.addAndGet(BigInt("329084291234234"))
```

But then if we have a type that isn't a number:

```scala mdoc:silent:nest
val string = Atomic("hello")
```

Trying to apply numeric operations will of course fail:

```scala mdoc:fail:nest
string.incrementAndGet()
```

### Support for Other Primitives (Float, Double, Short, Char, Byte)

Here's a common gotcha with Java's `AtomicReference<V>`. Suppose
we've got this Java atomic:

```scala mdoc:silent:nest
import java.util.concurrent.atomic.AtomicReference

val ref = new AtomicReference(0.0)
```

The unexpected happens on `compareAndSet`:

```scala mdoc:nest
val isSuccess = ref.compareAndSet(0.0, 100.0)
```

Calling `compareAndSet` fails because when using `AtomicReference<V>`
the equality comparison is done by reference and it doesn't work for
primitives because the process of
[Autoboxing/Unboxing](http://docs.oracle.com/javase/tutorial/java/data/autoboxing.html)
is involved. And then there's the efficiency issue. By using an
AtomicReference, you'll end up with extra boxing/unboxing going on.

`Float` can be stored inside an `AtomicInteger` by using Java's
`Float.floatToIntBits` and `Float.intBitstoFloat`. `Double` can be
stored inside an `AtomicLong` by using Java's
`Double.doubleToLongBits` and `Double.longBitsToDouble`. `Char`,
`Byte` and `Short` can be stored inside an `AtomicInteger` as well,
with special care to handle overflows correctly. All this is done to avoid boxing
for performance reasons.

```scala mdoc:nest
val ref = Atomic(0.0)

ref.compareAndSet(0.0, 100.0)

ref.incrementAndGet()

val ref2 = Atomic('a')

ref2.incrementAndGet()

ref2.incrementAndGet()
```

### Common Pattern: Loops for Transforming the Value

`incrementAndGet` represents just one use-case of a simple and more
general pattern. To push items in a queue for example, one would
normally do something like this in Java:

```scala mdoc:silent:nest
import collection.immutable.Queue
import java.util.concurrent.atomic.AtomicReference

def pushElementAndGet[T <: AnyRef, U <: T]
  (ref: AtomicReference[Queue[T]], elem: U): Queue[T] = {
  
  var continue = true
  var update = null

  while (continue) {
    var current: Queue[T] = ref.get()
    var update = current.enqueue(elem)
    continue = !ref.compareAndSet(current, update)
  }
  
  update
}
```

This is such a common pattern. Taking a page from the wonderful
[ScalaSTM](https://nbronson.github.io/scala-stm/),
with `Atomic` you can simply do this:

```scala mdoc:nest
val ref = Atomic(Queue.empty[String])

// Transforms the value and returns the update
ref.transformAndGet(_.enqueue("hello"))

// Transforms the value and returns the current one
ref.getAndTransform(_.enqueue("world"))

// We can be specific about what we want extracted as a result
ref.transformAndExtract { current =>
  val (result, update) = current.dequeue
  (result, update)
}

// Or the shortcut, because it looks so good
ref.transformAndExtract(_.dequeue)
```

Voilà, you now have a concurrent, thread-safe and non-blocking
Queue. You can do this for whatever persistent data-structure you
want.

NOTE: the transform methods are implemented using Scala macros, so
you get zero overhead by using them.

## Scala.js support for targeting Javascript

These atomic references are also cross-compiled to [Scala.js](http://www.scala-js.org/)
for targeting Javascript engines, because:

- it's a useful way of boxing mutable variables, in case you need to box
- it's a building block for doing synchronization, so useful for code that you want cross-compiled
- because mutability doesn't take *time* into account and `compareAndSet` does, atomic references and
  `compareAndSet` in particular is also useful in a non-multi-threaded / asynchronous environment

## Efficiency

Atomic references are low-level primitives for concurrency and because
of that any extra overhead is unacceptable.

### Boxing / Unboxing

Working with a common `Atomic[T]` interface implies boxing/unboxing of
primitives. This is why the constructor for atomic references always
returns the most specialized version, as to avoid boxing and unboxing:

```scala mdoc:nest
val ref1 = Atomic(1)

val ref2 = Atomic(1L)

val ref3 = Atomic(true)

val ref4 = Atomic("")
```

Increments/decrements are done by going through the
[Numeric[T]](http://www.scala-lang.org/api/current/index.html#scala.math.Numeric)
provided implicit, but only for `AnyRef` types, such as `BigInt` and
`BigDecimal`. For Scala's primitives the logic has been optimized to
bypass `Numeric[T]`.

### Cache-padded versions for avoiding the false sharing problem

In order to reduce cache contention, cache-padded versions for all Atomic
classes are provided. For reference on what that means, see:

- [mail.openjdk.java.net/pipermail/hotspot-dev/2012-November/007309.html](http://mail.openjdk.java.net/pipermail/hotspot-dev/2012-November/007309.html)
- [JEP 142: Reduce Cache Contention on Specified Fields](http://openjdk.java.net/jeps/142)

To use the cache-padded versions, you need to override the default
`PaddingStrategy`:

```scala mdoc:silent:nest
import monix.execution.atomic.PaddingStrategy.{Left64, LeftRight256}

// Applies padding to the left of the value for a cache line 
// of 64 bytes
val ref1 = Atomic.withPadding(1, Left64)

// Applies padding both to the left and the right of the value 
// for a total object size of at least 256 bytes
val ref2 = Atomic.withPadding(1, LeftRight256)
```

The strategies available are:

- `NoPadding`: doesn't apply any padding, the default
- `Left64`: applies padding to the left of the value, for a cache line of 64 bytes
- `Right64`: applies padding to the right of the value, for a cache line of 64 bytes
- `LeftRight128`: applies padding to both the left and the right, for a cache line of 128 bytes
- `Left128`: applies padding to the left of the value, for a cache line of 128 bytes
- `Right128`: applies padding to the right of the value, for a cache line of 128 bytes
- `LeftRight256`: applies padding to both the left and the right, for a cache line of 256 bytes

And now you can join the folks that have mechanical sympathy :-P

## Platform Intrinsics

Java 8 came with platform intrinsics, such that:

1. Issue
   [JDK-7023898](https://bugs.openjdk.java.net/browse/JDK-7023898)
   changed the `getAndAdd` method in `Unsafe` and all related methods
   in the `AtomicInt` and `AtomicLong` implementations, like
   `getAndIncrement` and `incrementAndGet`, to be translated to 
   `LOCK XADD` instructions on x86/x64 platforms, being far cheaper than 
   CAS loops based on `LOCK CMPXCHG` (normal `compareAndSet`)
2. Issue
   [JDK-8004330](https://bugs.openjdk.java.net/browse/JDK-8004330)
   changed the `getAndSet` in `Unsafe` and all atomic implementations
   to be translated to `LOCK XCHG`, which is also cheaper than CAS
   loops based on `LOCK CMPXCHG` (normal `compareAndSet`).  See
   this
   [article by Dave Dice](https://blogs.oracle.com/dave/entry/atomic_fetch_and_add_vs)
   for why this is awesome
   
Monix's `Atomic` implementations are also using the same platform
intrinsics when running on top of Java 8, but automatically fallback
to normal `compareAndSet` loops if running on top of Java 6 or 7.

So when you do this:

```scala mdoc:nest
val numberRef = Atomic(0)

val previous = numberRef.getAndSet(1)

val current = numberRef.incrementAndGet()
```

This code, depending on the Java version used will either use
optimized CPU instructions (Java 8 and above) or fallback to CAS
loops (e.g. Java 6 and 7, Android).