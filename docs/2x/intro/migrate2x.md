---
layout: docs
title: Migrating from 1.x to 2.x
description: "Guide on migrating from version 1.x (Monifu) to version 2.x"
---

Package renamings:

- `monifu.concurrent` -> `monix.execution`
- `monifu.reactive` -> `monix.reactive`

Moved types:

- `monifu.reactive.Ack` -> `monix.execution.Ack`
- `monifu.reactive.observers.SynchronousObserver` -> `monix.reactive.Observer.Sync`
- `monifu.reactive.observers.SynchronousSubscriber` -> `monix.reactive.observers.Subscriber.Sync`
- `monifu.reactive.Subscriber` -> `monix.reactive.observers.Subscriber`
- `monifu.concurrent.extensions` -> `monix.execution.FutureUtils.extensions`
- the `atomic.padded` package in `monifu.concurrent` is now gone, use
  the `Atomic.withPadding` constructor instead (default was `Right128`)
- `monifu.concurrent.async.AsyncQueue` is now `monix.execution.misc.AsyncQueue`  

Renamed operators:

- `Observable.lift` -> `Observable.transform`
- `Observable.onSubscribe` -> `Observable.unsafeSubscribeFn` (this one
  changed signature as well, it now returns a `Cancelable` instead of
  `Unit`)
- `Observable.create` -> `Observable.unsafeCreate` and there's a new
  `Observable.create` with different behavior
- `Ack.Cancel` -> `Ack.Stop`
- for the `FutureUtils` extensions `withTimeout` was renamed to
  `timeout` and `liftTry` to `materialize`, for consistency with
  `Task`
- `Observable.from` is `Observable.apply` and `Observable.unit` was
  renamed to `Observable.now`
- `Observable.whileBusyDropEvents(onOverflow)` renamed to
  `Observable.whileBusyDropEventsAndSignal`
- `Observable.toReactive` -> `toReactivePublisher`
- `Cancelable.apply` now wants a `Function0` and not a by-name parameter
- `Observable.doWork` -> `Observable.doOnNext`
- `Observable.whileBusyBuffer` no longer takes an `onOverflow` handler. The
  `onOverflow` handler is now a parameter of the `OverflowStrategy` (
    `DropNewAndSignal` and `DropOldAndSignal`)
- `Observable.buffer` -> `Observable.bufferTimedAndCounted`
- `Observable.asFuture` is to be done as `Observable.headL.runAsync`
- `Observable.count` is to be done as `Observable.fromTask(Observable.countL)`
- `Observable.fold` is to be done as `Observable.foldL`, now returning Task[T] not Future[Option[T]]

The `Channel` type is now gone, replaced by:

- `monix.reactive.Observer.Sync` for describing just
  synchronous input
- `monix.reactive.subjects.ConcurrentSubject` for describing
  synchronous input with attached output

`Future` no longer converts to `Observable` automatically. Use
`Observable.fromFuture`.
