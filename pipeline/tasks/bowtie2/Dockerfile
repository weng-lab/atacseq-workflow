#
# Dockerfile for the Bowtie2 step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
# encode_common and encode_trim_adapter modified to mv files rather than hard link
#

FROM alpine:3.8

# bowtie2 binaries and prerequisites for samtools
RUN apk add --no-cache wget unzip python perl build-base zlib-dev
RUN wget https://github.com/BenLangmead/bowtie2/releases/download/v2.2.6/bowtie2-2.2.6-linux-x86_64.zip && \
    unzip bowtie2-2.2.6-linux-x86_64.zip && mv bowtie2*/bowtie2* /bin && rm -rf bowtie2-2.2.6*

# install samtools
RUN wget https://github.com/samtools/samtools/releases/download/1.9/samtools-1.9.tar.bz2 && \
    tar xvjf samtools-1.9.tar.bz2 && cd samtools-1.9 && \
    ./configure --without-curses --disable-lzma --disable-bz2 && \
    make && make install && cd .. && \
    rm -r samtools-1.9 && rm samtools-1.9.tar.bz2

# clean up after apk
RUN apk del wget unzip build-base && rm -rf /var/cache/apk/*

# python scripts
RUN mkdir /app
COPY src/detect_adapter.py /app/detect_adapter.py
COPY src/encode_common.py /app/encode_common.py
COPY src/encode_common_genomic.py /app/encode_common_genomic.py
COPY src/encode_trim_adapter.py /app/encode_trim_adapter.py
COPY src/encode_bowtie2.py /app/encode_bowtie2.py
