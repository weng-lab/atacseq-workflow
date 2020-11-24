package task

import krews.core.*
import krews.file.*
import org.reactivestreams.Publisher

data class PoolTaInput(
        val exp: String,
        val pairedEnd: Boolean,
        val firstRepName: String,
        val tas: List<File>,
        val prefix: String
)

data class PoolTaOutput(
        val exp: String,
        val pairedEnd: Boolean,
        val pooledTa: File
)

fun WorkflowBuilder.poolTaTask(i: Publisher<PoolTaInput>) = this.task<PoolTaInput, PoolTaOutput>("pool-ta", i) {
    dockerImage = "dockerhub.reimonn.com:443/atacseq-poolta:1.1.2"

    val prefix = "pool-ta/${input.exp}.${input.prefix}"
    output =
            PoolTaOutput(
                    exp = input.exp,
                    pairedEnd = input.pairedEnd,
                    pooledTa = OutputFile("$prefix.pooled.tagAlign.gz")
            )

    val alltas = input.tas.map { it.dockerPath }.joinToString(" ")
    command =
            """
            /app/encode_pool_ta.py \
                ${alltas} \
                --out-dir $outputsDir/pool-ta \
                --prefix ${input.exp}.${input.prefix}
            """
}
