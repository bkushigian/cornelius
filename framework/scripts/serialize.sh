#!/usr/bin/env bash

source util.sh

function serialize_subject {

    base="$1"
    file="$2"

    java -cp "$SERIALIZE_JAR" serializer.peg.PegSubjectSerializer "$base" "$file"
}

dir="$(realpath "$(dirname "$1")")"
base="$(basename "$1")"
time serialize_subject "$dir" "$base"
