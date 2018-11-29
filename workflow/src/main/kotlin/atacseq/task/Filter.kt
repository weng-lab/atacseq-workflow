package atacseq.task

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
        val pairedEnd: Boolean,
        val params: FilterParams
)

data class FilterOutput(
        val repName: String,
        val pairedEnd: Boolean,
        val noDupBam: File,
        val noDupBai: File,
        val flagstateQC: File,
        val dupQC: File?,
        val pbcQC: File?,
        val mitoDupLog: File?
)

fun WorkflowBuilder.filterTask(i: Publisher<FilterInput>) = this.task<FilterInput, FilterOutput>("filter-alignment") {
    dockerImage = "genomealmanac/atacseq-filter-alignment:1.0.0"
    input = i
    outputFn {
        val prefix = "filter/${inputEl.repName}"
        FilterOutput(
                repName = inputEl.repName,
                pairedEnd = inputEl.pairedEnd,
                noDupBam = OutputFile("$prefix.bam"),
                noDupBai = OutputFile("$prefix.bai"),
                flagstateQC = OutputFile("$prefix.flagstat.qc"),
                dupQC = if (inputEl.params.noDupRemoval) null else OutputFile("$prefix.dup.qc"),
                pbcQC = if (inputEl.params.noDupRemoval) null else OutputFile("$prefix.pbc.qc"),
                mitoDupLog = if (inputEl.params.noDupRemoval) null else OutputFile("$prefix.mito_dup.txt")
        )
    }
    commandFn {
        val params = inputEl.params
        """
        /app/encode_filter.py \
            ${inputEl.bam.dockerPath} \
            --out-dir $dockerDataDir/filter \
            --out-prefix ${inputEl.repName} \
            ${if (inputEl.pairedEnd) "--paired-end" else ""} \
            --multimapping ${params.multimapping} \
            --dup-marker ${params.dupMarker.name.toLowerCase()} \
            --mapq-thresh ${params.mapqThresh} \
            ${if (params.noDupRemoval) "--no-dup-removal" else ""} \
            --nth ${params.numThreads}
        """
    }
}