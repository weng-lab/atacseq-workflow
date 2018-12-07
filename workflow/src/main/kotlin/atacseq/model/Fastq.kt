package atacseq.model

import krews.file.File

interface FastqSamples {
    val replicates: List<FastqReplicate>
}

data class FastqSamplesSE(override val replicates: List<FastqReplicateSE>) : FastqSamples
data class FastqSamplesPE(override val replicates: List<FastqReplicatePE>) : FastqSamples

interface FastqReplicate {
    val name: String
}

data class FastqReplicateSE(override val name: String, val fastqs: List<File>, val adaptor: File? = null) : FastqReplicate
data class FastqReplicatePE(
        override val name: String,
        val fastqsR1: List<File>,
        val fastqsR2: List<File>,
        val adaptorR1: File? = null,
        val adaptorR2: File? = null
) : FastqReplicate

interface MergedFastqReplicate {
    val name: String
}

data class MergedFastqReplicateSE(override val name: String, val merged: File) : MergedFastqReplicate
data class MergedFastqReplicatePE(override val name: String, val mergedR1: File, val mergedR2: File) : MergedFastqReplicate