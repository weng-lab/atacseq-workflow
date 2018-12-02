#
# Dockerfile for calling footprints using Wellington
# wellington_footprints.py modified for Python2 compatibility
#

FROM frolvlad/alpine-glibc

# samtools, bedops, pyDNase
RUN apk add --no-cache wget python3 zlib-dev build-base py-pip python-dev freetype-dev libpng-dev bzip2-dev xz-dev lapack-dev gfortran && \
    wget https://github.com/bedops/bedops/releases/download/v2.4.35/bedops_linux_x86_64-v2.4.35.tar.bz2 && \
    tar jxvf bedops_linux_x86_64-v2.4.35.tar.bz2 && \
    rm bedops_linux_x86_64-v2.4.35.tar.bz2 && \
    wget https://github.com/samtools/samtools/releases/download/1.9/samtools-1.9.tar.bz2 && \
    tar xvjf samtools-1.9.tar.bz2 && cd samtools-1.9 && \
    ./configure --without-curses --disable-lzma --disable-bz2 && \
    make && make install && cd .. && \
    rm -r samtools-1.9 && rm samtools-1.9.tar.bz2 && \
    pip install numpy scipy pyDNase && \
    apk del wget py-pip

COPY src/wellington_footprints.py /usr/bin/wellington_footprints.py
COPY src/run_wellington.py /bin/run_wellington.py
