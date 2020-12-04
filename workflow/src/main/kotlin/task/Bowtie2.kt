package task

import krews.core.WorkflowBuilder
import krews.file.File
import krews.file.OutputFile
import model.*
import org.reactivestreams.Publisher

data class Bowtie2Params(
        val idxTar: File,
        val multimapping: Int? = 4,
        val scoreMin: String? = null,
        val memGb: Int = 8,
        val nth: Int = 4,
        val chrsz: File,
        val mitoChrName: String = "chrM",
        val includeMitoOutputs: Boolean = true

)

data class Bowtie2Input(
        val exp: String,
        val repName: String,
        val pairedEnd: Boolean,
        val mergedR1: File,
        val mergedR2: File? = null
)

data class Bowtie2Output(
        val exp: String,
        val repName: String,
        val pairedEnd: Boolean,
        val bam: File,
        val bai: File? = null,
        val samstatsQC: File? = null,
        val read_length: File? = null,
        val nonMitoSamstats: File? = null,
        val unqiueReadsQC: File? = null
)

fun WorkflowBuilder.bowtie2Task(name: String, i: Publisher<Bowtie2Input>) = this.task<Bowtie2Input, Bowtie2Output>(name, i) {
    val params = taskParams<Bowtie2Params>()

    dockerImage = "dockerhub.reimonn.com:443/atacseq-bowtie:2.0.0"

    val prefix = "bowtie2/${input.exp}.${input.repName}"
    output =
            Bowtie2Output(
                    exp = input.exp,
                    repName = input.repName,
                    pairedEnd = input.pairedEnd,
                    bam = OutputFile("$prefix.srt.bam"),
                    bai = if (params.includeMitoOutputs) OutputFile("$prefix.srt.bam.bai") else null,
                    samstatsQC = OutputFile("$prefix.srt.samstats.qc"),
                    read_length = if (params.includeMitoOutputs) OutputFile("$prefix.R1.merged.read_length.txt") else null,
                    nonMitoSamstats = if (params.includeMitoOutputs) OutputFile("$prefix.srt.no_chrM.samstats.qc") else null,
                    unqiueReadsQC = OutputFile("$prefix.unique_reads.qc")
            )

    command =
            """
            /app/encode_task_bowtie2.py \
                ${params.idxTar.dockerPath} \
                ${input.mergedR1.dockerPath} \
                ${if (input.pairedEnd) "${input.mergedR2!!.dockerPath}" else ""} \
                ${if (input.pairedEnd) "--paired-end" else ""} \
                --multimapping ${params.multimapping} \
                --mem-gb ${params.memGb} \
                --nth ${params.nth} \
                --out-dir $outputsDir/bowtie2 \
                --output-prefix ${input.exp}.${input.repName}

            /app/encode_task_post_align.py \
                --chrsz ${params.chrsz.dockerPath} \
                --mito-chr-name ${params.mitoChrName} \
                --nth ${params.nth} \
                --out-dir $outputsDir/bowtie2 \
                ${input.mergedR1.dockerPath} $outputsDir/bowtie2/${input.exp}.${input.repName}.srt.bam

            mv $outputsDir/bowtie2/non_mito/*.qc $outputsDir/bowtie2/

            samtools view -@ ${params.nth} -q 255 -f 2 $outputsDir/bowtie2/${input.exp}.${input.repName}.srt.bam | wc -l > $outputsDir/bowtie2/${input.exp}.${input.repName}.unique_reads.qc

            ls -lh $outputsDir/bowtie2
            ls -lh $outputsDir/bowtie2/non_mito
            """
}
