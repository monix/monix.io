---
layout: docs
title: Presentations
---

Featured:

<iframe width="640" height="360" 
  src="https://www.youtube-nocookie.com/embed/JFbYQGG2Nb4" frameborder="0" 
  class="presentation" allow="autoplay; encrypted-media" allowfullscreen>
</iframe>

## Conferences

- [A Tale of Two Monix Streams](./2017-tale-two-monix-streams.html)
  by Alexandru Nedelcu, [Scala World 2017](https://scala.world/)
- [Monix Task: Lazy, Async &amp; Awesome](./2017-task-scaladays.html)
  by Alexandru Nedelcu, [Scala Days 2017](https://scaladays.org/archive/copenhagen2017.html)
- [Monix Task: Lazy, Async &amp; Awesome](./2016-task-flatmap-oslo.html)
  by Alexandru Nedelcu, at [flatMap(Oslo) 2016](http://2016.flatmap.no/)
- [Akka &amp; Monix: Controlling Power Plants](./2016-akka-monix-typelevel.html)
  by Alexandru Nedelcu, at [flatMap(Oslo) 2016](http://2016.flatmap.no/)

## Video Tutorials

{% for post in site.posts -%}{% if post.video %}
- [{{ post.title }}]({{ post.url }}) ({{ post.date | date: "%b %Y" }}) by [@{{ post.author }}](https://twitter.com/{{ post.author }})
{% endif %}{% endfor %}

## Related

- [The Making of an IO](https://www.youtube.com/watch?v=g_jP47HFpWA) 
  by Daniel Spiewak, at [ScalaIO 2017](https://scala.io/2017/)
- [What Referential Transparency can do for you](https://www.youtube.com/watch?v=X-cEGEJMx_4) by Luka Jacobowitz, at [ScalaIO 2017](https://scala.io/2017/)
- [Free as in Monads](https://www.youtube.com/watch?v=aKUQUIHRGec)
  by Daniel Spiewak, at [NEScala 2017](http://www.nescala.org/2017)
  
Add your own ;-)
