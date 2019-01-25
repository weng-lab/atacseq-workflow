#!/bin/bash
# Builds docker images for pipeline tasks if images for their Versionfile versions don't exist 
# already and tags them with those versions.
set -e

# cd to project root directory
cd "$(dirname "$(dirname "$0")")"

for taskDir in tasks/*/ ; do
    source $taskDir/docker-build-def.sh
    TAG=${IMAGE_NAME}:${IMAGE_VERSION}
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
