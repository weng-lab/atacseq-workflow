#!/bin/bash

# Note: (probably prune system, then) build images and check version #s
docker tag genomealmanac/atacseq-bowtie2:1.1.7 dockerhub.reimonn.com:5000/atacseq-bowtie2:1.1.7
docker tag genomealmanac/atacseq-idr:1.0.0 dockerhub.reimonn.com:5000/atacseq-idr:1.0.0
docker tag genomealmanac/atacseq-macs2:2.2.21 dockerhub.reimonn.com:5000/atacseq-macs2:2.2.21
docker tag genomealmanac/atacseq-spr:1.0.2 dockerhub.reimonn.com:5000/atacseq-spr:1.0.2
docker tag genomealmanac/atacseq-bam2ta:1.1.4 dockerhub.reimonn.com:5000/atacseq-bam2ta:1.1.4
docker tag genomealmanac/atacseq-trim-adapters:1.1.7 dockerhub.reimonn.com:5000/atacseq-trim-adapters:1.1.7
docker tag genomealmanac/atacseq-poolta:1.1.2 dockerhub.reimonn.com:5000/atacseq-poolta:1.1.2

# Push to dockerhub.reimonn.com
docker push dockerhub.reimonn.com:5000/atacseq-bowtie2:1.1.7
docker push dockerhub.reimonn.com:5000/atacseq-idr:1.0.0
docker push dockerhub.reimonn.com:5000/atacseq-macs2:2.2.21
docker push dockerhub.reimonn.com:5000/atacseq-spr:1.0.2
docker push dockerhub.reimonn.com:5000/atacseq-bam2ta:1.1.4
docker push dockerhub.reimonn.com:5000/atacseq-trim-adapters:1.1.7
docker push dockerhub.reimonn.com:5000/atacseq-poolta:1.1.2

# Get other docker images that we need
docker pull encodedcc/atac-seq-pipeline:v1.8.0
docker tag encodedcc/atac-seq-pipeline:v1.8.0 dockerhub.reimonn.com:5000/atac-seq-pipeline:v1.8.0
docker push dockerhub.reimonn.com:5000/atac-seq-pipeline:v1.8.0

docker pull treimonn/pcre-code
docker tag treimonn/pcre-code dockerhub.reimonn.com:5000/pcre-code:v1.0.0
docker push dockerhub.reimonn.com:5000/pcre-code:v1.0.0
