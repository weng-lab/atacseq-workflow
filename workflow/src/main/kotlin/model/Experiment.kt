package model

interface Replicate {
    val name: String
}

data class Experiment (
    val replicates: List<Replicate>
)
