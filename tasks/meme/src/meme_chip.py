#!/usr/bin/env python

from __future__ import print_function

import sys
import os
import argparse
import tempfile

twoBitToFa = lambda twobit, outfnp, infnp: "twoBitToFa %s %s -bed=%s" % (twobit, outfnp, infnp)
fastacenter = lambda infnp, flankfnp, outfnp, clen: "fasta-center -len %d -flank %s < %s > %s" % (clen, flankfnp, infnp, outfnp)
meme = lambda infnp, outfnp, options: "meme -oc %s -nostatus %s %s" % (outfnp, infnp, options)

class MemChipRunner:

    DEFAULT_MEME_OPTS = '-dna -mod zoops -nmotifs 5 -minw 6 -maxw 30 -revcomp'
    
    def __init__(self, peaks, twobit, chrominfo, meme_opts = MemeChipRunner.DEFAULT_MEME_OPTS):
        self.meme_opts = meme_opts
        self.peakFnp = peaks
        self.tmpDir = tempfile.mkdtemp()
        self.summitsFnp = self._out('summits.window150.narrowPeak')
        self.twobit = twobit
        self.chrominfo = chrominfo
        with open(chrominfo, 'r') as f:
            self.chromsizes = {
                line.strip().split('\t')[0]: int(line.strip().split('\t')[1])
                for line in f
            }

    def __del__(self):
        os.system("rm -rf {tmpdir}".format(tmpdir = self.tmpDir))

    def _out(self, fn):
        return os.path.join(self.tmpDir, fn)

    def tarUp(self, outpath):
        os.system("tar cf {outpath} -C {tmpdir} .".format(
            tmpf = outpath,
            tmpdir = tmpdir
        ))
                
    def get_summits(self, offset = 0):

        # generate peaks
        if offset != 0:
            peaksout = self.peakFnp + ".shift%d.bed" % offset
            os.system(
                "bedtools shift -i {peaks} -g {genome} -s {offset} > {output}".format(
                    peaks = self.peakFnp, genome = self.chrominfo, offset = offset,
                    output = peaksout
                )
            )
        else:
            peaksout = self.peakFnp
            
        # resize each peak 150 bp around center; clip peaks out of range
        with open(peaksout, 'r') as f:
            with open(peaksout + ".resized", 'w') as o:
                for line in f:

                    # read line, get center point (average of start and end)
                    line = line.strip().split('\t')
                    midpoint = (int(line[2]) + int(line[1])) / 2

                    # skip if invalid coordinates
                    if line[0] not in self.chromsizes: # unrecognized chromosome
                        continue
                    if midpoint < 150 or midpoint + 150 > self.chromsizes[line[0]]: # size out of range
                        continue

                    # resize and write
                    line[1] = str(midpoint - 150)
                    line[2] = str(midpoint + 150)
                    o.write('\t'.join(line) + '\n')

        # sort by Q-value then P-value then signal
        os.system("sort -k9rn -k8rn -k7rn {peaks} > {output}".format(
            peaks = peaksout, output = self.summitsFnp
        ))

    def get_sequences(self):
        
        # get top 500
        os.system(' '.join([
            "cat " + self.summitsFnp,
            "| head -n 500",
            "| cut -f1-4",
            '|', twoBitToFa(self.twobit, self._out('top500.seqs'), "stdin")
        ]))
        os.system(
            fastacenter(
                self._out('top500.seqs'), self._out('top500.seqs.flank'),
                self._out('top500.seqs.center'), 100
            )
        )
        
        # get top 501-1000
        os.system(' '.join([
            "cat " + self.summitsFnp,
            "| head -n 1000",
            "| tail -n 500",
            "| cut -f1-4",
            '|', twoBitToFa(self.twobit, self._out('top501-1000.seqs'), "stdin")
        ]))
        os.system(
            fastacenter(
                self._out('top501-1000.seqs'), self._out('top501-1000.seqs.flank'),
                self._out('top501-1000.seqs.center'), 100
            )
        )

    def run_meme_jobs(self):
        os.system(
            meme(
                self._out('top500.seqs.center'),
                self._out('top500.center.meme'),
                self.meme_opts
            )
        )

    def run(self, outpath, offset = 0):
        self.get_summits(offset)
        self.get_sequences()
        self.run_meme_jobs()
        self.tarUp(outpath)
        
def parseArgs():
    parser = argparse.ArgumentParser()
    parser.add_argument("--peaks", type = str, help = "path to peaks in narrowPeak format")
    parser.add_argument("--twobit", type = str, help = "path to two-bit file for this assembly")
    parser.add_argument("--offset", type = int, default = 0, help = "offset, in bp, to shift peaks")
    parser.add_argument("--chrominfo", type = str, help = "path to chromosome lengths for this assembly")
    parser.add_argument("--outpath", type = str, help = "path to write output tar")
    return parser.parse_args()

def main():
    args = parseArgs()
    MemChipRunner(args.peaks, args.twobit, args.chrominfo).run(args.outpath, args.offset)
    return 0

if __name__ == "__main__":
    sys.exit(main())
