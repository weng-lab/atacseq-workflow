#
# Dockerfile for the pool_ta step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
# encode_common modified to mv files rather than hard link
#

FROM atacseq-common:1.0.0 as common

FROM alpine:3.8

# python
RUN apk add --no-cache bash python3 && ln -s /usr/bin/python3 /usr/bin/python

# common scripts
COPY --from=common /app/common /app/common

# scripts
COPY src/ /app
