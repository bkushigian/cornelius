#!/usr/bin/env bash

################################################################################
# Install the serialization jar file to /framework/lib
#

source util.sh
PUSHD "$SERIALIZER_SRC"
mvn clean package
pwd
jar_file="$(find target -name "*-jar-with-dependencies.jar")"
install_library $jar_file "$LIB/serialization.jar"
POPD
