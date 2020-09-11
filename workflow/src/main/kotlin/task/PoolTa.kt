package task

import krews.core.*
import krews.file.*
import org.reactivestreams.Publisher

data class PoolTaInput(
        val repName: String,
        val pairedEnd: Boolean,
        val ta1: File,
        val ta2: File
)

data class PoolTaOutput(
        val repName: String,
        val pairedEnd: Boolean,
        val pooledTa: File
)

fun WorkflowBuilder.poolTaTask(i: Publisher<PoolTaInput>) = this.task<PoolTaInput, PoolTaOutput>("pool-ta", i) {
    dockerImage = "genomealmanac/atacseq-pool-ta:1.0.2"

    val prefix = "pool-ta/${input.repName}"
    output =
            PoolTaOutput(
                    repName = input.repName,
                    pairedEnd = input.pairedEnd,
                    pooledTa = OutputFile("$prefix-rep1.tn5.pooled.tagAlign.gz")
            )

    command =
            """
            /app/encode_pool_ta.py \
                --out-dir $outputsDir/pool-ta \
                ${input.ta1.dockerPath} ${input.ta2.dockerPath}
            """
}
