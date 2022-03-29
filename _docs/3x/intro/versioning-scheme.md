---
layout: docs3x
title: Version Scheme
description: |
  Semantic versioning, with fast releases.
---

The versioning scheme follows the
[Semantic Versioning](http://semver.org/) (semver) specification,
meaning stable versions have the form `$major.$minor.$patch`,
such that:

1. `$major` version updates make binary incompatible API changes
2. `$minor` version updates adds functionality in a
   backwards-compatible manner, and
3. `$patch` version updates makes backwards-compatible bug fixes

Examples: 

- version `3.2.2` should only fix bugs from version `3.2.1`, but shouldn't introduce new features
- version `3.2.0` should introduce features over version `3.1.0`, features that are both binary and source compatible with the whole `3.x.x` series
- version `4.0.0`, when it happens, can introduce source and binary backwards-incompatible changes

## Intermediary Snapshots

We also publish intermediary versions, automatically, whenever PRs get merged to `main`.

The project has a dynamic version setup (see [sbt-dynver](https://github.com/dwijnand/sbt-dynver)), releases are via GitHub Actions with versions such as `3.2.2+3-1234abcd`, where `3.2.2` is the base version, `+3` represents the number of commits, the distance from that base version, and `1234abcd` represents the commit's "sha" (hash) prefix, identifying the commit in GitHub.

We make NO GUARANTEES for these intermediary versions, but they are pretty high quality, they get published on Maven Central too (NOT on Sonatype's snapshots, so these releases are not volatile), and you can depend on them for testing purposes, or if you really, really need a new feature that's not published in a stable version yet.
