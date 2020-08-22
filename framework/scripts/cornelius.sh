#!/usr/bin/env bash

source util.sh

function compile_binary {
    PUSHD "$CORNELIUS"
    cargo build --release
    POPD
}

function run_on_subject {
    xml="$1"
    "$cornelius" "$xml"
}

compile_binary
cornelius=$(realpath "$CORNELIUS/target/release/cornelius")

run_on_subject "$1"
