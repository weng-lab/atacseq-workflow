#!/usr/bin/env python

from __future__ import print_function

import multiprocessing as mp
import argparse, os
from clint.textui import progress, puts_err
import pyDNase
from pyDNase import footprinting

__version__ = "0.2.0"

parser = argparse.ArgumentParser(description='Footprint the DHSs in a DNase-seq or ATAC-seq experiment using the Wellington Algorithm.')
parser.add_argument("-b","--bonferroni",action="store_true", help="Performs a bonferroni correction (default: False)",default=False)
parser.add_argument("-sh", "--shoulder-sizes", help="Range of shoulder sizes to try in format \"from,to,step\" (default: 35,36,1)",default="35,36,1",type=str)
parser.add_argument("-fp", "--footprint-sizes", help="Range of footprint sizes to try in format \"from,to,step\" (default: 11,26,2)",default="11,26,2",type=str)
parser.add_argument("-d", "--one_dimension",action="store_true", help="Use Wellington 1D instead of Wellington (default: False)",default=False)
parser.add_argument("-fdr","--FDR_cutoff", help="Write footprints using the FDR selection method at a specific FDR (default: 0.01)",default=0.01,type=float)
parser.add_argument("-fdriter", "--FDR_iterations", help="How many randomisations to use when performing FDR calculations (default: 100)",default=100,type=int)
parser.add_argument("-fdrlimit", "--FDR_limit", help="Minimum p-value to be considered significant for FDR calculation (default: -20)",default=-20,type=int)
parser.add_argument("-pv","--pv_cutoffs", help=" (Provide multiple values separated by spaces) Select footprints using a range of pvalue cutoffs (default: -10 -20 -30 -40 -50 -75 -100 -300 -500 -700",default=[-10,-20,-30,-40,-50,-75,-100,-300,-500,-700],type=int, nargs="+")
parser.add_argument("-dm","--dont-merge-footprints",action="store_true", help="Disables merging of overlapping footprints (Default: False)",default=False)
parser.add_argument("-o","--output_prefix", help="The prefix for results files (default: <reads.regions>)",default="",type=str)
parser.add_argument("-p", help="Number of processes to use, use 0 to use all cores (default: 1)",default=1,type=int)
parser.add_argument("-A",action="store_true", help="ATAC-seq mode (default: False)",default=False)
parser.add_argument("regions", help="BED file of the regions you want to footprint")
parser.add_argument("reads", help="The BAM file containing the DNase-seq reads")
parser.add_argument("outputdir", help="A writeable directory to write the results to")
clargs = parser.parse_args()

def percentile(N, percent):
    """
    Find the percentile of a list of values.

    @parameter N - is a list of values.
    @parameter percent - a float value from 0.0 to 1.0.

    @return - the percentile of the values as a float
    """
    if not N:
        return None
    N = sorted(N)
    k = (len(N)-1) * percent
    f = math.floor(k)
    c = math.ceil(k)
    if f == c:
        return float(N[int(k)])
    d0 = N[int(f)] * (c-k)
    d1 = N[int(c)] * (k-f)
    return float(d0+d1)

#Sanity check parameters from the user

def xrange_from_string(range_string):
    try:
        range_string = list(map(int,range_string.split(",")))
        range_string = list(range(range_string[0],range_string[1],range_string[2]))
        assert len(range_string) > 0
        return range_string
    except:
        raise ValueError

try:
    clargs.shoulder_sizes = xrange_from_string(clargs.shoulder_sizes)
    clargs.footprint_sizes = xrange_from_string(clargs.footprint_sizes)
except ValueError:
    raise RuntimeError("shoulder and footprint sizes must be supplied as from,to,step")

assert 0 < clargs.FDR_cutoff < 1, "FDR must be between 0 and 1"
assert clargs.FDR_limit <= 0, "FDR limit must be less than or equal to 0 (to disable)"
assert len([f for f in os.listdir(clargs.outputdir) if f[0] != "."]) == 0, "output directory {0} is not empty!".format(clargs.outputdir)

if not clargs.output_prefix:
    clargs.output_prefix = str(os.path.basename(clargs.reads)) + "." + str(os.path.basename(clargs.regions))

#Load reads and regions
regions = pyDNase.GenomicIntervalSet(clargs.regions)
reads = pyDNase.BAMHandler(clargs.reads,caching=False,ATAC=clargs.A)

#Create a directory for p-values and WIG output. This /should/ be OS independent
os.makedirs(os.path.join(clargs.outputdir,"p value cutoffs"))
wigout = open(os.path.relpath(clargs.outputdir) + "/" + clargs.output_prefix + ".WellingtonFootprints.wig","w")
fdrout = open(os.path.relpath(clargs.outputdir) + "/" + clargs.output_prefix + ".WellingtonFootprints.FDR.{0}.bed".format(clargs.FDR_cutoff),"w")

#Required for UCSC upload
print("track type=wiggle_0", file=wigout)

#Iterate in chromosome, basepair order
orderedbychr = [item for sublist in sorted(regions.intervals.values()) for item in sublist]  # Flatten the list of lists
puts_err("Calculating footprints...")

if clargs.p:
    CPUs = clargs.p
else:
    CPUs = mp.cpu_count()
max_regions_cached_in_memory = 10 * CPUs
p = mp.Pool(CPUs)


def writetodisk(fp):
    #Raw WIG scores
    print("fixedStep\tchrom=" + str(fp.interval.chromosome) + "\t start="+ str(fp.interval.startbp) +"\tstep=1", file=wigout)
    print('\n'.join(map(str, fp.scores)), file=wigout)
    #FDR cutoff footprints
    fdr = fp.FDR_value
    if fdr < clargs.FDR_limit:
         for footprint in fp.footprints(withCutoff=fdr,merge=clargs.dont_merge_footprints):
             print(footprint, file=fdrout)
    #p-value cutoff footprints
    for fpscore in clargs.pv_cutoffs:
        ofile = open(os.path.relpath(os.path.join(clargs.outputdir,"p value cutoffs")) + "/" + clargs.output_prefix + ".WellingtonFootprints.{0}.bed".format(fpscore),"a")
        for footprint in fp.footprints(withCutoff=fpscore):
            print(footprint, file=ofile)
        ofile.close()

def multiWellington(regions,reads,**kwargs):
    p = mp.Pool(CPUs)
    for i in progress.bar(regions):
        if clargs.one_dimension:
            fp = footprinting.wellington1D(i,reads,**kwargs)
        else:
            fp = footprinting.wellington(i,reads,**kwargs)
        p.apply_async(fp,callback = writetodisk)
        #Hold here while the queue is bigger than the number of reads we're happy to store in memory
        while p._taskqueue.qsize() > max_regions_cached_in_memory:
            pass
    p.close()
    puts_err("Waiting for the last {0} jobs to finish...".format(max_regions_cached_in_memory))
    p.join()

#TODO: Use **args or something similar to pass arguments?
multiWellington(orderedbychr,reads, shoulder_sizes = clargs.shoulder_sizes ,footprint_sizes = clargs.footprint_sizes, FDR_cutoff=clargs.FDR_cutoff,FDR_iterations=clargs.FDR_iterations,bonferroni = clargs.bonferroni)

wigout.close()
