#
# Dockerfile for the MACS2 step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
#

FROM alpine:3.8

# scripts
COPY src/ /app

# macs2 binaries and prerequisites
RUN apk add --no-cache python build-base py-pip python-dev && \
    pip install --no-cache-dir numpy && \
    pip install --no-cache-dir macs2 && \
    apk del py-pip build-base python-dev
