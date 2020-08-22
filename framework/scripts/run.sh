#!/usr/bin/env bash

source util.sh

if [ $# != 1 ]
then
    red "$(bold "usage: ./run.sh file")"
    exit 1
fi

dir="$(realpath "$(dirname $1)")"
base="$(basename "$1")"
tmp=$(mktemp -d -t cornelius-)
green "Created temp working directory $(yellow $tmp)"
cp "$1" "$tmp"

if [ -z "$MML" ]
then
    MML="$dir/mml/all.mml.bin"
    if [ ! -e "$MML" ]
    then
        red "$(bold "ERROR: No MML env variable specified, and no mml/all.mml.bin directory found in $dir")"
        red         "       Please specify MML location. Aborting "
        exit 1
    fi
fi

export MML
./mutate.sh "$tmp/$base"
./regularize.sh --subject "$tmp/$base"
./serialize.sh "$tmp/regularized/$base"
xml="$tmp/${base%.*}.xml"
mv subjects.xml "$xml"
echo "Serialized subjects file: $xml"
./cornelius.sh "$xml"
equiv_classes="$xml.equiv-class"
green "$(bold "Equiv Classes: $(blue "$equiv_classes")")"
echo "$(cat "$equiv_classes")"
