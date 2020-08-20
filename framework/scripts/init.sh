#!/usr/bin/env bash

################################################################################
# Initialize components of Cornelius

source util.sh

if [ ! -e "$FRAMEWORK" ]
then
    mkdir -p "$FRAMEWORK"
fi

if [ ! -e "$LIB" ]
then
    mkdir -p "$LIB"
fi

if [ ! -e "$REPOS" ]
then
    mkdir -p "$REPOS"
fi

./install_all.sh
cd "$BASE/cornelius"
cargo build --release
