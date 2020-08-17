#!/usr/bin/env bash

###############################################################################
# Utilities for all scripts, including project directory stuff, etc


# DIRECTORY MAPPING

# This one liner gets the directory containing this source file. Taken from:
#     https://stackoverflow.com/questions/59895/how-to-get-the-source-directory-of-a-bash-script-from-within-the-script-itself

# This scripts directory
export SCRIPTS="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
export BASE=$(realpath "$SCRIPTS/../..")
export FRAMEWORK=$(realpath "$SCRIPTS/..")

# Installed LIBS
export LIB=$(realpath "$FRAMEWORK/lib")

# Cloned Repositories
export REPOS=$(realpath "$FRAMEWORK/repos")

# Cornelius crate
export CORNELIUS=$(realpath "$BASE/cornelius")
# Serializer repo
export SERIALIZER_SRC="$BASE/serializer"

# LIB Projects
export REGULARIZER="$REPOS/ast-regularizer"
export MAJOR="$REPOS/major"

# JARs in LIB
export REGULARIZE_JAR="$LIB/ast-regularizer.jar"
export MAJOR_JAR="$LIB/major.jar"
export SERIALIZE_JAR="$LIB/serialization.jar"

# URLS
export REGULARIZER_REPO="https://github.com/bkushigian/ast-regularizer"
export MAJOR_REPO="https://github.com/bkushigian/major.git"
export TCE_REPO="https://gitlab.cs.washington.edu/benku/tce.git"
export SOOT_JAR_URL="https://repo1.maven.org/maven2/ca/mcgill/sable/soot/4.1.0/soot-4.1.0-jar-with-dependencies.jar"


function PUSHD {
    pushd "$1" > /dev/null
}

function POPD {
    popd > /dev/null
}


################################################################################
# Shallow clone a repo to the LIB directory
#
function clone_repo_to_lib {
    yellow "$(bold "Cloning repo $1")"
    PUSHD "$REPOS"
    git clone --depth 1 --single-branch "$1"
    POPD
}

function download_lib {
    PUSHD "$LIB"
    wget "$1"
    POPD
}

function install_library {
    src=$(realpath "$1")
    trg="$2"

    if [ -z "$1" ]
    then
        echo "Error: No source provided to install_library"
        exit 1
    fi

    if [ -z "$2"  ]
    then
        trg="$LIB"
    fi

    yellow "Installing $src --> $trg"

    if [ ! -e "$LIB" ]
    then
        yellow "Making $LIB"
        mkdir -p "$LIB"
    fi

    mv "$src" "$trg"
}

################################################################################
# Pretty printing functions
#
function bold {
    printf "\033[1${2}m$1\033[0m\n"
}

function red {
    printf "\033[31${2}m$1\033[0m\n"
}

function green {
    printf "\033[32${2}m$1\033[0m\n"
}

function yellow {
    printf "\033[33${2}m$1\033[0m\n"
}

function blue {
    printf "\033[33${2}m$1\033[0m\n"
}
