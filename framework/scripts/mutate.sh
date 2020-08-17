#!/usr/bin/env bash

source util.sh

echo "major: $MAJOR_JAR"

if [ -z "$MML" ]
then
  MML="mml/all.mml.bin"
fi

function generate_mutants {
  dir=$(realpath "$1")
  java_file="$2"
  echo "Generating mutants for $dir/$java_file"
  PUSHD "$dir"
  rm -rf mutants mutants.log major.log
  javac -Xplugin:"MajorPlugin mml:$MML export.mutants" -cp "$MAJOR_JAR" "$java_file"
  POPD
}


dir=$(dirname "$1")
base=$(basename "$1")
generate_mutants "$dir" "$base"
