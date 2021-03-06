#!/usr/bin/env bash

source util.sh

function serialize_subject {

    echo "================================================================================"
    echo "Serializing subject $1/$2"

    base="$1"
    file="$2"

    java -cp "$SERIALIZE_JAR" serializer.peg.PegSubjectSerializer "$base" "$file"
}

dir="$(realpath "$(dirname "$1")")"
base="$(basename "$1")"
serialize_subject "$dir" "$base"
