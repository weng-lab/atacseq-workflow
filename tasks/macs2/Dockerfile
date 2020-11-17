#
# Dockerfile for the MACS2 step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
# MUSL-compatible Kent utils from github.com/weng-lab/kent
#
# encode_macs2_atac modified to compute gensz automatically from chrsz, and to accept paired-end option for BEDPE calling
# encode_blacklist_filter and encode_common_genomic modified not to use grep -P (unsupported in alpine)
# "unusual" chromosomes will not be filtered; use a minimal chrom sizes file to remove these
#

FROM atacseq-common:1.0.0 as common

FROM ubuntu@sha256:d1d454df0f579c6be4d8161d227462d69e163a8ff9d20a847533989cf0c94d90

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y \
    libncurses5-dev libcurl4-openssl-dev libfreetype6-dev zlib1g-dev \
    python3 python3-setuptools python3-pip \
    git wget unzip ghostscript pkg-config libboost-dev \
    r-base libmysqlclient-dev \
    && rm -rf /var/lib/apt/lists/* && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    wget https://github.com/openssl/openssl/archive/OpenSSL_1_0_2t.tar.gz && \
    tar zxvf OpenSSL_1_0_2t.tar.gz && \
    cd openssl-OpenSSL_1_0_2t/ && ./config && make && make install && \
    cd ../ && rm -rf openssl-OpenSSL_1_0_2t* && rm /usr/bin/openssl && \
    ln -s /usr/local/ssl/bin/openssl /usr/bin/openssl && \
    git clone https://github.com/weng-lab/kent && \
    cd kent/src/lib && make CFLAGS=-DLIBUUID_NOT_PRESENT && cd ../jkOwnLib && make && cd ../htslib && make && \
    mkdir -p /root/bin/x86_64 && cd ../utils/bedClip && make && cd ../bedGraphToBigWig && make && \
    cd ../bedToBigBed && make && \
    cd / && rm -rf kent && mv /root/bin/x86_64/* /bin && \
    python3 -m pip install --no-cache-dir numpy && \
    python3 -m pip install --no-cache-dir Cython && \
    python3 -m pip install --no-cache-dir macs2==2.2.4 && \
    python3 -m pip install --no-cache-dir pandas matplotlib && \
    git clone https://github.com/arq5x/bedtools2 && cd bedtools2 && make && \
    mv bin/* /bin && cd .. && rm -rf bedtools2 && \
    git clone --branch 1.9 --single-branch https://github.com/samtools/samtools.git && \
    git clone --branch 1.9 --single-branch https://github.com/samtools/htslib.git && \
    cd samtools && make && make install && cd ../ && rm -rf samtools* && \
    cd htslib && make && make install && cd ../ && rm -rf htslib*

# common scripts
COPY --from=common /app/common /app/common

# scripts
COPY src/ /app
