package task

import krews.core.WorkflowBuilder
import krews.file.OutputFile
import krews.file.File
import model.*
import org.reactivestreams.Publisher

data class TrimAdapterParams(
        val cutAdaptParam: String = "-e 0.1 -m 5",
        val nth: Int = 4,
        val autoDetectAdapter: Boolean = false
)

data class TrimAdapterInput(
        val exp: String,
        val rep: Replicate
)

data class TrimAdapterOutput(
        val exp: String,
        val repName: String,
        val pairedEnd: Boolean,
        val mergedR1: File,
        val mergedR2: File?,
        val trimQC: File? = null
)

fun WorkflowBuilder.trimAdapterTask(name: String, i: Publisher<TrimAdapterInput>) = this.task<TrimAdapterInput, TrimAdapterOutput>(name, i) {
    val params = taskParams<TrimAdapterParams>()

    dockerImage = "dockerhub.reimonn.com:443/atacseq-trim-adapters:1.1.7"

    val rep = input.rep
    output =
            if (input.rep is FastqReplicateSE) {
                TrimAdapterOutput(
                        exp = input.exp,
                        repName = rep.name,
                        pairedEnd = false,
                        mergedR1 = OutputFile("trim/${input.exp}.${rep.name}.R1.merged.fastq.gz"),
                        mergedR2 = null,
                        trimQC = OutputFile("trim/${input.exp}.${rep.name}_CountReads.qc")
                )
            } else {
                TrimAdapterOutput(
                        exp = input.exp,
                        repName = rep.name,
                        pairedEnd = true,
                        mergedR1 = OutputFile("trim/${input.exp}.${rep.name}.R1.merged.fastq.gz"),
                        mergedR2 = OutputFile("trim/${input.exp}.${rep.name}.R2.merged.fastq.gz"),
                        trimQC = OutputFile("trim/${input.exp}.${rep.name}_CountReads.qc")
                )
            }

    val fastqs = if (rep is FastqReplicatePE) {
        rep.fastqsR1.zip(rep.fastqsR2).joinToString(" ") { it.toList().joinToString(",") { it.dockerPath } }
    } else if (rep is FastqReplicateSE) {
        rep.fastqs.joinToString(" ")
    } else {
        throw IllegalArgumentException("Expected PE or SE")
    }
    val adapters: String? = if (rep is FastqReplicatePE) {
        if (rep.adaptors != null) {
            rep.adaptors.dockerPath
        } else {
            rep.adaptorR1?.let { r1 ->
                rep.adaptorR2?.let { r2 ->
                    r1.zip(r2).joinToString(" ") { it.toList().joinToString(",") { it } }
                }
            }
        }
    } else if (rep is FastqReplicateSE) {
        if (rep.adaptors != null) {
            rep.adaptors.dockerPath
        } else {
            rep.adaptor?.let {
                it.joinToString(",") { it }
            }
        }
    } else {
        throw IllegalArgumentException("Expected PE or SE")
    }
    val adapter: String? = if (rep is FastqReplicatePE) {
        rep.adaptorAll
    } else if (rep is FastqReplicateSE) {
        rep.adaptorAll
    } else {
        throw IllegalArgumentException("Expected PE or SE")
    }
    command =
            """
            /app/encode_task_trim_adapter.py \
                ${fastqs} \
                ${if (adapters != null) "--adapters $adapters" else ""} \
                ${if (adapter != null) "--adapter $adapter" else ""} \
                ${if (rep is FastqReplicatePE) "--paired-end" else ""} \
                ${if (params.autoDetectAdapter) "--auto-detect-adapter" else ""} \
                --cutadapt-param ' ${params.cutAdaptParam}' \
                --nth ${params.nth} \
                --output-prefix ${input.exp}.${rep.name} \
                --out-dir $outputsDir/trim

            # Count input reads
            for f in ${'$'}(echo ${fastqs} | sed "s/,/ /g")
            do
                echo ${'$'}f
                echo ${'$'}((${'$'}(zcat ${'$'}f | wc -l) / 4))    ${'$'}(basename ${'$'}f) >> $outputsDir/trim/${input.exp}.${rep.name}_CountReads.qc
            done
            # Count trimmed reads
            for f in $outputsDir/trim/*.fastq.gz
            do
                echo ${'$'}f
                echo ${'$'}((${'$'}(zcat ${'$'}f | wc -l) / 4))    ${'$'}(basename ${'$'}f) >> $outputsDir/trim/${input.exp}.${rep.name}_CountReads.qc
            done
            """
}
