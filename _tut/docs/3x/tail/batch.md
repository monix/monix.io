---
layout: docs3x
title: Batch
type_api: monix.tail.batches.Batch
type_source: monix-tail/shared/src/main/scala/monix/tail/batches/Batch.scala
description: |
    Alternative to Scala's Iterable, for iterating over Scala's standard collections.
    
tut:
  scala: 2.12.4
  binaryScala: "2.12"
  dependencies:
    - io.monix::monix:version3x
---

## Introduction

Data type for iterating over Scala's standard collections.

Its `cursor()` method can be called repeatedly in order to yield
iterable cursors for the same sequence, of type 
<a href="{% api3x monix.tail.batches.BatchCursor %}">BatchCursor</a>.

This data type is provided as an alternative to Scala's 
<a href="{% scala_api scala.collection.Iterable %}">Iterable</a>,
having the same protocol, `Batch` corresponding to `Iterable`
and `BatchCursor` corresponding to 
<a href="{% scala_api scala.collection.Iterator %}">Iterator</a>, 
however the semantics are a little different, its API being optimized 
for `Iterant`:

- the list of supported operations is smaller, thus less API surface
  for things going wrong, without any broken default implementations
  that assume finite iterables
- implementations specialized for primitives are provided
  to avoid boxing
- it's a factory of `BatchCursor`, which provides hints
  for `recommendedBatchSize`, meaning how many elements should be
  be processed in a batch, more below
  
### Protocol

<a href="{% api3x monix.tail.batches.Batch %}">Batch</a> has the 
following definition, being an immutable factory of `BatchCursor`:

```scala
abstract class Batch[+A] extends Serializable {
  def cursor(): BatchCursor[A]
  
  // ...
}
```

<a href="{% api3x monix.tail.batches.BatchCursor %}">BatchCursor</a>
is a destructive, stateful type that has the following protocol:

```scala
abstract class BatchCursor[+A] extends Serializable {
  def hasNext(): Boolean

  def next(): A

  def recommendedBatchSize: Int
  
  // ...
}
```

So this is very much like the `Iterable` / `Iterator` pattern, except
for the odd looking `recommendedBatchSize` value. In order to consume
a `BatchCursor` you need to repeatedly call `hasNext()` as guard,
followed by `next()` to fetch the next element:

```tut:silent
import monix.tail.batches._

val batch = Batch.range(0, 100)
val cursor = batch.cursor()
var sum = 0

while (cursor.hasNext()) {
  sum += cursor.next()
}
```

Note the protocol isn't very pure, as it relies on internal, shared
state kept by `BatchCursor`, however it has two very important
virtues:

1. it can wrap and iterate over `Array` values with near zero overhead
2. it affords an efficient head/tail decomposition, for being able to
   process the current element and defer the rest, which we need for
   many Iterant operations
   
In FP code, as long as the processing of a `BatchCursor` is
encapsulated, such that referential transparency is preserved, then
all is fine.

## Recommended Batch Size

The `recommendedBatchSize` property of `BatchCursor` is needed because
it's perfectly OK for `Batch` and `BatchCursor` to wrap infinite
streams, or streams that trigger very expensive operations for each
element, but then when doing processing on such values, it might be
better to split the processing in smaller batches in order to 
preserve _fairness_.

For example say we want to locate an element, but we need to ensure
that once in a while cooperative yield of control (think cooperative
multi-threading) happens:

```tut:silent
import cats.syntax.all._
import cats.effect._
import monix.tail.batches._

def exists[A](batch: Batch[A], elem: A)
  (implicit timer: Timer[IO]): IO[Boolean] = {
  
  def loop(cursor: BatchCursor[A], batchSize: Int): IO[Boolean] =
    IO.suspend {
      if (!cursor.hasNext()) 
        IO.pure(false) 
      else {
        if (cursor.next() == elem)
          IO.pure(true)
        else if (batchSize > 1)
          loop(cursor, batchSize - 1)
        else {
          // Forced async boundary
          IO.shift *> loop(cursor, cursor.recommendedBatchSize)
        }
      }
    }
    
  IO(batch.cursor()).flatMap { c => 
    loop(c, c.recommendedBatchSize)
  }
}
```

This helps because in case you have a really big batch, infinite
perhaps, at the very least you won't block the current thread
indefinitely. And to do logic like this it's best if the `BatchCursor`
itself provides the needed hint, because it's the `BatchCursor`'s
implementation that knows best 😉

When wrapping something like an `Array`, it's assumed that the `Array`
has a reasonable size and so when processing it there's no reason to
split that in multiple batches, therefore the `recommendedBatchSize`
is going to be `Int.MaxValue`:

```tut:silent
val array = (0 until 1000).toArray

val batch = Batch.fromArray(array)
val cursor = batch.cursor()

assert(cursor.recommendedBatchSize == Int.MaxValue)
```

Here's a case of an `Iterable` that yields an iterator which
continuously produces the number `42`:

```tut:silent
val infinite42: Iterable[Int] = 
  new Iterable[Int] {
    def iterator = Iterator.continually(42)
    
    // Scala's Iterable is pretty unsafe ;-)
    override def toString(): String =
      "Iterable@" + System.identityHashCode(this)
  }
```

Producing this number is very cheap, however an implementation like
`Iterant` needs to provide some reasonable fairness guarantees, so it
would need to do batch processing in operations like
`zipMap`. Therefore when converting this iterable into a `Batch` we
might specify a value like `128`:

```tut:silent
val batch =
  Batch.fromIterable(infinite42, recommendedBatchSize = 128)
```

Thus when producing the cursor, we'll have the `128` as hint:

```tut:book
val cursor = batch.cursor()

cursor.recommendedBatchSize
```

Here's another case, an `Iterable` that reads lines from a `File`,
thus each element is expensive to produce, therefore when actual
processing is needed, it's better to process one line at a time:

```tut:silent
import java.io._

def readFile(file: File): Iterable[String] =
  new Iterable[String] {
    def iterator = new Iterator[String] {
      // Java's stdlib 🤢
      private[this] val in = new BufferedReader(new InputStreamReader(
        new FileInputStream(file), 
        "utf-8"))
          
      private[this] var line = in.readLine()
      
      def hasNext: Boolean =
        line != null
        
      def next(): String = {
        val prev = line
        line = in.readLine()
        prev
      }
    }
  }
```

Doing I/O is expensive, so when wrapping this in a `Batch`, it would
be better if the `recommendedBatchSize` is `1`:

```tut:silent
def readFileBatch(file: File): Batch[String] =
  Batch.fromIterable(readFile(file), recommendedBatchSize = 1)
```

## Building Batch values

### Wrapping Arrays: fromArray, fromAnyArray

You can wrap arrays, either as `Batch`:

```tut:silent
val array = (0 until 100).toArray

Batch.fromArray(array)
```

Or as `BatchCursor`:

```tut:silent
BatchCursor.fromArray(array)
```

This requires a `ClassTag`, yielding a specialized
<a href="{% api3x monix.tail.batches.ArrayBatch %}">ArrayBatch</a>.

When wrapping an array like this, when applying transformations like
`map` or `take`, the yielded `Batch` or `BatchCursor` are eagerly
processed:

```tut:silent
val batch = Batch.fromArray(Array(1, 2, 3)).map(_ + 1).take(10)

val cursor = BatchCursor.fromArray(Array(1, 2, 3)).map(_ + 1).take(10)
```

The `batch` or `cursor` values in the sample are not lazily evaluated,
because we know the source and `map` and `take` have immediate
execution for `ArrayBatch` and `ArrayCursor`.

When wrapping an array you can also specify an offset and a length:

```tut:book
Batch.fromArray(array, 10, 10)

BatchCursor.fromArray(array, 10, 10)
```

The implementation of this operation, in the case of `ArrayBatch` and
`ArrayCursor`, is also optimized as no array copy is being made, the
source array being used as is. Goes without saying that the wrapped
arrays should be "effectively immutable".

Wrapping specialized arrays can sometimes be a problem due to the need
for a `ClassTag`.  To coerce a conversion from `Array` to `Batch`
without having that `ClassTag` you can use `fromAnyArray`:

```tut:invisible
// https://github.com/monix/monix/issues/620
val array = (0 until 100).toArray[Any]
```
```tut:book
Batch.fromAnyArray[Int](array, 10, 10)

BatchCursor.fromAnyArray[Int](array, 10, 10)
```

Same thing as `fromArray`, except that the implementation uses
`Array[Any]`, thus boxing values as soon as the internal array gets
copied.

### Specialized Batches and BatchCursors

`Batch` and `BatchCursor` admit wrapping arrays in implementations
that are specialized for the standard primitives, in order to avoid
boxing. So specialized sub-types are provided that use an internal
specialized `Array`.

For `Boolean` this would yield
<a href="{% api3x monix.tail.batches.BooleansBatch %}">BooleansBatch</a>
and
<a href="{% api3x monix.tail.batches.BooleansCursor %}">BooleansCursor</a>
respectively:

```tut:book
Batch.booleans(Array(true, false, true))

BatchCursor.booleans(Array(true, false, true))
```

For `Byte` this would yield
<a href="{% api3x monix.tail.batches.BytesBatch %}">BytesBatch</a>
and
<a href="{% api3x monix.tail.batches.BytesCursor %}">BytesCursor</a>
respectively:

```tut:book
Batch.bytes((0 to 1000).map(_.toByte).toArray[Byte])

BatchCursor.bytes((0 to 1000).map(_.toByte).toArray[Byte])
```

For `Char` this would yield
<a href="{% api3x monix.tail.batches.CharsBatch %}">CharsBatch</a>
and
<a href="{% api3x monix.tail.batches.CharsCursor %}">CharsCursor</a>
respectively:

```tut:book
Batch.chars((0 to 1000).map(_.toChar).toArray[Char])

BatchCursor.chars((0 to 1000).map(_.toChar).toArray[Char])
```

For `Int` this would yield
<a href="{% api3x monix.tail.batches.IntegersBatch %}">IntegersBatch</a>
and
<a href="{% api3x monix.tail.batches.IntegersCursor %}">IntegersCursor</a>
respectively:

```tut:book
Batch.integers((0 to 1000).toArray[Int])

BatchCursor.integers((0 to 1000).toArray[Int])
```

For `Long` this would yield
<a href="{% api3x monix.tail.batches.LongsBatch %}">LongsBatch</a>
and
<a href="{% api3x monix.tail.batches.LongsCursor %}">LongsCursor</a>
respectively:

```tut:book
Batch.longs((0 to 1000).map(_.toLong).toArray[Long])

BatchCursor.longs((0 to 1000).map(_.toLong).toArray[Long])
```

For `Double` this would yield
<a href="{% api3x monix.tail.batches.DoublesBatch %}">DoublesBatch</a>
and
<a href="{% api3x monix.tail.batches.DoublesCursor %}">DoublesCursor</a>
respectively:

```tut:book
Batch.doubles((0 to 1000).map(_.toDouble).toArray[Double])

BatchCursor.doubles((0 to 1000).map(_.toDouble).toArray[Double])
```

### From any Iterable, Iterator or Seq

`Batch` being very much like an `Iterable`, you can use `fromIterable`
to easily get a `Batch` from one:

```tut:silent
Batch.fromIterable(Vector(1, 2, 3))
```

You can wrap an `Iterator` directly with `BatchCursor`:

```tut:silent
BatchCursor.fromIterator(Vector(1, 2, 3).iterator)

// Or with a specific recommendedBatchSize hint;
// see explanation in dedicated section
BatchCursor.fromIterator(
  Vector(1, 2, 3).iterator,
  recommendedBatchSize = 128)
```

Similarly this also works, being mostly an alias of `fromIterable`:

```tut:silent
Batch.fromSeq(Vector(1, 2, 3))
```

Note that it's perfectly possible to land with an infinite `Iterable`
on your hands, or one that has a very expensive payload for each
element, so you might want to fine-tune the `recommendedBatchSize`
parameter (see explanation below):

```tut:book
val only42: Iterable[Int] = 
  new Iterable[Int] {
    def iterator = Iterator.continually(42)
    
    // Scala's Iterable is pretty unsafe ;-)
    override def toString(): String =
      "Iterable@" + System.identityHashCode(this)
  }
  
Batch.fromIterable(only42, recommendedBatchSize = 128)
```

## Transformations

`Batch` and `BatchCursor` implement several operators for transforming
the source.

Note that not all `Iterable` / `Iterator` operators are supported,
only those that have proven useful to be used in concert with
the `recommendedBatchSize` hint.

Given this definition:

```tut:silent
val batch = Batch.range(0, 100)
val cursor = batch.cursor()
```

You can take a prefix of the elements via `take`:

```tut:silent
// Yields 0, 1, 2 ... 9
batch.take(10)

// Yields 0, 1, 2 ... 9
cursor.take(10)
```

You can `drop` a prefix:

```tut:silent
// Yields 10, 11 ... 99
batch.drop(10)

// Yields 10, 11 ... 99
cursor.drop(10)
```

You can take a `slice`:

```tut:silent
// Yields 10, 11 ... 19
batch.slice(10, 20)

// Yields 10, 11 ... 19
cursor.slice(10, 20)
```

You can `map` elements:

```tut:silent
// Yields 0, 2, 4 ... 198
batch.map(_ * 2)

// Yields 0, 2, 4 ... 198
cursor.map(_.toString)
```

You can `filter` them:

```tut:silent
// Yields 0, 2, 4 ... 98
batch.filter(_ % 2 == 0)

// Yields 0, 2, 4 ... 98
cursor.filter(_ % 2 == 0)
```

Or you can `collect` them:

```tut:silent
// Yields 0, 4, 8 ... 196
batch.collect { case x if x % 2 == 0 => x * 2 }

// Yields 0, 4, 8 ... 196
cursor.collect { case x if x % 2 == 0 => x * 2 }
```

## Conversions

Given a `Batch`:

```tut:silent
val batch = Batch.range(0, 10)
```

Conversion to `List`:

```tut:book
batch.toList
```

Conversion to `Array`:

```tut:book
batch.toArray
```

Conversion to `Iterable`:

```tut:book
batch.toIterable
```

Similarly given a `BatchCursor`:

```tut:silent
val cursor = batch.cursor()
```

Conversion to `List`:

```tut:book
cursor.toList
```

Conversion to `Array`:

```tut:invisible
val cursor = batch.cursor()
```
```tut:book
cursor.toArray
```

Conversion to an `Iterator`:

```tut:invisible
val cursor = batch.cursor()
```
```tut:book
cursor.toIterator
```
