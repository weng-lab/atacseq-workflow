#!/usr/bin/env python3

import sys
import os
import tempfile
import shutil
import subprocess

from .cmds import twoBitToFa, fastacenter, meme
from .utils import getfasta, centerseq, shiftpeaks, resizeandclip

DEFAULT_MEME_OPTS = ["-dna", "-mod", "zoops", "-nmotifs", "5", "-minw", "6", "-maxw", "30", "-revcomp" ]

class MemeChipRunner:
    """
    Executes one or more MEME runs on a single peak file for a single genome assembly
    """
    
    def __init__(self, peaks, twobit, chrominfo, offset = 0):
        """
        Constructs a new MemeChipRunner instance.
    
        @param peaks: path to the input peak file in narrow peak format.
        @param twobit: path to two-bit sequence file for this genome assembly.
        @param chrominfo: path to chroosome size list for this genome assembly.
        @param offset: if set, number of basepairs to shift peaks; use a negative value to shift upstream.
        """
        self.peakFnp = peaks
        self.tmpDir = tempfile.mkdtemp()
        self.summitsFnp = self._tmp('summits.window150.narrowPeak')
        self.twobit = twobit
        self.chrominfo = chrominfo
        with open(chrominfo, 'r') as f:
            self.chromsizes = {
                line.strip().split('\t')[0]: int(line.strip().split('\t')[1])
                for line in f
            }
        self._get_summits(offset)
        self._get_sequences()
        os.makedirs(self._out("")) # make output directory within temp directory

    def __del__(self):
        shutil.rmtree(self.tmpDir)

    def _tmp(self, fn):
        return os.path.join(self.tmpDir, fn)

    def _out(self, fn):
        return self._tmp(os.path.join("output", fn))

    def tarUp(self, outpath):
        """
        Produces a tar file containing all generated output.

        @param outpath path to write the tar.
        """
        subprocess.call([ "tar", "cf", outpath, "-C", self._out("") ])
                
    def _get_summits(self, offset = 0):
        """
        Resizes peaks to 150bp around their midpoints and, if an offset is passed, shifts peaks by a set number of basepairs.

        @param offset: if passed, number of basepairs to shift peaks; give a negative value to shift upstream.
        """

        # generate peaks
        peaksout = shiftpeaks(self.peakFnp, self.chrominfo, offset)
        resizeandclip(peaksout, peaksout + ".resized", self.chromsizes)

        # sort by Q-value then P-value then signal
        with open(self.summitsFnp, 'w') as o:
            subprocess.call([ "sort", "-k9rn", "-k8rn", "-k7rn", peaksout + ".resized" ], stdout = o)

    def _get_sequences(self):
        """
        Produces FASTA sequence files for the top 500 and next 500 peaks on the resized and shifted input peaks.
        """
        getfasta(self.summitsFnp, self.twobit, self._tmp('top500.seqs'), (0, 500))
        centerseq(self._tmp('top500.seqs'), self._tmp('top500.seqs.center'), self._tmp('top500.seqs.flank'), 100)
        getfasta(self.summitsFnp, self.twobit, self._tmp('top501-1000.seqs'), (500, 1000))
        centerseq(self._tmp('top501-1000.seqs'), self._tmp('top501-1000.seqs.center'), self._tmp('top501-1000.seqs.flank'), 100)

    def run_meme(self, outsuffix = ".center.meme", meme_opts = DEFAULT_MEME_OPTS):
        """
        Runs MEME on the FASTA sequences generated for the top 500 and next 500 peaks.
        
        @param outsuffix suffix to append to the input sequence file name when writing output.
        @param meme_opts if passed, command line arguments to pass to MEME.
        """
        for prefix in [ "top500", "top501-1000" ]:
            subprocess.call(
                meme(
                    self._tmp(prefix + '.seqs.center'),
                    self._out(prefix + outsuffix),
                    meme_opts
                )
            )
