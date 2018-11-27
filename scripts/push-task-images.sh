#!/bin/bash
# Pushes docker images for pipeline tasks if images for their Versionfile versions 
# don't exist remotely already.
# This does not build images. You will need to run scripts/build-task-images.sh first to build.
set -e

# cd to project root directory
cd "$(dirname "$(dirname "$0")")"

# import common stuff
source scripts/lib/common.sh

for taskDir in tasks/*/ ; do
    IMAGE_NAME="${ORG}/${IMAGE_PREFIX}-$(basename ${taskDir})"
    VERSION=$(cat ${taskDir}Versionfile)
    TAG=${IMAGE_NAME}:${VERSION}
    if docker pull ${TAG} >/dev/null 2>&1; then
        echo "${TAG} found remotely. Skipping push..."
    else
        echo "Pushing ${TAG}"
        docker push ${TAG}
    fi
done
