package task

import krews.core.*
import krews.file.*
import org.reactivestreams.Publisher

enum class FilterDupMarker {
    PICARD, SAMBABA
}

data class FilterParams(
        val multimapping: Int = 0,
        val mapqThresh: Int = 30,
        val dupMarker: FilterDupMarker = FilterDupMarker.PICARD,
        val noDupRemoval: Boolean = false,
        @CacheIgnored val numThreads: Int = 1
)

data class FilterInput(
        val bam: File,
        val repName: String,
        val pairedEnd: Boolean
)

data class FilterOutput(
        val repName: String,
        val pairedEnd: Boolean,
        val bam: File,
        val bai: File,
        val flagstateQC: File,
        val dupQC: File?,
        val pbcQC: File?,
        val mitoDupLog: File?
)

fun WorkflowBuilder.filterTask(i: Publisher<FilterInput>) = this.task<FilterInput, FilterOutput>("filter-alignments", i) {
    val params = taskParams<FilterParams>()

    dockerImage = "genomealmanac/atacseq-filter-alignments:1.0.5"

    val prefix = "filter/${input.repName}"
    val noDupRemoval = params.noDupRemoval
    output =
            FilterOutput(
                    repName = input.repName,
                    pairedEnd = input.pairedEnd,
                    bam = if (noDupRemoval) OutputFile("$prefix.filt.bam") else OutputFile("$prefix.nodup.bam"),
                    bai = if (noDupRemoval) OutputFile("$prefix.filt.bam.bai") else OutputFile("$prefix.nodup.bam.bai"),
                    flagstateQC = if (noDupRemoval) OutputFile("$prefix.filt.flagstat.qc") else OutputFile("$prefix.nodup.flagstat.qc"),
                    dupQC = if (noDupRemoval) null else OutputFile("$prefix.dup.qc"),
                    pbcQC = if (noDupRemoval) null else OutputFile("$prefix.pbc.qc"),
                    mitoDupLog = if (noDupRemoval) null else OutputFile("$prefix.mito_dup.txt")
            )

    command =
        """
        /app/encode_filter.py \
            ${input.bam.dockerPath} \
            --out-dir $dockerDataDir/filter \
            --output-prefix ${input.repName} \
            ${if (input.pairedEnd) "--paired-end" else ""} \
            --multimapping ${params.multimapping} \
            --dup-marker ${params.dupMarker.name.toLowerCase()} \
            --mapq-thresh ${params.mapqThresh} \
            ${if (params.noDupRemoval) "--no-dup-removal" else ""} \
            --nth ${params.numThreads}
        """
}