#!/bin/bash
# Builds the Krews workflow and runs it on google with the given configuration
# arg1: configuration file to run the workflow with
set -e

# Exit if one arg not given
if [[ $# -ne 1 ]]; then
    echo "One argument required.";
    exit;
fi

# cd to workflow directory
cd "$(dirname "$(dirname "$0")")/workflow"

./gradlew clean shadowJar
for JAR in build/*.jar; do :; done;
java -jar ${JAR} --on google --config ${1}