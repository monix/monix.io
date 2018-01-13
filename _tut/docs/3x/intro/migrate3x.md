---
layout: docs3x
title: Migrating from 2.x to 3.x
description: "Guide on migrating from version 2.x to version 3.x"
---

## Types renamed

- `monix.execution.schedulers.ExecutionModel` -> `monix.execution.ExecutionModel`

## Typelevel Cats

The optional `monix-cats` dependency is no longer required or available.
At the moment of writing, Monix directly depends on `cats-core` 
and `cats-effect`.