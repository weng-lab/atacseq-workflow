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
        val nth: Int = 4
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
        val bai: File,
        val alignLog: File,
        val flagstatQC: File,
        val readLenLog: File
)

fun WorkflowBuilder.bowtie2Task(name: String, i: Publisher<Bowtie2Input>) = this.task<Bowtie2Input, Bowtie2Output>(name, i) {
    val params = taskParams<Bowtie2Params>()

    dockerImage = "genomealmanac/atacseq-bowtie2:1.1.0"

    val prefix = "bowtie2/${input.exp}.${input.repName}"
    output =
            Bowtie2Output(
                    exp = input.exp,
                    repName = input.repName,
                    pairedEnd = input.pairedEnd,
                    bam = OutputFile("$prefix.bam"),
                    bai = OutputFile("$prefix.bam.bai"),
                    alignLog = OutputFile("$prefix.align.log"),
                    flagstatQC = OutputFile("$prefix.flagstat.qc"),
                    readLenLog = OutputFile("$prefix.read_length.txt")
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
                --nth ${params.nth}
            """
}