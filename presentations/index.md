---
layout: docs
title: Presentations
comments: true
---

{% include vimeo_thumb.html link="/presentations/2018-tale-two-monix-streams.html" uid="300052765" %}

## Conferences

- [Asynchronous Programming: Scala.concurrent and Monix! What?](./2018-async-programming-yifan-xing.html)<br />
  by Yifan Xing, [ReactSphere 2018](https://react.sphere.it/){:target=>"_blank"}
- [Practical Reactive Streams with Monix](./2018-practical-reactive-streams-jacek-kunicki.html)<br />
  by Jacek Kunicki, [ReactSphere 2018](https://react.sphere.it/){:target=>"_blank"}
- [A road trip with Monads: From MTL, through Tagless to Bio](./2018-road-trip-monads-mtl-tagless-bio.html)<br />
  by Paweł Szulc, [Scala in the City 2018](https://twitter.com/scalainthecity){:target=>"_blank"}
- [A Tale of Two Monix Streams](./2018-tale-two-monix-streams.html) (repeat)<br />
  by Alexandru Nedelcu, [Scala Days 2018](https://eu.scaladays.org/){:target=>"_blank"}
- [A Tale of Two Monix Streams](./2017-tale-two-monix-streams.html)<br />
  by Alexandru Nedelcu, [Scala World 2017](https://scala.world/){:target=>"_blank"}
- [Asynchronous processing with Monix](./2017-async-processing-monix-tomek-kogut.html)<br />
  by Tomek Kogut, at [ScalaWAW](https://www.meetup.com/ScalaWAW/){:target=>"_blank"}
- [Monix Task: Lazy, Async &amp; Awesome](./2017-task-scaladays.html) (repeat)<br />
  by Alexandru Nedelcu, [Scala Days 2017](https://scaladays.org/archive/copenhagen2017.html){:target=>"_blank"}
- [Monix Task: Lazy, Async &amp; Awesome](./2016-task-flatmap-oslo.html)<br />
  by Alexandru Nedelcu, at [flatMap(Oslo) 2016](http://2016.flatmap.no/){:target=>"_blank"}
- [Akka &amp; Monix: Controlling Power Plants](./2016-akka-monix-typelevel.html)<br />
  by Alexandru Nedelcu, at [flatMap(Oslo) 2016](http://2016.flatmap.no/){:target=>"_blank"}
  
## Video Tutorials

{% for post in site.posts -%}{% if post.vimeo %}
- [{{ post.title }}]({{ post.url }}) ({{ post.date | date: "%b %Y" }})
{% endif %}{% endfor %}

## Typelevel Ecosystem

{% include vimeo_thumb.html link="/presentations/2018-cancelable-io-typelevel-summit.html" uid="300067490" %}

Presentations on Cats and Cats-Effect, the sister projects 🙂

- [Cancelable IO](./2018-cancelable-io-typelevel-summit.html)<br/>
  by Alexandru Nedelcu, at [Typelevel Summit, 2018, Berlin](https://typelevel.org/event/2018-05-summit-berlin/){:target=>"_blank"}
- [The Making of an IO](https://www.youtube.com/watch?v=g_jP47HFpWA)<br/>
  by Daniel Spiewak, at [ScalaIO 2017](https://scala.io/2017/){:target=>"_blank"}
- [What Referential Transparency can do for you](https://www.youtube.com/watch?v=X-cEGEJMx_4)<br/>
  by Luka Jacobowitz, at [ScalaIO 2017](https://scala.io/2017/){:target=>"_blank"}
- [Free as in Monads](https://www.youtube.com/watch?v=aKUQUIHRGec)<br/>
  by Daniel Spiewak, at [NEScala 2017](http://www.nescala.org/2017){:target=>"_blank"}
  
Add your own ;-)
