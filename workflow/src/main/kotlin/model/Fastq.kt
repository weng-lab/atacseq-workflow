package model

import krews.file.File

data class FastqReplicateSE(override val name: String, val fastqs: List<File>, val adaptor: File? = null) : Replicate
data class FastqReplicatePE(
        override val name: String,
        val fastqsR1: List<File>,
        val fastqsR2: List<File>,
        val adaptorR1: File? = null,
        val adaptorR2: File? = null
) : Replicate


data class MergedFastqReplicateSE(override val name: String, val merged: File) : Replicate
data class MergedFastqReplicatePE(override val name: String, val mergedR1: File, val mergedR2: File) : Replicate