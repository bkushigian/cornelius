#!/usr/bin/env bash

# Check that everything we need is installed

source framework/scripts/util.sh
for lib in "framework/lib/ast-regularizer.jar" "framework/lib/serialization.jar" "framework/lib/major.jar"
do
    if [ ! -e "$lib" ]
    then
        red "$(bold "Error: Missing Library $lib")"
        echo "       Have you run \`$(bold init.sh)\`?"
        exit 1
    fi
done

# Run!

target="$(realpath "$1")"
cd framework/scripts
if [[ -f "$target" ]]
then
    ./run.sh "$target"
elif [[ -d "$target" ]]
then
    for f in $(ls "$target/"*.cor)
    do
        echo $f
        ./run.sh "$target/$f"
    done
fi
