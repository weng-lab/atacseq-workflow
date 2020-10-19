#
# Dockerfile for the Bowtie2 step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
# encode_common modified to mv files rather than hard link
#

FROM atacseq-common:1.0.0 as common

FROM ubuntu:18.04

# scripts
COPY src/ /app

# common scripts
COPY --from=common /app/common /app/common

# bowtie2 and samtools
RUN mkdir /software
WORKDIR /software
ENV PATH="/software:${PATH}"
RUN apt-get update && apt-get install -y \
    libncurses5-dev libcurl4-openssl-dev libfreetype6-dev \
    gcc perl zlib1g-dev libbz2-dev liblzma-dev \
    python3 python3-setuptools python3-pip \
    git wget unzip ghostscript pkg-config libboost-dev \
    && \
    wget https://github.com/openssl/openssl/archive/OpenSSL_1_0_2t.tar.gz && tar zxvf OpenSSL_1_0_2t.tar.gz && cd openssl-OpenSSL_1_0_2t/ && ./config && make && make install && cd ../ && rm -rf openssl-OpenSSL_1_0_2t* && rm /usr/bin/openssl && ln -s /usr/local/ssl/bin/openssl /usr/bin/openssl && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    wget https://github.com/BenLangmead/bowtie2/releases/download/v2.3.4.3/bowtie2-2.3.4.3-linux-x86_64.zip && \
    unzip bowtie2-2.3.4.3-linux-x86_64.zip && mv bowtie2*/bowtie2* . && rm -rf bowtie2-2.3.4.3* && \
    git clone --branch 1.9 --single-branch https://github.com/samtools/samtools.git && \
    git clone --branch 1.9 --single-branch https://github.com/samtools/htslib.git && \
    cd samtools && make && make install && cd ../ && rm -rf samtools* htslib* && \
    rm -rf /var/lib/apt/lists/*
