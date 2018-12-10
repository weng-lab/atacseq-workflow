#
# Dockerfile for the naive overlap step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
#

FROM atacseq-common:1.0.0 as common

FROM alpine:3.8

# scripts
COPY src/ /app
RUN rm /app/common

# common scripts
COPY --from=common /app/common /app/common

# bedtools
RUN apk add --no-cache git bash build-base zlib-dev python && \
    git clone https://github.com/arq5x/bedtools2 && cd bedtools2 && make && \
    mv bin/* /bin && cd .. && rm -rf bedtools2 && \
    apk del git bash build-base 
