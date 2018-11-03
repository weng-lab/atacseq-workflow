#
# Dockerfile for the pool_ta step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
# encode_common modified to mv files rather than hard link
#

FROM alpine:3.8

# python
RUN apk add --no-cache python

# python scripts
RUN mkdir /app
COPY src/encode_pool_ta.py /app/encode_pool_ta.py
COPY src/encode_common.py /app/encode_common.py

