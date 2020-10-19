#
# Dockerfile for the bam2ta (bam to tagAlign) step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
#
# modified encode_bam2ta to remove unsupported -P option in alpine grep, and to preserve bedpe file for paired end
# modified encode_common_genomic to allow a tmpdir on the data disk rather than the smaller boot disk for sambamba
#

FROM atacseq-common:1.0.0 as common

FROM alpine:3.8

# scripts
COPY src/ /app

# common scripts
COPY --from=common /app/common /app/common

# bedtools, sambamba
RUN apk add --no-cache xz-dev git bash build-base zlib-dev bzip2-dev python3 libstdc++ && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    apk add --no-cache --upgrade grep && \
    git clone https://github.com/arq5x/bedtools2 && cd bedtools2 && make && \
    mv bin/* /bin && cd .. && rm -rf bedtools2 && \
    wget https://github.com/biod/sambamba/releases/download/v0.6.8/sambamba-0.6.8-linux-static.gz && \
    gunzip sambamba-0.6.8-linux-static.gz && mv sambamba-0.6.8-linux-static /bin/sambamba && chmod 755 /bin/sambamba && \
    apk del git build-base 
