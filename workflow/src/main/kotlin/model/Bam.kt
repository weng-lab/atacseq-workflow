package model

import krews.file.File

interface BamSamples {
    val alignments: List<BamAlignment>
}
data class BamSampleFiles(override val alignments: List<BamAlignmentFiles>) : BamSamples

interface BamAlignment {
    val name: String
}

data class BamAlignmentFiles(override val name: String, val bam: File,val pairedend: Boolean) : BamAlignment

