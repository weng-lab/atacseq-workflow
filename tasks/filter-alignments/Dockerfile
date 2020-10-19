#
# Dockerfile for the alignment filtering step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
#


FROM atacseq-common:1.0.0 as common

FROM anapsix/alpine-java:latest

# scripts
COPY src/ /app

# common scripts
COPY --from=common /app/common /app/common

# Picard jar and sambamba
RUN apk add --no-cache bash grep gzip xz-dev bzip2-dev wget build-base python zlib-dev git python3 py3-setuptools py3-pip && \
    python3 -m pip install SAMstats==0.2.1 && \
    wget https://github.com/biod/sambamba/releases/download/v0.6.8/sambamba-0.6.8-linux-static.gz && \
    gunzip sambamba-0.6.8-linux-static.gz && mv sambamba-0.6.8-linux-static /bin/sambamba && chmod 755 /bin/sambamba && \
    wget https://github.com/broadinstitute/picard/releases/download/2.18.14/picard.jar -O /bin/picard.jar && chmod +x /bin/picard.jar && \
    wget https://github.com/samtools/samtools/releases/download/1.9/samtools-1.9.tar.bz2 && \
    tar xvjf samtools-1.9.tar.bz2 && cd samtools-1.9 && \
    ./configure --without-curses --disable-lzma --disable-bz2 && \
    make && make install && cd .. && \
    rm -r samtools-1.9 && rm samtools-1.9.tar.bz2 && \
    git clone https://github.com/arq5x/bedtools2 && cd bedtools2 && make && mv bin/bedtools /bin/bedtools && \
    cd .. && rm -r bedtools2 && \
    apk del wget build-base git
