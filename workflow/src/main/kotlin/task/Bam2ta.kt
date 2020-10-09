package task

import krews.core.WorkflowBuilder
import krews.file.File
import krews.file.OutputFile
import org.reactivestreams.Publisher

data class Bam2taParams(
        val disableTn5Shift: Boolean = false,
        val regexGrepVTA: String = "chrM",
        val regexgreppta: String? = null,
        val subsample: Int = 0
)

data class Bam2taInput(
        val exp: String,
        val bam: File,
        val repName: String,
        val pairedEnd: Boolean
)

data class Bam2taOutput(
        val exp: String,
        val ta: File,
        val repName: String,
        val pairedEnd: Boolean
)

fun WorkflowBuilder.bam2taTask(name: String,i: Publisher<Bam2taInput>) = this.task<Bam2taInput, Bam2taOutput>(name, i) {
    val params = taskParams<Bam2taParams>()

    dockerImage = "genomealmanac/atacseq-bam2ta:v2.0.1"

    val prefix = "bam2ta/${input.exp}.${input.repName}"
    output =
            Bam2taOutput(
                    exp = input.exp,
                    ta = OutputFile("$prefix.tn5.tagAlign.gz"),
                    repName = input.repName,
                    pairedEnd = input.pairedEnd
            )

    command =
            """
            /app/encode_bam2ta.py \
                ${input.bam.dockerPath} \
                --out-dir $outputsDir/bam2ta \
                --output-prefix ${input.exp}.${input.repName} \
                ${if (input.pairedEnd) "--paired-end" else ""} \
                ${if (params.disableTn5Shift) "--disable-tn5-shift" else ""} \
                ${if (params.regexgreppta != null) "--regex-grep-p-ta ${params.regexgreppta}" else ""} \
                --regex-grep-v-ta ${params.regexGrepVTA} \
                --subsample ${params.subsample}
            """
}
