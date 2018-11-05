#!/usr/bin/env python

#
# implements bedClip (https://github.com/ENCODE-DCC/kentUtils/blob/master/src/utils/bedClip/bedClip.c)
# trims bed lines where:
#    1) start is negative
#    2) stop is not greater than start, or
#    3) stop is past chromosome end
#

from __future__ import print_function

import sys

def _chrom_sizes(path):
    with open(path, 'r') as f:
        return { x.strip().split('\t')[0]: int(x.strip().split('\t')[1])
                 for x in f }

def main(argc, argv):

    if argc < 4:
        print("usage: bedclip.py input.bed chrom.sizes.tsv output.bed", file = sys.stderr)
        return 1

    chromsizes = _chrom_sizes(argv[2])
    with open(argv[1], 'r') as f:
        with open(argv[3], 'w') as o:
            for line in f:
                l = line.strip().split('\t')
                chrom = l[0]; start = int(l[1]); stop = int(l[2])
                if chrom not in chromsizes: continue # missing chromosome
                if start < 0 or stop <= start or stop > chromsizes[chrom]: continue # invalid coord
                o.write(line) # if here, this line is valid, so write

    return 0

if __name__ == "__main__":
    sys.exit( main(len(sys.argv), sys.argv) )
