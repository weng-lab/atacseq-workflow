package atacseq.model

import krews.file.File

interface FastqReplicates {
    val replicates: List<FastqReplicate>
}

data class FastqReplicatesSE(override val replicates: List<FastqReplicateSE>) : FastqReplicates
data class FastqReplicatesPE(override val replicates: List<FastqReplicatePE>) : FastqReplicates

interface FastqReplicate {
    val name: String
}

data class FastqReplicateSE(override val name: String, val merges: List<File>, val adaptor: File? = null) : FastqReplicate
data class FastqReplicatePE(
        override val name: String,
        val mergesR1: List<File>,
        val mergesR2: List<File>,
        val adaptorR1: File? = null,
        val adaptorR2: File? = null
) : FastqReplicate

interface MergedFastqReplicate {
    val name: String
}

data class MergedFastqReplicateSE(override val name: String, val merged: File) : MergedFastqReplicate
data class MergedFastqReplicatePE(override val name: String, val mergedR1: File, val mergedR2: File) : MergedFastqReplicate