#!/usr/bin/env bash

################################################################################
# Install AST regularization jar to /framework/lib
#

source util.sh

if [ ! -e "$REGULARIZER" ]
then
    clone_repo_to_lib $REGULARIZER_REPO
fi
PUSHD "$REGULARIZER"
./gradlew fatJar
install_library "build/libs/ast-regularizer-all-1.0-SNAPSHOT.jar" "$LIB/ast-regularizer.jar"
POPD
