#!/bin/bash
# Builds docker images for pipeline tasks if images for their Versionfile versions don't exist 
# already and tags them with those versions.
set -e

# cd to project root directory
cd "$(dirname "$(dirname "$0")")"

# import common stuff
source scripts/lib/common.sh

for taskDir in pipeline/tasks/*/ ; do
    IMAGE_NAME="${ORG}/${IMAGE_PREFIX}-$(basename ${taskDir})"
    VERSION=$(cat ${taskDir}Versionfile)
    TAG=${IMAGE_NAME}:${VERSION}
    docker pull ${TAG} >/dev/null 2>&1 || true
    
    EXISTING_IMAGE=$(docker image ls ${TAG} --format '{{ .ID }}')
    if [ -z $EXISTING_IMAGE ]; then
        echo "Building ${TAG}..."
        docker build -t ${TAG} ${taskDir}
        echo "${TAG} build complete."
    else
        echo "${TAG} already exists. Skipping build."
    fi
done