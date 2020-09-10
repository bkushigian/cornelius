#!/usr/bin/env bash

source util.sh

################################################################################
# Print a usage screen with an optional message
function usage {
  printf "$(bold "usage:") ./mutate.sh path/to/File.java\n"
  if [ ! -z "$1" ]
  then
    printf "    $(bold "Reason: ")$1\n\n"
  fi
  printf "    This command invokes the Major Javac plugin\n"
  printf "\n"
  printf "$(bold "Environment Variables")\n"
  printf "%s\n" "---------------------"
  printf "   MML: path of the compiled MML file. Default: 'cornelius/mml/all.mml.bin'\n"
  printf "   MAJOR_JAR: path to the Major Javac Plugin jar file\n"
  exit 1
}

################################################################################
# Parse args/sanitize input
if [ "--help" == "$1" ]
then
  usage
fi

if [ -z "$1" ]
then
  usage "No argument provided"
fi

if [ -z "$MML" ]
then
  MML="$BASE/mml/all.mml.bin"
fi

if [ -z "$MAJOR_JAR" ]
then
   usage "MAJOR_JAR environment variable must be set"
fi

################################################################################
# Business logic
function generate_mutants {
  dir=$(realpath "$1")
  java_file="$2"
  echo "================================================================================"
  echo "Running Major to generate mutants for $dir/$java_file"
  PUSHD "$dir"
  rm -rf mutants mutants.log major.log
  $JAVA_HOME/bin/javac -Xplugin:"MajorPlugin mml:$MML export.mutants" -cp "$MAJOR_JAR" "$java_file"
  POPD
}

function ensure_java_8 {
  if ! $JAVA_HOME/bin/java -version 2>&1 | grep "1\.8\..*" >/dev/null
  then
    red "$(bold "Please make sure that JAVA_HOME points to a Java 1.8 installation")"
    echo "currently JAVA_HOME=$JAVA_HOME"
    exit 1
  fi
}

ensure_java_8
dir=$(dirname "$1")
base=$(basename "$1")
generate_mutants "$dir" "$base"
