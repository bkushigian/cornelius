#!/bin/bash

###############################################################################
# Regularize a file and its mutants
#
# Cornelius only works on Simple programs, and Simple programs have regular
# control flow (no early returns/breaks/continues/etc). This script transforms a
# subject and its mutants into reguarlized but equivalent versions and compiles
# them into classfiles (the classfiles are used by Medusa to compute ground
# truth).
#
# This outputs the regularized version of the program in `/path/to/subject/regularized`

source util.sh

################################################################################
# Print a usage screen with an optional message
function usage {
  printf "$(bold "usage:") $1\n"
  printf "$(bold Forms)\n"
  printf "$(bold -----)\n"
  printf "    ./regularize.sh --help: print this help message\n"
  printf "    ./regularize.sh --subject SUBJECT: regularize the subject whose original\n"
  printf "            source code is at path SUBJECT\n"
  printf "    ./regularize.sh --single-source SOURCE: regularize the SOURCE Java file\n"

  exit 1
}

###############################################################################
# regularize: given a source file $1 and a target file $2, regularize the file
# and save it to disk at location target.
#
# For instance, if the original file is `src/Foo.java` and you want to write a
# new file `regularized/src/Foo.java`, run
#
#     regularize "src/Foo.java" "regularized/src/Foo.java"
#
# Note that this will overwrite `regularized/src/Foo.java` if it exists without
# asking for permission.
function regularize {
    file="$1"
    target="$2"

    target_dir="$(dirname "$target")"
    if [ ! -e "$target_dir" ]
    then
      mkdir -p "$target_dir"
    fi

    java -jar "$REGULARIZE_JAR" "$file" > "$target"
}

###############################################################################
# A helper function that takes a mutant id and regularizes it to the new
# location
function regularize_mutant {
    base="$1"
    filename="$2"
    regdir="$3"
    mid="$4"
    mkdir -p "$regdir/mutants/$mid"
    source="$base/mutants/$mid/$filename"
    target="$regdir/mutants/$mid/$filename"
    regularize "$source" "$target"
    printf "."
}

function regularize_subject  {
    dir="$1"
    filename="$2"
    regdir=$(realpath "$dir/regularized")
    echo "================================================================================"
    echo "Regularizing subject $(green "$dir/$filename") to $(green "$dir/regularized")\n"

    if [ -e "$regdir" ]
    then
        rm -rf "$regdir"
    fi
    mkdir -p "$regdir"

    cp "$dir/mutants.log" "$regdir"

    regularize "$dir/$filename" "$regdir/$filename"

    # regularize the mutants
    for mid in $(ls "$dir/mutants")
    do
        regularize_mutant "$dir" "$filename" "$regdir" "$mid"
    done
    echo
}

while (( "$#" )); do
  case "$1" in
    --help)
      usage
      ;;
    --subject)
        shift
        if [ ! -e "$1" ]; then
          red "no such file as $1...aborting"
          exit 1
        fi
        dir="$(realpath "$(dirname "$1")")"
        base="$(basename "$1")"
        shift
        regdir="$dir/regularized"
        if [ -d "$dir" ]
        then
            if [ -e "$regdir"  ]
            then
                rm -rf "$regdir"
            fi
            mkdir -p "$regdir"
            regularize_subject "$dir" "$base"
        fi
      ;;
    *)
        if [ ! -e "$1" ]; then
          red "no such file as $1...aborting"
          exit 1
        fi
        dir="$(realpath "$(dirname "$1")")"
        base="$(basename "$1")"
        shift
        regdir="$dir/regularized"
        if [ -d "$dir" ]
        then
            if [ -e "$regdir"  ]
            then
                rm -rf "$regdir"
            fi
            mkdir -p "$regdir"
            regularize "$dir/$base" "$regdir/$base"
        fi
      ;;
  esac
done
