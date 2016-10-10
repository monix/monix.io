#!/bin/bash
set -e

# Change to monix.io directory
cd `dirname $0`

# Cleanup
rm -rf docs && rm -rf _site
# Compiling tut files
sbt run
# Compiling Jekyll website
jekyll b

# Sync everything
rsync -rcv --delete-excluded _site/ alexn.org:/var/www/monix.io/
