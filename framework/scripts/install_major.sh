#!/usr/bin/env bash

source util.sh

################################################################################
# Install the Major mutation framework to /framework/lib, cloning the repo if
# necessary.
#

if [ ! -d "$MAJOR" ]
then
    clone_repo_to_lib $MAJOR_REPO
fi
PUSHD "$MAJOR/mutator"
./gradlew assemble
install_library "build/libs/major.jar" "$LIB/major.jar"
POPD
