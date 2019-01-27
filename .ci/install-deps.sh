#!/usr/bin/env bash

set -euo pipefail

if [ "$TRAVIS_OS_NAME" == "linux" ]; then
# autotools, automake, make are present in the trusty image
# openjdk-8 available
  sudo apt-get install -y \
    junit4
fi

exit 0
