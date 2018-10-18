#
# Dockerfile for the alignment filtering step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
#

FROM anapsix/alpine-java:latest

# Picard jar and sambamba
RUN apk add --no-cache wget build-base python zlib-dev git
RUN wget https://github.com/biod/sambamba/releases/download/v0.6.8/sambamba-0.6.8-linux-static.gz && \
    gunzip sambamba-0.6.8-linux-static.gz && mv sambamba-0.6.8-linux-static /bin/sambamba && chmod 755 /bin/sambamba
RUN wget https://github.com/broadinstitute/picard/releases/download/2.18.14/picard.jar -O /bin/picard.jar && chmod +x /bin/picard.jar

# install samtools
RUN wget https://github.com/samtools/samtools/releases/download/1.9/samtools-1.9.tar.bz2 && \
    tar xvjf samtools-1.9.tar.bz2 && cd samtools-1.9 && \
    ./configure --without-curses --disable-lzma --disable-bz2 && \
    make && make install && cd .. && \
    rm -r samtools-1.9 && rm samtools-1.9.tar.bz2

# install bedtools
RUN git clone https://github.com/arq5x/bedtools2 && cd bedtools2 && make && mv bin/bedtools /bin/bedtools && \
    cd .. && rm -r bedtools2

# clean up after apk
RUN apk del wget build-base git

# Python scripts
RUN mkdir /app
COPY src/encode_common.py /app/encode_common.py
COPY src/encode_common_genomic.py /app/encode_common_genomic.py
COPY src/encode_filter.py /app/encode_filter.py
