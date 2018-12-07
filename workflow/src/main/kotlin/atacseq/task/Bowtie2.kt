package atacseq.task

import atacseq.model.*
import krews.core.*
import krews.file.*
import org.reactivestreams.Publisher

data class Bowtie2Params(
        val idxTar: File,
        val multimapping: Int? = 4,
        val scoreMin: String? = null,
        @CacheIgnored val numThreads: Int = 1
)

data class Bowtie2Input(
        val mergedRep: MergedFastqReplicate
)

data class Bowtie2Output(
        val repName: String,
        val pairedEnd: Boolean,
        val bam: File,
        val bai: File,
        val alignLog: File,
        val flagstatQC: File,
        val readLenLog: File
)

fun WorkflowBuilder.bowtie2Task(i: Publisher<Bowtie2Input>) = this.task<Bowtie2Input, Bowtie2Output>("bowtie2", i) {
    val params = taskParams<Bowtie2Params>()

    dockerImage = "genomealmanac/atacseq-bowtie2:1.0.3"

    val prefix = "bowtie2/${input.mergedRep.name}"
    output =
            Bowtie2Output(
                    repName = input.mergedRep.name,
                    pairedEnd = input.mergedRep is MergedFastqReplicatePE,
                    bam = OutputFile("$prefix.bam"),
                    bai = OutputFile("$prefix.bam.bai"),
                    alignLog = OutputFile("$prefix.align.log"),
                    flagstatQC = OutputFile("$prefix.flagstat.qc"),
                    readLenLog = OutputFile("$prefix.read_length.txt")
            )

    val mergedRep = input.mergedRep
    command =
            """
            /app/encode_bowtie2.py \
                ${params.idxTar.dockerPath} \
                --out-dir $dockerDataDir/bowtie2 \
                --output-prefix ${mergedRep.name} \
                ${if (mergedRep is MergedFastqReplicateSE) "--fastq ${mergedRep.merged.dockerPath}" else ""} \
                ${if (mergedRep is MergedFastqReplicatePE) "--fastq-r1 ${mergedRep.mergedR1.dockerPath}" else ""} \
                ${if (mergedRep is MergedFastqReplicatePE) "--fastq-r2 ${mergedRep.mergedR2.dockerPath}" else ""} \
                ${if (mergedRep is MergedFastqReplicatePE) "--paired-end" else ""} \
                ${if (params.scoreMin != null) "--score-min ${params.scoreMin}" else ""} \
                --multimapping ${params.multimapping} \
                --nth ${params.numThreads}
            """
}