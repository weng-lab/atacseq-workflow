#
# Dockerfile for the MACS2 step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
#

FROM ubuntu:18.04

# macs2 binaries and prerequisites
RUN apt-get update && apt-get -y install python python-pip
RUN pip install --no-cache-dir numpy
RUN pip install --no-cache-dir macs2

# scripts
COPY src/encode_common.py /app/encode_common.py
COPY src/encode_common_genomic.py /app/encode_common_genomic.py
COPY src/encode_blacklist_filter.py /app/encode_blacklist_filter.py
COPY src/encode_frip.py /app/encode_frip.py
COPY src/encode_macs2_atac.py /app/encode_macs2_atac.py
