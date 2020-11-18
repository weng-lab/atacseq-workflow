#!/usr/env/python3

import sys, getopt, json
from copy import deepcopy
from os.path import basename

def parse_args(argv):
    # Input arguments:
    # -i: Input filelist of fastq files (Two columns for paired-end sequencing)
    # -c: Input config file to use as reference
    # -o: Output config file to write to
    # -d: working directory / output directory
    args = '-i -c -o -d'.split()
    try:
        optlist, args = getopt.getopt(argv, "i:c:o:d:a:")
    except getopt.GetoptError:
        print('create-atacseq-config -i <input filelist> -c <input config> -o <output config> -d <output dir> -a <adapter>')
        sys.exit(2)
    print(optlist)
    for k, v in optlist:
        if k == '-i':
            infile = v
        if k == '-c':
            configfile = v
        if k == '-o':
            outfile = v
        if k == '-d':
            workingdir = v
        if k == '-a':
            adapter = v
    print("Infile: ", infile)
    print("Outfile: ", outfile)
    print("Configfile: ", configfile)
    print('Output Directory: ', workingdir)
    print('Adapter: ', adapter)
    return([infile, outfile, configfile, workingdir, adapter])


def load_config(configfile):
    with open(configfile) as f:
        conf = json.load(f)
    return(conf)


def update_wd(conf, workingdir):
    conf['working-dir'] = workingdir
    return(conf)


def update_experiments(conf, infile, adapter):
    # We need to update the experiments portion of this config
    # Grab the first experiment as a template
    exp = conf['params']['experiments'][0]
    # Create a new list to fill of experiments
    explist = []
    # Iterate through the input filelist and create new experiments
    with open(infile, 'r') as f:
        line = f.readline()
        count = 1
        while line:
            print(count, line.strip())
            linearr = line.strip().split()
            e = deepcopy(exp)
            # Set the experiment name
            e['name'] = basename(linearr[0]).replace("_1.fastq.gz", "")
            # This is unreplicated data, so update it
            exprep = e['replicates'][0]
            # Add a replicate name
            exprep['name'] = "rep0"
            # Set the adapter
            exprep['adaptor-all'] = adapter
            # Update the short name of the sample
            exprep['fastqs-r1'][0]['path'] = basename(linearr[0])
            exprep['fastqs-r2'][0]['path'] = basename(linearr[1])
            # Update the experiment path
            exprep['fastqs-r1'][0]['local-path'] = linearr[0]
            exprep['fastqs-r2'][0]['local-path'] = linearr[1]
            # Set all to LocalInputFile
            exprep['fastqs-r1'][0]['-type'] = "krews.file.LocalInputFile"
            exprep['fastqs-r2'][0]['-type'] = "krews.file.LocalInputFile"
            # Append this experiment
            explist.append(e)
            count += 1
            line = f.readline()
    conf['params']['experiments'] = explist
    return(conf)

def write_conf(conf, outfile):
    # print(json.dumps(conf, indent = 4))
    with open(outfile, 'w') as f:
        f.write(json.dumps(conf, indent = 4))

def main(argv):
    infile, outfile, configfile, workingdir, adapter = parse_args(argv)
    config = load_config(configfile)
    config = update_experiments(config, infile, adapter)
    config = update_wd(config, workingdir)
    write_conf(config, outfile)



if __name__ == "__main__":
   main(sys.argv[1:])
