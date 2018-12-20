#!/usr/bin/env python

import tempfile
import subprocess

from .cmds import meme
from .utils import insertmethylfasta, methylintersection, linerange, centerseq
from .meme_runner import MemeChipRunner, DEFAULT_MEME_OPTS

class MethylMemeRunner(MemeChipRunner):

    def __init__(self, peaks, twobit, chrominfo, methylpeaks, alphabetfile, offset = 0):
        """
        Constructs a new MemeChipRunner instance.
    
        @param peaks: path to the input peak file in narrow peak format.
        @param twobit: path to two-bit sequence file for this genome assembly.
        @param chrominfo: path to chroosome size list for this genome assembly.
        @param methylpeaks: path to methyl BED from which to add methyl basepairs.
        @param alphabetfile: path to an alphabet file containing 'm' and '1'.
        @param offset: if set, number of basepairs to shift peaks; use a negative value to shift upstream.
        """
        MemeChipRunner.__init__(self, peaks, twobit, chrominfo, offset)
        self.alphabetfile = alphabetfile
        self._add_methyl(methylpeaks)
    
    def _add_methyl(self, methylpaths):
        """
        Adds methyl basepairs to the top 500 and top 501-1000 peaks.
        
        @param methylpaths paths to methyl BEDs from which to add methylated symbols.
        """
        
        for prefix, lrange in [ ("top500", (0, 500)), ("top501-1000", (500, 1000)) ]:
        
            with tempfile.NamedTemporaryFile('wt') as t:

                # get requested peaks
                coords = []
                for line in linerange(self.summitsFnp, *lrange):
                    coords.append(tuple(line.strip().split('\t')[:3]))
                    t.write(line)
                t.seek(0)

                # get intersecting methylated basepairs and write
                methylpeaks = methylintersection(t.name, coords, methylpaths)
                insertmethylfasta(self._tmp(prefix + ".seqs"), self._tmp(prefix + ".methyl.seqs"), coords, methylpeaks)

            centerseq(
                self._tmp(prefix + ".methyl.seqs"), self._tmp(prefix + ".methyl.seqs.center"),
                self._tmp(prefix + ".methyl.seqs.flank"), 100
            )

    def run_meme(self, outsuffix = ".center.methyl.meme", meme_opts = DEFAULT_MEME_OPTS):
        """
        Runs MEME on the FASTA sequences generated for the top 500 and next 500 peaks.
        
        @param outsuffix suffix to append to the input sequence file name when writing output.
        @param meme_opts if passed, command line arguments to pass to MEME.
        """
        for prefix in [ "top500", "top501-1000" ]:
            subprocess.call(
                meme(
                    self._tmp(prefix + '.methyl.seqs.center'),
                    self._out(prefix + outsuffix),
                    meme_opts + [ "-alph", self.alphabetfile ]
                )
            )
