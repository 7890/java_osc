#!/usr/bin/env bash

set -euo pipefail

if [ "$TRAVIS_OS_NAME" == "linux" ]; then
# autotools, automake, make are present in the trusty image
  sudo apt-get install -y \
    openjdk-7-jdk
fi

exit 0
