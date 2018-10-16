#
# Dockerfile for the alignment filtering step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
#

FROM ubuntu:18.04

# Picard jar and sambamba
RUN apt-get update && apt-get -y install wget default-jre sambamba
RUN wget https://github.com/broadinstitute/picard/releases/download/2.18.14/picard.jar -O /bin/picard.jar && chmod +x /bin/picard.jar
RUN apt-get -y remove wget && rm -rf /var/lib/apt/lists/*

# Python scripts
RUN mkdir /app
COPY src/encode_filter.py /app/encode_filter.py
COPY src/encode_common_genomic.py /app/encode_common_genomic.py
