#!/usr/bin/env python

from __future__ import print_function

import sys
import os
import tempfile

def main(argc, argv):

    if argc < 4:
        print("usage: run_wellington.py regions reads outdir [cores=1]", file = sys.stderr)
        return 1
    regions = argv[1]; reads = argv[2]; outdir = argv[3]
    cores = int(argv[4]) if argc >= 5 else 1

    with tempfile.NamedTemporaryFile() as o:
        with open(regions, 'r') as f:
            for line in f:
                l = line.strip().split('\t')
                if int(l[2]) - int(l[1]) >= 98:
                    o.write('\t'.join(l[:3]) + '\n')
            o.seek(0)
            return os.system(
                "python /usr/bin/wellington_footprints.py -p {cores} -A {regions} {reads} {outdir}".format(
                    regions = o.name, reads = reads, outdir = outdir, cores = cores
                )
            )

if __name__ == "__main__":
    sys.exit(main(len(sys.argv), sys.argv))
