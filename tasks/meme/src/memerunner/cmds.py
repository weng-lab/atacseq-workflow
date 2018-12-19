#!/usr/bin/env python3

twoBitToFa = lambda twobit, outfnp, infnp: [ "twoBitToFa", twobit, outfnp, "-bed=" + infnp ]
fastacenter = lambda flankfnp, clen: [ "fasta-center", "-len", str(clen), "-flank", flankfnp ]
meme = lambda infnp, outfnp, options: ["meme", "-oc", outfnp, "-nostatus", infnp ] + options
