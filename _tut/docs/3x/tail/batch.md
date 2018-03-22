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

It's `cursor()` method can be called repeatedly in order to yield 
iterable cursors for the same sequence.

This data type is provided as an alternative to Scala's 
<a href="{% scala_api scala.collection.Iterable %}">Iterable</a>,
having the same protocol (e.g. `Batch` corresponding to `Iterable`
and `BatchCursor` corresponding to `Iterator`), however the semantics
are a little different, its API being optimized for `Iterant`:

- the list of supported operations is smaller, thus less API surface for
  things going wrong
- implementations specialized for primitives are provided
  to avoid boxing
- it's a factory of `BatchCursor`, which provides hints
  for `recommendedBatchSize`, meaning how many batch can
  be processed in a batches

## Building Batch values

To get the imports out of the way:

```scala
import monix.tail.batches._
```

### Wrapping Arrays: fromArray, fromAnyArray

You can wrap arrays:

```tut:book
val array = (0 until 100).toArray
Batch.fromArray(array)
```

This requires a `ClassTag`, yielding a specialized
<a href="{% api3x monix.tail.batches.ArrayBatch %}">ArrayBatch</a>.

When wrapping an array like this, when applying transformations like `map` or `take`,
the yielded `Batch` is eagerly processed:

```tut:silent
val batch = Batch.fromArray(Array(1, 2, 3)).map(_ + 1).take(10)
```

The `batch` in the sample is not lazily evaluated, because we know the source and
`map` and `take` have immediate execution for `ArrayBatch`.

When wrapping an array you can also specify an offset and a length:

```tut:book
Batch.fromArray(array, 10, 10)
```

The implementation of this operation, in the case of `ArrayBatch` is also optimized,
as no array copy is being made, the source array being used as is. Goes without saying 
that the wrapped arrays should be "effectively immutable".

Wrapping specialized arrays can sometimes be a problem due to the need for a `ClassTag`.
To coerce a conversion from `Array` to `Batch` without having that `ClassTag` you can
use `fromAnyArray`:

```tut:book
Batch.fromAnyArray[Int](array, 10, 10)
```

Same thing as `fromArray`, except that the implementation uses `Array[Any]`, thus
boxing values as soon as the internal array gets copied.

### From any Iterable or Seq

`Batch` being very much like an `Iterable`, you can use `fromIterable` to easily get 
a `Batch` from one:

```tut:silent
Batch.fromIterable(Vector(1, 2, 3))
```

Similarly this also works, being mostly an alias:

```tut:silent
Batch.fromSeq(Vector(1, 2, 3))
```

Note that it's perfectly possible to land with an infinite `Iterable` on your hands.
And the implementation of iterant needs to provide 
