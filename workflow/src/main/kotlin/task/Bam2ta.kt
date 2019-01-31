package task

import krews.core.WorkflowBuilder
import krews.file.File
import krews.file.OutputFile
import org.reactivestreams.Publisher

data class Bam2taParams(
        val disableTn5Shift: Boolean,
        val regexGrepVTA: String = "chrM",
        val subsample: Int = 0
)

data class Bam2taInput(
        val bam: File,
        val repName: String,
        val pairedEnd: Boolean
)

data class Bam2taOutput(
        val ta: File,
        val repName: String,
        val pairedEnd: Boolean
)

fun WorkflowBuilder.bam2taTask(i: Publisher<Bam2taInput>) = this.task<Bam2taInput, Bam2taOutput>("bam2ta", i) {
    val params = taskParams<Bam2taParams>()

    dockerImage = "genomealmanac/atacseq-bam2ta:1.0.4"

    output =
            Bam2taOutput(
                    ta = OutputFile("bam2ta/${input.repName}.tn5.tagAlign.gz"),
                    repName = input.repName,
                    pairedEnd = input.pairedEnd
            )

    command =
            """
            /app/encode_bam2ta.py \
                ${input.bam.dockerPath} \
                --out-dir $dockerDataDir/bam2ta \
                --output-prefix ${input.repName} \
                ${if (input.pairedEnd) "--paired-end" else ""} \
                ${if (params.disableTn5Shift) "--disable-tn5-shift" else ""} \
                --regex-grep-v-ta ${params.regexGrepVTA} \
                --subsample ${params.subsample}
            """
}
