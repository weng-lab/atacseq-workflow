#
# Dockerfile for the Bowtie2 step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
# encode_common and encode_trim_adapter modified to mv files rather than hard link
#

FROM ubuntu:18.04

# bowtie2 binaries and prerequisites
RUN apt-get update && apt-get -y install wget unzip python samtools perl
RUN wget https://github.com/BenLangmead/bowtie2/releases/download/v2.2.6/bowtie2-2.2.6-linux-x86_64.zip && \
    unzip bowtie2-2.2.6-linux-x86_64.zip && mv bowtie2*/bowtie2* /bin && rm -rf bowtie2-2.2.6*
RUN apt-get -y remove wget unzip && rm -rf /var/lib/apt/lists/*

# python scripts
RUN mkdir /app
COPY src/detect_adapter.py /app/detect_adapter.py
COPY src/encode_common.py /app/encode_common.py
COPY src/encode_common_genomic.py /app/encode_common_genomic.py
COPY src/encode_trim_adapter.py /app/encode_trim_adapter.py
COPY src/encode_bowtie2.py /app/encode_bowtie2.py
