package model

import krews.file.File

data class GeoInputReplicateSE(override val name: String, val geofile: File) : Replicate
data class GeoInputReplicatePE(override val name: String, val geofileR1: File, val geofileR2: File) : Replicate