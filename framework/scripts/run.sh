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
green "$(bold "Equiv Classes:")"

equiv_classes="$tmp/equiv-classes"
mkdir "$equiv_classes"
bold "Writing equivalence classes to $(green "$equiv_classes"):"
for file in $(ls "${base%.*}"*".equiv-class")
do
    mv "$file" $tmp/equiv-classes
    bold "    $tmp/$(green "equiv-classes/$file")"
    # echo "$(cat "$tmp/$file")"
done

echo "base: $BASE"
linked_equiv_classes="$BASE/${base%.*}-equiv-classes"
bold "Linking to equivalence classes: $(green "$linked_equiv_classes")"

if [ -e "$linked_equiv_classes" ]
then
    echo "Removing old link"
    rm "$linked_equiv_classes"
fi

ln -s "$equiv_classes" "$linked_equiv_classes"
