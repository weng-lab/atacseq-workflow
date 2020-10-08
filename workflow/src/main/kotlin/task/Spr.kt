package task

import krews.core.*
import krews.file.*
import org.reactivestreams.Publisher

data class SprInput(
    val exp: String,
    val repName: String,
    val tagAlign: File,
    val pairedEnd: Boolean
)

data class SprOutput(
    val exp: String,
    val repName: String,
    val prOne: File,
    val prTwo: File,
    val pairedEnd: Boolean
)

fun WorkflowBuilder.sprTask(i: Publisher<SprInput>) = this.task<SprInput, SprOutput>("spr", i) {
    dockerImage = "genomealmanac/atacseq-spr:1.0.0"

    val prefix = "spr/${input.exp}.${input.repName}"
    output =
            SprOutput(
                exp = input.exp,
                repName = input.repName,
                prOne = OutputFile("$prefix.tn5.pr1.tagAlign.gz"),
                prTwo = OutputFile("$prefix.tn5.pr2.tagAlign.gz"),
                pairedEnd = input.pairedEnd
            )

    command =
            """
            /app/encode_spr.py \
                --out-dir $outputsDir/spr \
                ${if (input.pairedEnd) "--paired-end" else ""} \
                ${input.tagAlign.dockerPath}
            """
}
