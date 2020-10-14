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

FROM alpine:3.8

# scripts
COPY src/ /app

# common scripts
COPY --from=common /app/common /app/common

# macs2 binaries and prerequisites
RUN apk add --no-cache xz-dev bzip2-dev git py3-pip bash build-base zlib-dev libpng-dev python3 python3-dev libstdc++ && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    apk add --no-cache mysql-dev linux-headers && \
    git clone https://github.com/weng-lab/kent && \
    cd kent/src/lib && make CFLAGS=-DLIBUUID_NOT_PRESENT && cd ../jkOwnLib && make && cd ../htslib && make && \
    mkdir -p /root/bin/x86_64 && cd ../utils/bedClip && make && cd ../bedGraphToBigWig && make && \
    cd ../bedToBigBed && make && \
    cd / && rm -rf kent && mv /root/bin/x86_64/* /bin && \
    git clone https://github.com/arq5x/bedtools2 && cd bedtools2 && make && \
    mv bin/* /bin && cd .. && rm -rf bedtools2 && \
    python3 -m pip install --no-cache-dir numpy && \
    python3 -m pip install --no-cache-dir macs2 && \
    apk del py3-pip build-base python3-dev git bash libpng-dev linux-headers
