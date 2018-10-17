#
# Dockerfile for the alignment filtering step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
#

FROM alpine:3.8

# Picard jar and sambamba
RUN apk add --no-cache wget openjdk7-jre python
RUN wget https://github.com/biod/sambamba/releases/download/v0.6.8/sambamba-0.6.8-linux-static.gz && \
    gunzip sambamba-0.6.8-linux-static.gz && mv sambamba-0.6.8-linux-static /bin/sambamba && chmod 755 /bin/sambamba
RUN wget https://github.com/broadinstitute/picard/releases/download/2.18.14/picard.jar -O /bin/picard.jar && chmod +x /bin/picard.jar
RUN apk del wget

# Python scripts
RUN mkdir /app
COPY src/encode_filter.py /app/encode_filter.py
COPY src/encode_common_genomic.py /app/encode_common_genomic.py
