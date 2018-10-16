#
# Dockerfile for the bam2ta (bam to tagAlign) step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
#

FROM ubuntu:18.04

# bedtools
RUN apt-get update && apt-get -y install bedtools
RUN rm -rf /var/lib/apt/lists/*

# Python scripts
RUN mkdir /app
COPY src/encode_bam2ta.py /app/encode_bam2ta.py
COPY src/encode_common_genomic.py /app/encode_common_genomic.py
