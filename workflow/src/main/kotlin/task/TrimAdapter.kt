package task

import krews.core.WorkflowBuilder
import krews.file.OutputFile
import krews.file.File
import model.*
import org.reactivestreams.Publisher

data class TrimAdapterParams(
        val minTrimLen: Int = 5,
        val errRate: Double = 0.1
)

data class TrimAdapterInput(
        val rep: Replicate
)

data class TrimAdapterOutput(
        val name: String,
        val pairedEnd: Boolean,
        val mergedR1: File,        
        val mergedR2: File?
)

fun WorkflowBuilder.trimAdapterTask(name: String, i: Publisher<TrimAdapterInput>) = this.task<TrimAdapterInput, TrimAdapterOutput>(name, i) {
    val params = taskParams<TrimAdapterParams>()

    dockerImage = "genomealmanac/atacseq-trim-adapters:1.0.8"

    val rep = input.rep
    output =
            if (input.rep is FastqReplicateSE) {
                TrimAdapterOutput(name = rep.name,pairedEnd = false, mergedR1 = OutputFile("trim/${rep.name}.merged.fastq.gz"), mergedR2 = null)
            } else {
                TrimAdapterOutput( name = rep.name, pairedEnd = true,
                        mergedR1 = OutputFile("trim/${rep.name}.R1.merged.fastq.gz"),
                        mergedR2 = OutputFile("trim/${rep.name}.R2.merged.fastq.gz")
                       )
            }

    val detectAdaptor = (rep is FastqReplicateSE && rep.adaptor == null) ||
            (rep is FastqReplicatePE && (rep.adaptorR1 == null || rep.adaptorR2 == null))
    command =
            """
            /app/encode_trim_adapter.py \
                --out-dir $outputsDir/trim \
                --output-prefix ${rep.name} \
                ${if (rep is FastqReplicateSE) "--fastqs ${rep.fastqs.joinToString(" ") { it.dockerPath }}" else ""} \
                ${if (rep is FastqReplicateSE && !detectAdaptor) "--adapter ${rep.adaptor!!.dockerPath}" else ""} \
                ${if (rep is FastqReplicatePE) "--fastqs-r1 ${rep.fastqsR1.joinToString(" ") { it.dockerPath }}" else ""} \
                ${if (rep is FastqReplicatePE) "--fastqs-r2 ${rep.fastqsR2.joinToString(" ") { it.dockerPath }}" else ""} \
                ${if (rep is FastqReplicatePE && !detectAdaptor) "--adapter-r1 ${rep.adaptorR1!!.dockerPath}" else ""} \
                ${if (rep is FastqReplicatePE && !detectAdaptor) "--adapter-r2 ${rep.adaptorR2!!.dockerPath}" else ""} \
                ${if (rep is FastqReplicatePE) "--paired-end" else ""} \
                ${if (detectAdaptor) "--auto-detect-adapter" else ""} \
                --min-trim-len ${params.minTrimLen} \
                --err-rate ${params.errRate}
            """
}