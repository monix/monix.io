---
layout: post
title: "I'm Monix.io, Help Wanted!"
author: alexelcu
excerpt_separator: <!--more-->
description: We've come a long way and now the library is better than ever but there's much to do.
---

[Monix](https://github.com/monix/monix) was started in 2014 with the
purpose of aiding me in dealing with processing asynchronous events in
a soft real-time system. We've come a long way and now the library is
better than ever but there's much to do.

<!--more-->

At the time we were using [Akka Actors](http://doc.akka.io/) for
communications and it soon became apparent that it's not enough,
because often in such a system you've got signals coming from
multiple sources that need to be merged, state machines have to
be evolved and Akka Actors are too low level and too powerful,
easily leading to cycles in your communications graph, easily
making the flow so complex that it makes it hard to understand
what's going on. And another problem is that working with
Akka Actors efficiently must be done with the wisdom of others,
best practices, patterns that are sadly missing in the Scala
community.

At the time I looked for alternatives and stumbled upon
[ReactiveX / RxJava](http://reactivex.io/). A very solid library
built on top of a really cool model, but unfortunately at
that point we needed to handle back-pressure and the Scala
integration wasn't there and so at that point in time it
wasn't suitable for our needs. Even now their 1.x line has
limitations and design choices that I don't like. But granted,
it's a very solid library, I'm very grateful for their
work and I keep getting inspired by them. And I've also been
inspired by [Scalaz](https://github.com/scalaz/scalaz) and by
[Cats](https://github.com/non/cats). The community is simply
great and filled with awesome projects from which to draw
inspiration.

Monix has evolved with the help of myself and my colleagues
in response to the needs of the project we've been working on,
functioning in a very demanding environment. And now in version 2
it expanded beyond the `Observable` pattern, to provide other needed
abstractions like `Task`. It also works on top of [Scala.js](scala-js.org),
all of it. If something cannot be supported on top of Javascript engines,
then it won't make it in Monix. Monix's purpose is to make asynchronous
programming easier and  is build in an idiomatic Scala style. I was asked
for example if Monix supports Java and I said: *no, not at this point*.

And here we are. Monix has been my love child and I've loved every
minute of working on it. But it has to keep evolving. And one problem
it has is one of documentation. It has great API documentation, mind you,
but it needs more than that.

So here it is, the start of [Monix.io](https://monix.io), its documentation
website, which I hope to evolve into a useful resource.

**HELP WANTED:** if you like Monix, if you're using it, please help us
write some useful docs for it. You can start by being inspired from
[reactivex.io](http://reactivex.io/). Oh yes, the inspiration continues,
we are a big community ;-)
