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
        val makeSignal: Boolean = true
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
    val sigPval: File?,
    val sigFc: File?,
    val fripQc: File
)

fun WorkflowBuilder.macs2Task(i: Publisher<Macs2Input>) = this.task<Macs2Input, Macs2Output>("macs2") {
    dockerImage = "genomealmanac/atacseq-macs2:1.0.2"
    input = i
    outputFn {
        val params = inputEl.params
        val prefix = "macs2/${inputEl.repName}"
        val npPrefix = "$prefix.pval${params.pvalThresh}.${capNumPeakFilePrefix(params.capNumPeak)}"
        Macs2Output(
                npeak = OutputFile("$npPrefix.narrowPeak.gz"),
                bfiltNpeak = OutputFile("$npPrefix.bfilt.narrowPeak.gz"),
                bfiltNpeakBB = OutputFile("$npPrefix.bfilt.narrowPeak.bb"),
                fripQc = OutputFile("$npPrefix.bfilt.frip.qc"),
                sigPval = if (params.makeSignal) OutputFile("$prefix.pval.signal.bigwig") else null,
                sigFc = if (params.makeSignal) OutputFile("$prefix.fc.signal.bigwig") else null
        )
    }
    commandFn {
        val params = inputEl.params
        """
        /app/encode_macs2_atac.py \
            ${inputEl.ta.dockerPath} \
            --out-dir $dockerDataDir/macs2 \
            --output-prefix ${inputEl.repName} \
            ${if (params.gensz != null) "--gensz ${params.gensz}" else ""} \
            --chrsz ${params.chrsz.dockerPath} \
            --cap-num-peak ${params.capNumPeak} \
            --pval-thresh ${params.pvalThresh} \
            --smooth-win ${params.smoothWin} \
            --blacklist ${params.blacklist.dockerPath} \
            ${if (params.makeSignal) "--make-signal" else "" } \
            ${if (inputEl.pairedEnd) "--paired-end" else "" }
        """
    }
}

private fun capNumPeakFilePrefix(capNumPeak: Int): String {
    var displayVal = capNumPeak
    var displayUnit = ""
    for (unit in listOf("K", "M", "G", "T", "P", "E")) {
        if (displayVal < 1000) break
        displayVal /= 1000
        displayUnit = unit
    }
    return "$displayVal$displayUnit"
}