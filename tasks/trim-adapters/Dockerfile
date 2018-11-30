#
# Dockerfile for the trim adapters step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
# encode_common and encode_trim_adapter modified to mv files rather than hard link
#

FROM alpine:3.8

# python scripts
COPY src/ /app

# cutadapt
RUN apk add --no-cache python python-dev build-base py-pip py-setuptools && pip install cutadapt && apk del py-pip build-base
