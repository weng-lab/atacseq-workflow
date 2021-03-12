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
        val makeSignal: Boolean = true,
        val regexBfiltPeakChrName: String = "chr[\\dXY]+"
)

data class Macs2Input(
        val exp: String,
        val ta: File,
        val repName: String
)

data class Macs2Output(
        val exp: String,
        val repName: String,
        val npeak: File,
        val bfiltNpeak: File,
        val bfiltNpeakBB: File,
        val sigPval: File?,
        val sigFc: File?,
        val fripQc: File
)

fun WorkflowBuilder.macs2Task(i: Publisher<Macs2Input>, peak: String) = this.task<Macs2Input, Macs2Output>("macs2-$peak", i, "macs2") {
    val params = taskParams<Macs2Params>()

    dockerImage = "dockerhub.reimonn.com:443/atacseq-macs2:2.2.21"

    val prefix = "macs2/${input.exp}.${input.repName}"
    val npPrefix = "$prefix.pval${params.pvalThresh}.${capNumPeakFilePrefix(params.capNumPeak)}"
    val npeak = OutputFile("$npPrefix.narrowPeak.gz")
    output =
            Macs2Output(
                    exp = input.exp,
                    repName = input.repName,
                    npeak = npeak,
                    bfiltNpeak = OutputFile("$npPrefix.bfilt.narrowPeak.gz"),
                    bfiltNpeakBB = OutputFile("$npPrefix.bfilt.narrowPeak.bb"),
                    fripQc = OutputFile("$npPrefix.bfilt.frip.qc"),
                    sigPval = if (params.makeSignal) OutputFile("$prefix.pval.signal.bigwig") else null,
                    sigFc = if (params.makeSignal) OutputFile("$prefix.fc.signal.bigwig") else null
            )

    command =
            """
            /app/encode_task_macs2_atac.py \
                ${input.ta.dockerPath} \
                --out-dir $outputsDir/macs2 \
                --output-prefix ${input.exp}.${input.repName} \
                ${if (params.gensz != null) "--gensz ${params.gensz}" else ""} \
                --chrsz ${params.chrsz.dockerPath} \
                --cap-num-peak ${params.capNumPeak} \
                --pval-thresh ${params.pvalThresh} \
                --smooth-win ${params.smoothWin}

            /app/encode_task_post_call_peak_atac.py \
                ${npeak.dockerPath} \
                --out-dir $outputsDir/macs2 \
                --ta ${input.ta.dockerPath} \
                --regex-bfilt-peak-chr-name ${"'"}${params.regexBfiltPeakChrName}${"'"} \
                --chrsz ${params.chrsz.dockerPath} \
                --peak-type narrowPeak \
                ${if (params.blacklist != null) "--blacklist ${params.blacklist.dockerPath}" else "--blacklist /dev/null"}

            /app/encode_task_macs2_signal_track_atac.py \
                ${input.ta.dockerPath} \
                --out-dir $outputsDir/macs2 \
                --output-prefix ${input.exp}.${input.repName} \
                ${if (params.gensz != null) "--gensz ${params.gensz}" else ""} \
                --chrsz ${params.chrsz.dockerPath} \
                --pval-thresh ${params.pvalThresh} \
                --smooth-win ${params.smoothWin}
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
