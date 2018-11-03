#
# Dockerfile for the bam2ta (bam to tagAlign) step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
# modified encode_bam2ta to remove unsupported -P option in alpine grep
#

FROM alpine:3.8

# bedtools
RUN apk add --no-cache git bash build-base zlib-dev python libstdc++
RUN git clone https://github.com/arq5x/bedtools2 && cd bedtools2 && make && \
    mv bin/* /bin && cd .. && rm -rf bedtools2
RUN wget https://github.com/biod/sambamba/releases/download/v0.6.8/sambamba-0.6.8-linux-static.gz && \
    gunzip sambamba-0.6.8-linux-static.gz && mv sambamba-0.6.8-linux-static /bin/sambamba && chmod 755 /bin/sambamba
RUN apk del git bash build-base 

# Python scripts
RUN mkdir /app
COPY src/encode_bam2ta.py /app/encode_bam2ta.py
COPY src/encode_common_genomic.py /app/encode_common_genomic.py
COPY src/encode_common.py /app/encode_common.py
