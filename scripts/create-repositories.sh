#!/bin/bash
# Creates docker repositories under genomealmanac org that don't already exist
# Requires 'jq': https://stedolan.github.io/jq/
set -e

# cd to project root directory
cd "$(dirname "$(dirname "$0")")"

# import common stuff
source scripts/lib/common.sh

echo Please enter your docker hub username
read USERNAME

echo Please enter your docker hub password
read -s PASSWORD

TOKEN=$(curl -s -X POST \
    -H "Content-Type: application/json" \
    -d '{"username": "'${USERNAME}'", "password": "'${PASSWORD}'"}' \
    https://hub.docker.com/v2/users/login/ | jq -r .token)

for taskDir in pipeline/tasks/*/ ; do
    REPO_NAME="${IMAGE_PREFIX}-$(basename ${taskDir})"
    echo "Checking if ${REPO_NAME} exists..."
    EXISTS_RESPONSE=$(curl -s \
        -w "%{http_code}" -o /dev/null \
        -H "Authorization: JWT ${TOKEN}" \
        https://hub.docker.com/v2/repositories/${ORG}/${REPO_NAME}/?page_size=1)
    if [ $EXISTS_RESPONSE = "404" ]; then
        echo "Repo ${REPO_NAME} does not exist. Creating..."
        CREATE_RESPONSE=$(curl -s -X POST \
            -H "Authorization: JWT ${TOKEN}" \
            -H "Content-Type: application/json" \
            -d '{ "namespace": "'${ORG}'", "name": "'${REPO_NAME}'", "is_private": false }' \
            https://hub.docker.com/v2/repositories/)
        echo "Create Response: $CREATE_RESPONSE"
    elif [ $EXISTS_RESPONSE = "200" ]; then
        echo "Repo ${REPO_NAME} already exists"
    else
        echo "Error checking for existence of repo ${REPO_NAME}"
    fi
done

echo $RESPONSE