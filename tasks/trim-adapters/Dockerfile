#
# Dockerfile for the trim adapters step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
# encode_common and encode_trim_adapter modified to mv files rather than hard link
#

FROM atacseq-common:1.0.0 as common

FROM alpine:3.8

# scripts
COPY src/ /app

# common scripts
COPY --from=common /app/common /app/common

# cutadapt
RUN apk add --no-cache bash python3 python3-dev build-base py3-pip py3-setuptools && python3 -m pip install cutadapt && apk del py3-pip build-base 

