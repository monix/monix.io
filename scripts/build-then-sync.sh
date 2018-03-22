#!/usr/bin/env bash

set -e

cd `dirname $0`/..

# Compiling tut files
./scripts/sbt -J-XX:MetaspaceSize=2g -J-XX:MaxMetaspaceSize=2g -J-Xmx4g clean run

# Compiling Jekyll website
bundle exec jekyll build

# Copying SSH key
mkdir -p $HOME/.ssh
cp scripts/travis_rsa $HOME/.ssh
chmod -R go-rwx $HOME/.ssh

rsync --delete-excluded -Pacv \
  -e "ssh -p 223 -o 'StrictHostKeyChecking no' -i $HOME/.ssh/travis_rsa" \
  _site/ alex@alexn.org:/var/www/monix.io/
