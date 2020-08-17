#!/usr/bin/env bash

source util.sh

function clean_old_data {
    PUSHD "$DATA"
    rm *.equiv
    POPD
}

function compile_binary {
    PUSHD "$CORNELIUS"
    cargo build --release
    POPD
}

function run_on_subject {
    subj="$1"
    xml="$XMLS/$subj.xml"
    PUSHD "$CORNELIUS"
    time "$cornelius" "$xml"
    mv "equiv-classes" "$BASE/data/$subj.equiv"
    POPD
}

function run_on_subjects {
    while IFS=, read -r name type directory filename
    do
      run_on_subject $name || echo "Error running on $name"
    done < "$SUBJECTS_CSV"
}

clean_old_data
compile_binary
cornelius=$(realpath "$CORNELIUS/target/release/cornelius")

if [ ! -z "$1" ]
then
    run_on_subject "$1"
else
    run_on_subjects
fi
