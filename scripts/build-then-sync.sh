#!/usr/bin/env bash

set -e

cd `dirname $0`/..

# Cleanup
rm -rf docs && rm -rf _site

# Compiling tut files
sbt run

# Compiling Jekyll website
bundle exec jekyll build

# Copying SSH key
mkdir -p $HOME/.ssh
cp scripts/travis_rsa $HOME/.ssh
chmod -R go-rwx $HOME/.ssh

rsync --delete-excluded -Pacv \
  -e "ssh -o 'StrictHostKeyChecking no' -i $HOME/.ssh/travis_rsa" \
  _site/ alex@alexn.org:/var/www/monix.io/
