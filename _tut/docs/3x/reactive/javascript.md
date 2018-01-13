---
layout: docs
title: Javascript Event Listeners
description: |
  How do you convert Javascript event listeners to Observable?
---

A common problem on the client side, when working with Javascript, is
dealing with event listeners. How do you convert a Javascript event
listener to an `Observable`?

It's simple, as seen in the example below. Then you can subscribe to 
it and manipulate it, like any other `Observable`

{% scalafiddle %}
```scala
import org.scalajs.dom
import org.scalajs.dom.{Event, EventTarget, MouseEvent}
import monix.execution.Scheduler.Implicits.global
import monix.execution.{Cancelable, Ack}
import monix.execution.cancelables.SingleAssignmentCancelable
import monix.reactive.Observable
import monix.reactive.OverflowStrategy.Unbounded
import concurrent.duration._

def eventListener(target: EventTarget, event: String): Observable[Event] =
  Observable.create(Unbounded) { subscriber =>
    val c = SingleAssignmentCancelable()
    // Forced conversion, otherwise canceling will not work!
    val f: scalajs.js.Function1[Event,Ack] = (e: Event) =>
      subscriber.onNext(e).syncOnStopOrFailure(_ => c.cancel())
    
    target.addEventListener(event, f)
    c := Cancelable(() => target.removeEventListener(event, f))
  }

val conn = eventListener(dom.window, "mousemove")
  .collect { case e: MouseEvent => e }
  .sampleRepeated(1.second)
  .foreach(e => println(s"Mouse: ${e.screenX}, ${e.screenY}"))

println("Started! Move mouse over the bottom panel!")
  
// Canceling in 20 seconds!
dom.window.setTimeout(
  () => { println("Canceling!"); conn.cancel() },
  20000
)
```
{% endscalafiddle %}

Click the "Run" button to try out the example in your browser!

Above I'm using `sampleRepeated` for effect. Of special interest for
Javascript developers is `debounce`, try it out.

Note that I'm using
[scala-js-dom](https://github.com/scala-js/scala-js-dom), which is a
small library providing static types for your DOM. But you could use
just `scala.scalajs.js.Dynamic`, doesn't really matter.