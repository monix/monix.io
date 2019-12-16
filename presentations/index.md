---
layout: docs
title: Presentations
comments: true
---

{% include vimeo_thumb.html link="/presentations/2018-tale-two-monix-streams.html" uid="300052765" %}

## Conferences

- [Functional Concurrency in Scala 101](./2019-functional-concurrency-in-scala-101.html) (repeat with Japanese slides subtitles)<br />
  by Piotr Gawryś, [ScalaMatsuri 2019](https://2019.scalamatsuri.org/index_en.html)
- [Dispelling magic behind Concurrency in FP](./2019-dispelling-magic-behind-concurrency-in-fp.html)<br />
  by Piotr Gawryś, [flatMap(Oslo) 2019](https://2019.flatmap.no/)
- [Practical Reactive Streams with Monix](./2018-practical-reactive-streams-jacek-kunicki-sbtb.html)<br />
  by Jacek Kunicki, [Scale By The Bay 2018](https://scalebythebay2018.sched.com/)
- [Asynchronous Programming: Scala.concurrent and Monix! What?](./2018-async-programming-yifan-xing.html)<br />
  by Yifan Xing, [ReactSphere 2018](https://react.sphere.it/)
- [Practical Reactive Streams with Monix](./2018-practical-reactive-streams-jacek-kunicki.html)<br />
  by Jacek Kunicki, [ReactSphere 2018](https://react.sphere.it/)
- [A road trip with Monads: From MTL, through Tagless to Bio](./2018-road-trip-monads-mtl-tagless-bio.html)<br />
  by Paweł Szulc, [Scala in the City 2018](https://twitter.com/scalainthecity)
- [A Tale of Two Monix Streams](./2018-tale-two-monix-streams.html) (repeat)<br />
  by Alexandru Nedelcu, [Scala Days 2018](https://eu.scaladays.org/)
- [A Tale of Two Monix Streams](./2017-tale-two-monix-streams.html)<br />
  by Alexandru Nedelcu, [Scala World 2017](https://scala.world/)
- [Asynchronous processing with Monix](./2017-async-processing-monix-tomek-kogut.html)<br />
  by Tomek Kogut, at [ScalaWAW](https://www.meetup.com/ScalaWAW/)
- [Monix Task: Lazy, Async &amp; Awesome](./2017-task-scaladays.html) (repeat)<br />
  by Alexandru Nedelcu, [Scala Days 2017](https://scaladays.org/archive/copenhagen2017.html)
- [Monix Task: Lazy, Async &amp; Awesome](./2016-task-flatmap-oslo.html)<br />
  by Alexandru Nedelcu, at [flatMap(Oslo) 2016](http://2016.flatmap.no/)
- [Akka &amp; Monix: Controlling Power Plants](./2016-akka-monix-typelevel.html)<br />
  by Alexandru Nedelcu, at [flatMap(Oslo) 2016](http://2016.flatmap.no/)

## Video Tutorials

{% for post in site.posts -%}{% if post.vimeo %}
- [{{ post.title }}]({{ post.url }}) ({{ post.date | date: "%b %Y" }})
{% endif %}{% endfor %}

## Typelevel Ecosystem

{% include vimeo_thumb.html link="/presentations/2018-cancelable-io-typelevel-summit.html" uid="300067490" %}

Presentations on Cats and Cats-Effect, the sister projects 🙂

- [Cancelable IO](./2018-cancelable-io-typelevel-summit.html)<br/>
  by Alexandru Nedelcu, at [Typelevel Summit, 2018, Berlin](https://typelevel.org/event/2018-05-summit-berlin/)
- [The Making of an IO](https://www.youtube.com/watch?v=g_jP47HFpWA)<br/>
  by Daniel Spiewak, at [ScalaIO 2017](https://scala.io/2017/)
- [What Referential Transparency can do for you](https://www.youtube.com/watch?v=X-cEGEJMx_4)<br/>
  by Luka Jacobowitz, at [ScalaIO 2017](https://scala.io/2017/)
- [Free as in Monads](https://www.youtube.com/watch?v=aKUQUIHRGec)<br/>
  by Daniel Spiewak, at [NEScala 2017](http://www.nescala.org/2017)

Add your own ;-)
