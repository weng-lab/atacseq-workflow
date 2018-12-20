#!/usr/bin/env python3

import sys
import os
import argparse

from .meme_runner import MemeChipRunner
from .methyl_meme_runner import MethylMemeRunner

def parseArgs():
    parser = argparse.ArgumentParser()
    parser.add_argument("peaks", type = str, help = "path to peak file in narrowPeak format")
    parser.add_argument("twobit", type = str, help = "path to two-bit file for this assembly")
    parser.add_argument("chrominfo", type = str, help = "path to chromosome lengths for this assembly")
    parser.add_argument("--outpath", type = str, help = "path to write output tar; defaults to [peakfile].tar", default = "")
    parser.add_argument("--methylbeds", nargs = '+',
                        help = "path to methyl beds from which to add methylated basepairs to input sequences")
    parser.add_argument("--alphabetfile", type = str,
                        help = "if methylbeds is set, path to an alphabet file containing 'm' and '1' for methylated basepairs")
    parser.add_argument("--offset", type = int, default = 0, help = "offset, in bp, to shift peaks")
    return parser.parse_args()

def main():
    
    args = parseArgs()
    if args.outpath == "":
        args.outpath = args.peaks + ".tar"
        
    if args.methylbeds is None or 0 == len(args.methylbeds):
        m = MemeChipRunner(args.peaks, args.twobit, args.chrominfo, args.offset)
    else:
        m = MethylMemeRunner(args.peaks, args.twobit, args.chrominfo, args.methylbeds, args.alphabetfile, args.offset)
    m.run_meme("top500.center.meme")
    m.tarUp(args.outpath)
        
    return 0

if __name__ == "__main__":
    sys.exit(main())
