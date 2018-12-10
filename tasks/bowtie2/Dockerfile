#
# Dockerfile for the Bowtie2 step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
# encode_common modified to mv files rather than hard link
#

FROM atacseq-common:1.0.0 as common

FROM frolvlad/alpine-glibc

# scripts
COPY src/ /app
RUN rm /app/common

# common scripts
COPY --from=common /app/common /app/common

# bowtie2 and samtools
RUN apk add --no-cache wget unzip python perl zlib-dev build-base libstdc++ python-dev py-setuptools && \
    wget https://github.com/BenLangmead/bowtie2/releases/download/v2.2.6/bowtie2-2.2.6-linux-x86_64.zip && \
    unzip bowtie2-2.2.6-linux-x86_64.zip && mv bowtie2*/bowtie2* /bin && rm -rf bowtie2-2.2.6* && \
    wget https://github.com/samtools/samtools/releases/download/1.9/samtools-1.9.tar.bz2 && \
    tar xvjf samtools-1.9.tar.bz2 && cd samtools-1.9 && \
    ./configure --without-curses --disable-lzma --disable-bz2 && \
    make && make install && cd .. && \
    rm -r samtools-1.9 && rm samtools-1.9.tar.bz2 && \
    apk del wget unzip build-base python-dev && rm -rf /var/cache/apk/*
