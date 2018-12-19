#!/usr/bin/env python3

import sys
import os
import argparse

from .meme_runner import MemeChipRunner

def parseArgs():
    parser = argparse.ArgumentParser()
    parser.add_argument("peaks", type = str, help = "path to peak file in narrowPeak format")
    parser.add_argument("twobit", type = str, help = "path to two-bit file for this assembly")
    parser.add_argument("chrominfo", type = str, help = "path to chromosome lengths for this assembly")
    parser.add_argument("--outpath", type = str, help = "path to write output tar; defaults to [peakfile].tar", default = "")
    parser.add_argument("--offset", type = int, default = 0, help = "offset, in bp, to shift peaks")
    return parser.parse_args()

def main():
    args = parseArgs()
    if args.outpath == "":
        args.outpath = args.peaks + ".tar"
    MemeChipRunner(args.peaks, args.twobit, args.chrominfo).run(args.outpath, args.offset)
    return 0

if __name__ == "__main__":
    sys.exit(main())
