package model

import krews.file.File

data class BamReplicate (override val name: String, val bam: File? = null, val pairedend: Boolean) : Replicate

data class FilteredBamReplicate (override val name: String, val bam: File? = null, val bai: File? = null, val pairedend: Boolean) : Replicate
