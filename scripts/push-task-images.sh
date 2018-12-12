#!/bin/bash
# Pushes docker images for pipeline tasks if images for their Versionfile versions 
# don't exist remotely already.
# This does not build images. You will need to run scripts/build-task-images.sh first to build.
set -e

# cd to project root directory
cd "$(dirname "$(dirname "$0")")"

for taskDir in tasks/*/ ; do
    PUSH_IMAGE=true
    source $taskDir/docker-build-def.sh
    TAG=${IMAGE_NAME}:${IMAGE_VERSION}
    if [[ "$PUSH_IMAGE" = false ]]; then
        echo "PUSH_IMAGE disabled for ${IMAGE_NAME}"
    elif docker pull ${TAG} >/dev/null 2>&1; then
        echo "${TAG} found remotely. Skipping push..."
    else
        echo "Pushing ${TAG}"
        docker push ${TAG}
    fi
done
