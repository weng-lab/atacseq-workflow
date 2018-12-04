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
        val mergedRep: MergedFastqReplicate,
        val params: Bowtie2Params
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

fun WorkflowBuilder.bowtie2Task(i: Publisher<Bowtie2Input>) = this.task<Bowtie2Input, Bowtie2Output>("bowtie2") {
    dockerImage = "genomealmanac/atacseq-bowtie2:1.0.3"
    input = i
    outputFn {
        val prefix = "bowtie2/${inputEl.mergedRep.name}"
        Bowtie2Output(
                repName = inputEl.mergedRep.name,
                pairedEnd = inputEl.mergedRep is MergedFastqReplicatePE,
                bam = OutputFile("$prefix.bam"),
                bai = OutputFile("$prefix.bam.bai"),
                alignLog = OutputFile("$prefix.align.log"),
                flagstatQC = OutputFile("$prefix.flagstat.qc"),
                readLenLog = OutputFile("$prefix.read_length.txt")
        )
    }
    commandFn {
        val mergedRep = inputEl.mergedRep
        """
        /app/encode_bowtie2.py \
            ${inputEl.params.idxTar.dockerPath} \
            --out-dir $dockerDataDir/bowtie2 \
            --output-prefix ${inputEl.mergedRep.name} \
            ${if (mergedRep is MergedFastqReplicateSE) "--fastq ${mergedRep.merged.dockerPath}" else ""} \
            ${if (mergedRep is MergedFastqReplicatePE) "--fastq-r1 ${mergedRep.mergedR1.dockerPath}" else ""} \
            ${if (mergedRep is MergedFastqReplicatePE) "--fastq-r2 ${mergedRep.mergedR2.dockerPath}" else ""} \
            ${if (mergedRep is MergedFastqReplicatePE) "--paired-end" else ""} \
            ${if (inputEl.params.scoreMin != null) "--score-min ${inputEl.params.scoreMin}" else ""} \
            --multimapping ${inputEl.params.multimapping} \
            --nth ${inputEl.params.numThreads}
        """
    }
}