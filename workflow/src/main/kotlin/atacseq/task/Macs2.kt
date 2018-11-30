package atacseq.task

import krews.core.*
import krews.file.*
import org.reactivestreams.Publisher

data class Macs2Params(
        val chrsz: File,
        val blacklist: File,
        val gensz: String? = null,
        val capNumPeak: Int = 300_000,
        val pvalThresh: Double = 0.01,
        val smoothWin: Int = 150,
        val makeSignal: Boolean = true,
        @CacheIgnored val numThreads: Int = 1
)

data class Macs2Input(
        val ta: File,
        val repName: String,
        val pairedEnd: Boolean,
        val params: Macs2Params
)

data class Macs2Output(
    val npeak: File,
    val bfiltNpeak: File,
    val bfiltNpeakBB: File,
    val bfiltNpeakHammock: File,
    val sigPval: File?,
    val sigFc: File?,
    val fripQc: File
)

fun WorkflowBuilder.macs2Task(i: Publisher<Macs2Input>) = this.task<Macs2Input, Macs2Output>("macs2") {
    dockerImage = "genomealmanac/atacseq-bam2ta:1.0.2"
    input = i
    outputFn {
        val prefix = "macs2/${inputEl.repName}"
        Macs2Output(
                npeak = OutputFile("$prefix.narrowPeak.gz"),
                bfiltNpeak = OutputFile("$prefix.bfilt.narrowPeak.gz"),
                bfiltNpeakBB = OutputFile("$prefix.bfilt.narrowPeak.bb"),
                bfiltNpeakHammock = OutputFile("$prefix.bfilt.narrowPeak.hammock.gz"),
                sigPval = if (inputEl.params.makeSignal) OutputFile("$prefix.pval.signal.bigwig") else null,
                sigFc = if (inputEl.params.makeSignal) OutputFile("$prefix.fc.signal.bigwig") else null,
                fripQc = OutputFile("$prefix.frip.qc")
        )
    }
    commandFn {
        val params = inputEl.params
        """
        /app/encode_macs2_atac.py \
            ${inputEl.ta.dockerPath} \
            --out-dir $dockerDataDir/macs2 \
            --out-prefix ${inputEl.repName} \
            ${if (params.gensz != null) "--genz ${params.gensz}" else ""} \
            --chrsz ${params.chrsz.dockerPath} \
            --cap-num-peak ${params.capNumPeak} \
            --pval-thresh ${params.pvalThresh} \
            --smooth-win ${params.smoothWin} \
            --blacklist ${params.blacklist.dockerPath} \
            ${if (params.makeSignal) "--make-signal" else "" } \
            ${if (inputEl.pairedEnd) "--paired-end" else "" } \
            --nth ${inputEl.params.numThreads}
        """
    }
}