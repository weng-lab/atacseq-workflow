#
# Dockerfile for splitting HiSeq fastq files with both ends into separate files.
#

FROM alpine:3.8

# single executable
RUN mkdir /app
COPY src /app
RUN apk add --no-cache build-base libstdc++ && \
    cd /app && g++ main.cpp -o /bin/splitfastq && \
    cd .. && rm -r /app && \
    apk del build-base
