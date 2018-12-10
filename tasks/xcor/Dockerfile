#
# Dockerfile for the xcor step of the ENCODE-DCC ATAC-seq pipeline
# scripts from github.com/ENCODE-DCC/atac-seq-pipeline#86ed2d2
# run_spp.R from github.com/ENCODE-DCC/pipeline-container#d2e385a
#
# encode_xcor modified to point to run_spp.R
# encode_common modified to mv files rather than hard link
# run_spp.R modified to load required caTools
#

FROM atacseq-common:1.0.0 as common

FROM conoria/alpine-r

# scripts
COPY src/ /app
RUN rm /app/common

# common scripts
COPY --from=common /app/common /app/common

# SPP and dependencies
RUN apk add --no-cache python wget boost-dev ghostscript && \
    Rscript /app/install.R && rm /app/install.R && \
    wget https://github.com/hms-dbmi/spp/archive/1.15.2.tar.gz && \
    R CMD INSTALL 1.15.2.tar.gz && rm 1.15.2.tar.gz && \
    apk del wget boost-dev && rm -rf /var/cache/apk/*
