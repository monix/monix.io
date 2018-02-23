# Monix.io

The homepage and the documentation website for the Monix project.

[![Travis](https://img.shields.io/travis/monix/monix.io.svg)](https://travis-ci.org/monix/monix.io)

## Generating the Website Locally

The website gets generated with [Jekyll](https://jekyllrb.com/) and articles are type-checked with [tut](https://github.com/tpolecat/tut). In order to install the required dependencies:

1. make sure you have a recent Ruby version installed, see for example [RVM](https://rvm.io/) for managing Ruby versions, but whatever you can install through your OS's package manager will probably do
2. install [bundler](https://bundler.io/)
3. make sure you have at least Java 8 installed; for managing multiple Java versions see [jenv](http://www.jenv.be/)
4. install [sbt](https://www.scala-sbt.org/)

Then to install the Ruby dependencies of the project:

```
bundle
```

To generate the `tut`-enabled articles, which takes articles from `./_tut` and drops them parsed in `./docs`:

```
sbt -J-Xmx4096m -J-XX:MaxMetaspaceSize=2048m run
```

N.B. the memory settings are there because we are downloading and loading up specific library dependencies per article, which can yield "metaspace" problems.

To serve the website locally and see what it looks like:

```
bundle exec jekyll serve
```

To build the final website:

```
bundle exec jekyll
```

N.B. the `tut` / `sbt` step does not happen automatically, that's a separate step that needs to be execute as shown above.
