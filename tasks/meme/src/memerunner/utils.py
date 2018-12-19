#!/usr/bin/env python

import subprocess
import tempfile

from .cmds import twoBitToFa

def linerange(infile, start, end):
    """
    Yields a range of lines from an input file, beginning at line #start and ending at line #end.
    
    @param infile: path to the input file.
    @param start: first line to yield (0-based).
    @param end: last line to yield (1-based, or 0-based not inclusive).
    """
    with open(infile, 'r') as f:
        for i in range(start):
            f.readline()
        for i in range(end - start):
            line = f.readline()
            if not line: break
            yield line

def getfasta(peaks, twobit, outpath, linerange = (0, 500)):
    """
    Produces a FASTA for a subset of lines in a peak file.
    
    @param peaks: path to the peak file.
    @param twobit: path to the two bit sequence file for this genome.
    @param outpath: output path to write the FASTA.
    @param linerange: range of lines to select from the peak file.
    """
    with tempfile.NamedTemporaryFile() as f:
        for line in linerange(peaks, *linerange):
            line = line.strip().split('\t')
            f.write('\t'.join(line[:4]) + '\n') # cut to first four fields for twoBitToFa
        f.seek(0)
        with open(outpath, 'r') as outf:
            subprocess.call(twoBitToFa(twobit, self._out("top501-1000.seqs"), f.name))

def centerseq(seqs, outpath, flank, clen = 100):
    """
    Calls fasta-center on a FASTA file.

    @param seqs input fasta file.
    @param path to write output.
    @param path to write flanking sequences.
    @param clen length of sequences to output.
    """
    with open(seqs, 'r') as f:
        with open(outpath, 'w') as o:
            subprocess.call(fastacenter(flank, clen), stdin = seqs, stdout = outpath)

def shiftpeaks(peaks, chrominfo, offset):
    """
    Shifts peaks by a given number of basepairs.
    
    @param peaks path to the peak file to shift.
    @param chrominfo path to chromosome lengths for this genome assembly.
    @param offset number of basepairs to shift; may be positive or negative.
    """
    if offset != 0:
        peaksout = peaks + ".shift%d.bed" % offset
        with open(peaksout, 'w') as o:
            subprocess.call(
                [ "bedtools", "shift", "-i", peaks,
                  "-g", chrominfo, "-s", offset ], stdout = o
            )
        return peaksout
    return peaks

def resizeandclip(inpath, outpath, chromsizes, newsize = 150):
    """
    Resizes peaks to a fixed width around their midpoint, excluding any peaks which extend off the chromosome.
    
    @param inpath path to peaks to resize.
    @param outpath path to write resized output peaks.
    @param chromsizes path to chromosome sizes for this assembly.
    @param newsize fixed with to which peaks should be resized.
    """
    
    with open(inpath, 'r') as f:
        with open(outpath, 'w') as o:
            for line in f:

                # read line, get center point (average of start and end)
                line = line.strip().split('\t')
                midpoint = (int(line[2]) + int(line[1])) / 2
                
                # skip if invalid coordinates
                if line[0] not in chromsizes: # unrecognized chromosome
                    continue
                if midpoint < 150 or midpoint + newsize > chromsizes[line[0]]: # size out of range
                    continue
                
                # resize and write
                line[1] = str(midpoint - newsize)
                line[2] = str(midpoint + newsize)
                o.write('\t'.join(line) + '\n')
