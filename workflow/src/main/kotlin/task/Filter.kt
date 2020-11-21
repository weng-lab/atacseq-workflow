package task

import krews.core.WorkflowBuilder
import krews.file.File
import krews.file.OutputFile
import org.reactivestreams.Publisher

enum class FilterDupMarker {
    PICARD, SAMBABA
}

data class FilterParams(
        val multimapping: Int = 4,
        val dupMarker: FilterDupMarker = FilterDupMarker.PICARD,
        val mapqThresh: Int = 30,
        val filterChrs: List<String> = listOf("chrM", "MT"),
        val noDupRemoval: Boolean = false,
        val mitoChrName: String = "chrM",
        val memGb: Int = 8,
        val nth: Int = 4,
        val chrsz: File
)

data class FilterInput(
        val exp: String,
        val bam: File,
        val repName: String,
        val pairedEnd: Boolean
)

data class FilterOutput(
        val exp: String,
        val repName: String,
        val pairedEnd: Boolean,
        val bam: File,
        val bai: File? = null,
        val samstatsQC: File? = null,
        val dupQC: File? = null,
        val pbcQC: File? = null
)

fun WorkflowBuilder.filterTask(name:String, i: Publisher<FilterInput>) = this.task<FilterInput, FilterOutput>(name, i) {
    val params = taskParams<FilterParams>()

    dockerImage = "genomealmanac/atacseq-filter-alignments:2.0.10"
    val prefix = "filter/${input.exp}.${input.repName}"
    val noDupRemoval = params.noDupRemoval
    val bam_prefix = "$prefix${if (noDupRemoval) ".filt" else ".nodup" }${if (params.filterChrs.size > 0) ".no_${params.filterChrs.joinToString("_")}" else "" }"
    output =
            FilterOutput(
                    exp = input.exp,
                    repName = input.repName,
                    pairedEnd = input.pairedEnd,
                    bam = OutputFile("$bam_prefix.bam"),
                    bai = OutputFile("$bam_prefix.bam.bai"),
                    samstatsQC = OutputFile("$bam_prefix.samstats.qc"),
                    dupQC = if (noDupRemoval) null else OutputFile("$prefix.dup.qc"),
                    pbcQC = if (noDupRemoval) null else OutputFile("$prefix.dupmark.lib_complexity.qc")
            )

    command =
        """
        /app/encode_task_filter.py \
            ${input.bam.dockerPath} \
            ${if (input.pairedEnd) "--paired-end" else ""} \
            --multimapping ${params.multimapping} \
            --dup-marker ${params.dupMarker.name.toLowerCase()} \
            --mapq-thresh ${params.mapqThresh} \
            --filter-chrs ${params.filterChrs.joinToString(" ") { it }} \
            --chrsz ${params.chrsz.dockerPath} \
            ${if (params.noDupRemoval) "--no-dup-removal" else ""} \
            --mito-chr-name ${params.mitoChrName} \
            --mem-gb ${params.memGb} \
            --nth ${params.nth} \
            --picard-java-heap ${params.memGb}G \
            --out-dir $outputsDir/filter \
            --output-prefix ${input.exp}.${input.repName}
        """
}
