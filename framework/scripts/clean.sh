#!/usr/bin/env bash

source util.sh


red "$(bold "Cleaning Repositories")"
rm -rf "$REPOS/*"

red "$(bold "Cleaning Libraries")"
rm -rf "$LIB/*"
