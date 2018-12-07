#
# Dockerfile for running MEME on the top 500 ATAC-seq peaks for an experiment
#

FROM alpine:3.8

# script
COPY src/ /app

# dependencies
RUN apk add --no-cache git python build-base wget bash zlib-dev perl ghostscript && \
    git clone https://github.com/arq5x/bedtools2 && cd bedtools2 && make && \
    mv bin/* /bin && cd .. && rm -rf bedtools2 && \
    wget http://meme-suite.org/meme-software/5.0.3/meme-5.0.3.tar.gz && tar zxf meme-5.0.3.tar.gz && \
    cd meme-5.0.3 && ./configure --enable-build-libxml2 --enable-build-libxslt && \
    make && make install && cp /root/bin/* /bin && cp /root/libexec/meme-5.0.3/* /bin && \
    cd .. && rm -rf meme-5.0.3 && rm meme-5.0.3.tar.gz && \
    apk del build-base git wget bash
