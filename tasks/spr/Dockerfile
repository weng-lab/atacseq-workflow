#
# Dockerfile for the pool_ta step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
# encode_common modified to mv files rather than hard link
#

FROM atacseq-common:1.0.0 as common

FROM ubuntu:18.04

RUN apt-get update && apt-get install -y \
    libncurses5-dev libcurl4-openssl-dev libfreetype6-dev zlib1g-dev \
    ghostscript pkg-config libboost-dev \
    python3 wget \
    && ln -s /usr/bin/python3 /usr/bin/python && \
    wget https://github.com/openssl/openssl/archive/OpenSSL_1_0_2t.tar.gz && tar zxvf OpenSSL_1_0_2t.tar.gz && cd openssl-OpenSSL_1_0_2t/ && ./config && make && make install && cd ../ && rm -rf openssl-OpenSSL_1_0_2t* && rm /usr/bin/openssl && ln -s /usr/local/ssl/bin/openssl /usr/bin/openssl && \
    rm -rf /var/lib/apt/lists/*

# scripts
COPY src/ /app

# common scripts
COPY --from=common /app/common /app/common
