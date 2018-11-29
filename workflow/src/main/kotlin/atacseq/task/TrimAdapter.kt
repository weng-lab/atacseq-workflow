package atacseq.task

import atacseq.model.*
import krews.core.*
import krews.file.*
import org.reactivestreams.Publisher

data class TrimAdapterParams(
        val minTrimLen: Int = 5,
        val errRate: Double = 0.1,
        @CacheIgnored val numThreads: Int = 1
)

data class TrimAdapterInput(
        val rep: FastqReplicate,
        val params: TrimAdapterParams
)

data class TrimAdapterOutput(
        val mergedReplicate: MergedFastqReplicate
)

fun WorkflowBuilder.trimAdaptorTask(i: Publisher<TrimAdapterInput>) = this.task<TrimAdapterInput, TrimAdapterOutput>("trim-adaptor") {
    dockerImage = "genomealmanac/atacseq-trim-adapters:1.0.1"
    input = i

    outputFn {
        val prefix = "trim/${inputEl.rep.name}"
        if (inputEl.rep is FastqReplicateSE) {
            val merged = MergedFastqReplicateSE(name = inputEl.rep.name, merged = OutputFile("$prefix.trim.fastq.gz"))
            TrimAdapterOutput(merged)
        } else {
            val merged = MergedFastqReplicatePE(
                    name = inputEl.rep.name,
                    mergedR1 = OutputFile("$prefix.R1.trim.fastq.gz"),
                    mergedR2 = OutputFile("$prefix.R2.trim.fastq.gz")
            )
            TrimAdapterOutput(merged)
        }
    }

    commandFn {
        val rep = inputEl.rep
        val detectAdaptor = (rep is FastqReplicateSE && rep.adaptor == null) ||
                (rep is FastqReplicatePE && (rep.adaptorR1 == null || rep.adaptorR2 == null))
        """
        /app/encode_trim_adaptor.py \
            --out-dir $dockerDataDir/trim \
            --out-prefix ${inputEl.rep.name} \
            ${if (rep is FastqReplicateSE) "--fastqs ${rep.merges.joinToString(" ") { it.dockerPath }}" else ""} \
            ${if (rep is FastqReplicateSE && !detectAdaptor) "--adapter ${rep.adaptor!!.dockerPath}" else ""} \
            ${if (rep is FastqReplicatePE) "--fastqs-r1 ${rep.fastqsR1.joinToString(" ") { it.dockerPath }}" else ""} \
            ${if (rep is FastqReplicatePE) "--fastqs-r2 ${rep.fastqsR2.joinToString(" ") { it.dockerPath }}" else ""} \
            ${if (rep is FastqReplicatePE && !detectAdaptor) "--adapter-r1 ${rep.adaptorR1!!.dockerPath}" else ""} \
            ${if (rep is FastqReplicatePE && !detectAdaptor) "--adapter-r2 ${rep.adaptorR2!!.dockerPath}" else ""} \
            ${if (detectAdaptor) "--auto-detect-adapter" else ""} \
            --min-trim-len ${inputEl.params.minTrimLen} \
            --err-rate ${inputEl.params.errRate} \
            --nth ${inputEl.params.numThreads}
        """
    }
}