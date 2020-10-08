package model

interface Replicate {
    val name: String
}

data class Experiment (
    val name: String,
    val replicates: List<Replicate>
)
