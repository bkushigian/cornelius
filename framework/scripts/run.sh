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
echo "Created temp working directory $(green $tmp)"
cp "$1" "$tmp"

if [ -z "$MML" ]
then
    MML="$BASE/mml/all.mml.bin"
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

equiv_classes="$tmp/equiv-classes"
mkdir "$equiv_classes"
echo "Writing equivalence classes to $(green "$equiv_classes"):"
for file in $(ls "${base%.*}"*".equiv-class")
do
    mv "$file" $tmp/equiv-classes
    echo "    $tmp/$(green "equiv-classes/$file")"
    # echo "$(cat "$tmp/$file")"
done

linked_equiv_classes="$BASE/${base%.*}-equiv-classes"


if [ -e "$linked_equiv_classes" ]
then
    echo "Removing old link"
    rm "$linked_equiv_classes"
fi

echo
echo
echo "Working Directory ......... $(green "$tmp")"
echo "Generated mutants ......... $(green "$tmp/mutants")"
echo "Serialized ................ $(green "$xml")"
echo "Equivalence classes ....... $(green "$linked_equiv_classes")"

ln -s "$equiv_classes" "$linked_equiv_classes"
