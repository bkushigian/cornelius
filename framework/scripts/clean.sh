#!/usr/bin/env bash

source util.sh

red "$(bold "Cleaning Repositories")"
PUSHD "$REPOS"
for repo in $(ls); do
    bold   "    removing $repo"
    rm -rf "$repo"
done
POPD

PUSHD "$LIB"
red "$(bold "Cleaning Libraries")"
rm -rf "$LIB/*"
for lib in $(ls); do
    bold   "    removing $lib"
    rm -rf "$lib"
done
POPD
