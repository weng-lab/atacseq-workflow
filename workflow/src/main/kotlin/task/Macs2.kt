package task

import krews.core.*
import krews.file.*
import org.reactivestreams.Publisher

data class Macs2Params(
        val chrsz: File,
        val blacklist: File?,
        val gensz: String? = null,
        val capNumPeak: Int = 300_000,
        val pvalThresh: Double = 0.01,
        val smoothWin: Int = 150,
        val makeSignal: Boolean = true
)

data class Macs2Input(
        val ta: File,
        val repName: String,
        val pairedEnd: Boolean
)

data class Macs2Output(
        val npeak: File,
        val bfiltNpeak: File,
        val bfiltNpeakBB: File,
        val sigPval: File?,
        val sigFc: File?,
        val fripQc: File
)

fun WorkflowBuilder.macs2Task(name: String, i: Publisher<Macs2Input>) = this.task<Macs2Input, Macs2Output>(name, i) {
    val params = taskParams<Macs2Params>()

    dockerImage = "genomealmanac/atacseq-macs2:1.0.6"

    val prefix = "macs2/${input.repName}"
    val npPrefix = "$prefix.pval${params.pvalThresh}.${capNumPeakFilePrefix(params.capNumPeak)}"
    output =
            Macs2Output(
                    npeak = OutputFile("$npPrefix.narrowPeak.gz"),
                    bfiltNpeak = OutputFile("$npPrefix.bfilt.narrowPeak.gz"),
                    bfiltNpeakBB = OutputFile("$npPrefix.bfilt.narrowPeak.bb"),
                    fripQc = OutputFile("$npPrefix.bfilt.frip.qc"),
                    sigPval = if (params.makeSignal) OutputFile("$prefix.pval.signal.bigwig") else null,
                    sigFc = if (params.makeSignal) OutputFile("$prefix.fc.signal.bigwig") else null
            )

    command =
            """
            /app/encode_macs2_atac.py \
                ${input.ta.dockerPath} \
                --out-dir $outputsDir/macs2 \
                --output-prefix ${input.repName} \
                ${if (params.gensz != null) "--gensz ${params.gensz}" else ""} \
                --chrsz ${params.chrsz.dockerPath} \
                --cap-num-peak ${params.capNumPeak} \
                --pval-thresh ${params.pvalThresh} \
                --smooth-win ${params.smoothWin} \
                ${if (params.blacklist != null) "--blacklist ${params.blacklist.dockerPath}" else "--blacklist /dev/null"} \
                ${if (params.makeSignal) "--make-signal" else ""} \
                ${if (input.pairedEnd) "--paired-end" else ""}
            """
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