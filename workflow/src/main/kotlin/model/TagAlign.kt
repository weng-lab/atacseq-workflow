package model

import krews.file.File

data class TagAlignReplicate (override val name: String, val ta: File? = null, val pairedend: Boolean) : Replicate

