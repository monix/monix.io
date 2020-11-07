---
layout: docs3x
title: Local
type_api: monix.execution.misc.Local
type_source: monix-execution/shared/src/main/scala/monix/execution/misc/Local.scala
description: |
  A ThreadLocal whose scope is flexible and can be preserved across asynchronous boundaries.
---

A `Local` is a `ThreadLocal` whose scope is flexible and can be preserved across asynchronous boundaries.

## Rationale

The main use case for `Local` is integration with tools such as [MDC](http://logback.qos.ch/manual/mdc.html), or [OpenTelemetry](https://opentelemetry.io/)
to propagate the context without passing it manually in parameters.

Traditionally, it is achieved via `ThreadLocal` usage. 
However, in Scala, it is common to write applications with asynchronous data types, such as Scala's `Future`, or Monix `Task`.
These types can jump between threads, so the context in `ThreadLocal` will be lost.

Consider the following example:

```scala
object LocalExample extends App with StrictLogging {
  implicit val ec = ExecutionContext.global

  def req(requestId: String, userName: String): Future[Unit] = Future {
    logger.info(s"Received a request to create a user $userName")
    // business logic
  }.flatMap(_ => registerUser(userName))
  
  def registerUser(name: String): Future[Unit] = Future {
    // business logic
    logger.info(s"Registering a new user named $name")
  }

  val requests = List(req("1", "Clark"), req("2", "Bruce"), req("3", "Diana"))
  Await.result(Future.sequence(requests), Duration.Inf)
  
  //=> Received a request to create a user Bruce
  //=> Registering a new user named Bruce
  //=> Received a request to create a user Diana
  //=> Registering a new user named Diana
  //=> Received a request to create a user Clark
  //=> Registering a new user named Clark
}
```

If we would like to attach corresponding `requestId` to the logs, we could pass it as a parameter:

```scala
def req(requestId: String, userName: String): Future[Unit] = Future {
  logger.info(s"$requestId: Received a request to create a user $userName")
  // business logic
}.flatMap(_ => registerUser(requestId, userName))

def registerUser(requestId: String, name: String): Future[Unit] = Future {
  // business logic
  logger.info(s"$requestId: Registering a new user named $name")
}
```

The problem with this approach is that now we need to include the context in all logging methods. 
Depending on your preferences, you might be okay with it or consider it a different concern and would rather keep it away from business logic.

Let's take a look at why `MDC` with `ThreadLocal` doesn't solve the use case:

```scala
object LocalExample extends App with StrictLogging {
  implicit val ec = ExecutionContext.global

  def req(requestId: String, userName: String): Future[Unit] = Future {
    MDC.put("requestId", requestId)
    logger.info(s"Received a request to create a user $userName")
    // more flatmaps to add async boundaries
  }.flatMap(_ => Future(()).flatMap(_ => Future())).flatMap(_ => registerUser(userName))

  def registerUser(name: String): Future[Unit] = Future {
    // business logic
    logger.info(s"Registering a new user named $name")
  }

  val requests = List(req("1", "Clark"), req("2", "Bruce"), req("3", "Diana"))
  Await.result(Future.sequence(requests), Duration.Inf)
  
  //=> 3: Received a request to create a user Diana
  //=> 2: Received a request to create a user Bruce
  //=> 1: Received a request to create a user Clark
  //=> 1: Registering a new user named Clark
  //=> 2: Registering a new user named Bruce
  //=> 2: Registering a new user named Diana
}
```

As we can see in the snippet above, if concurrent operations are reusing the same threads, the proper context can be overwritten.

If we use Monix `Local` instead, we can make it work:

```scala
implicit val s = Scheduler.traced

// from https://github.com/mdedetrich/monix-mdc
MonixMDCAdapter.initialize()

def req(requestId: String, userName: String): Future[Unit] = Local.isolate {
  Future {
    MDC.put("requestId", requestId)
    logger.info(s"Received a request to create a user $userName")
    // more flatmaps to add async boundaries
  }.flatMap(_ => Future(()).flatMap(_ => Future())).flatMap(_ => registerUser(userName))
}

//=> 3: Received a request to create a user Diana
//=> 2: Received a request to create a user Bruce
//=> 1: Received a request to create a user Clark
//=> 1: Registering a new user named Clark
//=> 2: Registering a new user named Bruce
//=> 3: Registering a new user named Diana
```

## Integration with Future

`Local` works with `Future` if `monix.execution.TracingScheduler` is used as an `ExecutionContext`.

`Local`s are *shared* by default, meaning that in the following example:

```scala 
implicit val s = Scheduler.traced

val local = Local(0)

val f1 = Future {
  local := 200 + local.get * 2
}.map(_ => local.get)

val f2 = Future {
  local := 100 + local.get * 3
}.map(_ => local.get)
```

The results of `f1` and `f2` can vary depending on futures scheduling because they will modify the same variable.
For instance, `f1` might set `local` to `200`, then `f2` sets the `local` to `700` and both `f1` and `f2` return `700`.
If the ordering is different, `f2` could set the `local` to `100` and then read the same value, returning it before `f1` acts and completes with `400`.

In the case of `Future`, isolation needs to be explicit and is achieved with `Local.isolate`:

```scala 
implicit val s = Scheduler.traced

val local = Local(0)

val f1 = Local.isolate { 
  Future {
    local := 200 + local.get * 2
  }.map(_ => local.get)
}

val f2 = Local.isolate { 
  Future {
    local := 100 + local.get * 3
  }.map(_ => local.get)
}
```

Thanks to `isolate`, `f1` will always return `200`, and `f2` will return `100` because each `Future` will operate on a different copy of `Local`.

## Integration with Task

`Local` can be used with `Task` either with `Local` API, or `TaskLocal`, a purely functional interface built on top of impure `Local`.
In case isolation is needed, we need to use `TaskLocal.isolate` instead of `Local.isolate`. In other cases, it doesn't matter which one do we use.
It is perfectly acceptable to interact with the same `Local` in both `Future` and `Task`.

### How to enable

`Local` context propagation is disabled by default. There are several ways to enable it:

1. Use `TracingScheduler` as your `Scheduler`.
2. Apply `.executeWithOptions(_.enableLocalContextPropagation)` on `Task`
3. Use `runToFutureOpt` or similar with `Task.defaultOptions.enableLocalContextPropagation` in the implicit scope.
4. Set system property `monix.environment.localContextPropagation` to 1

The first option is recommended because any `Scheduler` will be lifted to `TracingScheduler` if context propagation is enabled.

### auto-isolation

Unlike `Future`, we don't always have to call `isolate`.

`Task` is automatically isolated when it is being run.

In the following example, the results will always be `200` and `100` respectively.

```scala 
implicit val s = Scheduler.traced

val local = Local(0)

val f1 = Task.evalAsync { 
  local := 200 + local.get * 2
}.map(_ => local.get).runToFuture

val f2 = Task.evalAsync {
  local := 100 + local.get * 3
}.map(_ => local.get).runToFuture
```

However, concurrent tasks in operators such as `parZip2` are not automatically isolated:

```scala
val local = Local(0)

val childTaskA = Task(local := 200)

val childTaskB = for {
  _ <- Task.sleep(100.millis)
  v1 <- Task(local.get)
  _ <- Task(local := 100)
} yield v1 + local.get

val task = Task.parZip2(childTaskA, childTaskB)
```

Since `local` is shared between `childTaskA` and `childTaskB`, the latter `Task` will complete with `300` due to earlier modification of `childTaskA`.

If that's not the desired behavior, we need to add `TaskLocal.isolate`:

```scala
val local = Local(0)

val childTaskA = Task(local := 200)

val childTaskB = for {
  v1 <- Task(local.get)
  _ <- Task.sleep(100.millis)
  _ <- Task(local := 100)
} yield v1 + local.get

val task = Task.parZip2(TaskLocal.isolate(childTaskA), TaskLocal.isolate(childTaskB))
```

And `childTaskB` will always return `100`.

Note that we have an [opened issue](https://github.com/monix/monix/issues/1302) about changing those operators to auto-isolate as well.

### runToFuture

`Task#runToFuture` isolates the `Local` automatically, but the isolated reference is kept in the `Future` continuation:

```scala 
implicit val s: Scheduler = Scheduler.Implicits.traced

val local = Local(0)

for {
  _  <- Task(local.update(1)).runToFuture
  value <- Future(local.get)
} yield println(s"Local value in Future $value")

println(s"Local value on the current thread = $value")

// => Local value on the current thread = 0
// => Local value in Future = 1
```

The current thread and other concurrent operations are not affected.

The purpose of this integration is to support seamless interop of `Task` codebases with `Future` based libraries that want to interact with `Local`.
The example in the next section shows how the context can be written in `Task` via MDC and then read in Akka HTTP's Directive.

Note that there is a known corner case related to this feature. 
The isolated context is lost if the `Future` is wrapped into other implementation, such as [FastFuture](https://doc.akka.io/api/akka-http/current/akka/http/scaladsl/util/FastFuture.html).
Please, let us know if it's preventing any use cases to prioritize addressing this issue accordingly.

## Example Repository

Take a look at [example repository](https://github.com/Avasil/akka-monix-local-example/blob/master/src/main/scala/AkkaHTTPExample.scala) 
to see how `Local` can be integrated with [Akka HTTP](https://doc.akka.io/docs/akka-http/current/index.html) 
and MDC when most of the application is written using Monix Task.

Note that the context is written in `Task,` but it is read in `Directive`, operating on a `Future`.

Eventually, we will provide high-level libraries that will handle the low-level details, and `Local` usage will be transparent to the user.

## Limitations

A major design constraint is that we want to support both `Future` and `Task`.
Since we don't have any access to `Future` implementation, we decided to build `Local` on top of `ThreadLocal`.
In some cases, it results in unfortunate consequences that we are yet to address appropriately.

Consider the following code:

```scala 
implicit val ec = Scheduler.traced

val local = Local(0)

def blackbox: Future[Unit] = {
  val p = Promise[Unit]()
  new Thread {
    override def run(): Unit = {
      Thread.sleep(100)
      p.success(())
    }
  }.start()
  p.future
}

val f = Local.isolate {
  for {
    _ <- Future { local := local.get + 100 }
    _ <- blackbox
    _ <- Future { local := local.get + 100 }
  } yield println(local.get) 
}

Await.result(f, Duration.Inf)

// => 100
```

The initial value of `local` is `0`.
Then in an isolated block, it is modified to `100`.
Next, we call the `blackbox` method that is spinning its own `Thread,` but it doesn't interact with `Local` whatsoever.
After `blackbox`, we add `100` to the current `local` value that we expect to be `100`.
However, it wasn't `100`; it was `0`! Why?

When `new Thread` is created, it doesn't know about the isolated `Local` reference. 
It uses the main one that wasn't modified.
For this reason, we have to isolate this call, so it doesn't affect the rest of the `Future` chain with `Local.isolate(blackbox)`.

This corner case applies whenever we interact with a code that handles concurrency outside of `TracingScheduler`, or `Task`.
Until this is solved, look at `Local` as a low-level mechanism.