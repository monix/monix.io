---
layout: docs
title: Javascript Event Listeners
play_url: "https://scalafiddle.io/sf/0tkpBUS/10"
description: |
  How do you convert Javascript event listeners to Observable?
---

A common problem on the client side, when working with Javascript, is
dealing with event listeners. How do you convert a Javascript event
listener to an `Observable`?

It's simple:

```scala
import org.scalajs.dom.{Event, EventTarget}
import monix.execution.{Cancelable,Ack}
import monix.reactive.Observable
import monix.reactive.OverflowStrategy.Unbounded

def eventListener(target: EventTarget, event: String): Observable[Event] =
  Observable.create(Unbounded) { subscriber =>
    val c = SingleAssignmentCancelable()
    // Forced conversion, otherwise canceling will not work!
    val f: js.Function1[Event,Ack] = (e: Event) =>
      subscriber.onNext(e).syncOnStopOrFailure(c.cancel())
    
    target.addEventListener(event, f)
    c := Cancelable(() => target.removeEventListener(event, f))
  }
```

Then you can subscribe to it and manipulate it, like any other
`Observable`:

```scala
import org.scalajs.dom
import org.scalajs.dom.MouseEvent
import monix.execution.Scheduler.Implicits.global
import concurrent.duration._

val conn = eventListener(dom.window, "mousemove")
  .collect { case e: MouseEvent => e }
  .sampleRepeated(1.second)
  .foreach(e => println(s"Mouse: ${e.screenX}, ${e.screenY}"))
  
// Canceling in 20 seconds!
dom.window.setTimeout(
  () => { println("Canceling!"); conn.cancel() },
  20000
)
```

Above I'm using `sampleRepeated` for effect. Of special interest for
Javascript developers is `debounce`, try it out.

Note that I'm using
[scala-js-dom](https://github.com/scala-js/scala-js-dom), which is a
small library providing static types for your DOM. But you could use
just `scala.scalajs.js.Dynamic`, doesn't really matter.