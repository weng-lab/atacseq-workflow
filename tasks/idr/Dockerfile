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
    libbz2-dev liblzma-dev \
    && rm -rf /var/lib/apt/lists/* && \
    ln -s /usr/bin/python3 /usr/bin/python && \
   	wget http://hgdownload.soe.ucsc.edu/admin/exe/linux.x86_64/bigWigCorrelate -O /bin/bigWigCorrelate && chmod +x /bin/bigWigCorrelate && \
   	wget http://hgdownload.soe.ucsc.edu/admin/exe/linux.x86_64/bedClip -O /bin/bedClip && chmod +x /bin/bedClip && \
   	wget http://hgdownload.soe.ucsc.edu/admin/exe/linux.x86_64/bedGraphToBigWig -O /bin/bedGraphToBigWig && chmod +x /bin/bedGraphToBigWig && \
   	wget http://hgdownload.soe.ucsc.edu/admin/exe/linux.x86_64/bedToBigBed -O /bin/bedToBigBed && chmod +x /bin/bedToBigBed && \
    python3 -m pip install --no-cache-dir Cython numpy && \
    python3 -m pip install --no-cache-dir matplotlib && \
    git clone --branch 2.0.4.2 --single-branch https://github.com/kundajelab/idr && \
    cd idr && python3 setup.py install && cd ../ && rm -rf idr* && \
    git clone --branch v2.29.0 --single-branch https://github.com/arq5x/bedtools2.git && \
    cd bedtools2 && make && make install && cd ../ && rm -rf bedtools2* && \
    git clone --branch 1.9 --single-branch https://github.com/samtools/htslib.git && \
    cd htslib && make && make install && cd ../ && rm -rf htslib*

# common scripts
COPY --from=common /app/common /app/common

# scripts
COPY src/ /app
