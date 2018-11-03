#
# Dockerfile for the xcor step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
# run_spp.R from github.com/ENCODE-DCC/pipeline-container#d2e385a
#
# encode_xcor modified to point to run_spp.R
# encode_common modified to mv files rather than hard link
#

FROM conoria/alpine-r

# scripts
RUN mkdir /app
COPY src/encode_xcor.py /app/encode_xcor.py
COPY src/encode_common.py /app/encode_common.py
COPY src/encode_common_genomic.py /app/encode_common_genomic.py
COPY src/encode_common_log_parser.py /app/encode_common_log_parser.py
COPY src/run_spp.R /app/run_spp.R
COPY src/install.R /app/install.R

# SPP and dependencies
RUN apk add --no-cache python wget boost-dev && \
    Rscript /app/install.R && rm /app/install.R && \
    wget https://github.com/hms-dbmi/spp/archive/1.15.2.tar.gz && \
    R CMD INSTALL 1.15.2.tar.gz && rm 1.15.2.tar.gz && \
    apk del wget boost-dev && rm -rf /var/cache/apk/*
