# Monix.io

The homepage and the documentation website for the Monix project.

[![Build status](https://github.com/monix/monix.io/workflows/build/badge.svg)](https://github.com/monix/monix.io/actions?query=workflow%3Abuild)

## Developing Locally

The website gets generated with [Jekyll](https://jekyllrb.com/) and articles are type-checked with [mdoc](https://github.com/scalameta/mdoc). In order to install the required dependencies:

1. make sure you have a recent Ruby version installed, see for example [rbenv](https://github.com/rbenv/rbenv) for managing Ruby versions, but whatever you can install through your OS's package manager will probably do
2. install [bundler](https://bundler.io/)
3. make sure you have at least Java 8 installed; for managing multiple Java versions see [jenv](http://www.jenv.be/)
4. install [sbt](https://www.scala-sbt.org/)

Then to install the Ruby dependencies of the project:

```
bundle
```

Then to generate the whole website:

```
./script/build
```

### Incremental compilation

To generate the `mdoc`-enabled articles, which takes articles from [./_docs](./_docs), generating them parsed into `./docs`:

```
sbt mdoc
```

You can also watch for changes and do incremental compilation:

```
sbt mdoc --watch
```

You can also generate the docs for a specific version only, e.g. 2.x vs 3.x, since the versions are described as separate sub-modules:

```
sbt docs3x/mdoc --watch
```

To serve the website locally and see what it looks like:

```
bundle exec jekyll serve
```

To build the final website:

```
bundle exec jekyll build
```

N.B. the `sbt mdoc` step does not happen automatically, that's a separate step that needs to be execute as shown above.
