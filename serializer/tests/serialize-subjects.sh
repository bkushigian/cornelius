#!/usr/bin/env bash
set -euo pipefail

# This file assumes that:
# 1. The serializer has been compiled to $serializater/target
# 2. Mutation has already happend (e.g., mutants/ and mutants.log are present)
# 3. That there is a single java file in the subject directory

die () {
    echo "Die: $1"
    exit 1
}

TESTS="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
SERIALIZER=$(realpath "$TESTS/..")
TARGET="$SERIALIZER/target"
ROOT=$(realpath "$SERIALIZER/..")
TESTS_OUTPUT="$ROOT/tests"

SERIALIZER_JAR=$(find "$TARGET" -name "serialization-*-jar-with-dependencies.jar")
[ "$(echo "$SERIALIZER_JAR" | wc -l)" -eq 1 ] || die "Found multiple jar files: $SERIALIZER_JAR"
SERIALIZER_JAR=$(realpath "$SERIALIZER_JAR")
cd "$TESTS/subjects"


while read -r line
do
    subject="$(echo "$line " | cut -d' ' -f1)"
    java_file="$(echo "$line " | cut -d' ' -f2)"
    if [ -z "$java_file" ]
    then
        echo "No default java file provided: searching for a java file"
        java_file=$(find "$subject" -name "*.java" -d 1)
    else
        java_file="$subject/$java_file"
    fi

    echo
    echo "Line: '$line'"
    echo "Subject: $subject"
    echo "Java file: $java_file"
    echo

    if [ "$(echo "$java_file" | wc -l)" -eq 1 ]
    then
        java_file="$(realpath "$java_file")"
        cd "$subject"
        if [ -e cornelius ]
        then
            rm -rf cornelius/
        fi
        java -cp "$SERIALIZER_JAR" serializer.Serializer mutants.log mutants/ "$java_file"
        cp cornelius/serialize/subjects/*.cor "$TESTS_OUTPUT/$subject.xml"
        cd ..
    fi
done < ../test-subjects.txt
