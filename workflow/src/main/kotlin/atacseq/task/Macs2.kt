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
    val bfiltNpeakHammock: File,
    val sigPval: File?,
    val sigFc: File?,
    val fripQc: File
)

fun WorkflowBuilder.macs2Task(i: Publisher<Macs2Input>) = this.task<Macs2Input, Macs2Output>("macs2") {
    dockerImage = "genomealmanac/atacseq-bam2ta:1.0.0"
    input = i
    outputFn {
        Macs2Output(
                npeak = OutputFile(""),
                bfiltNpeak = OutputFile(""),
                bfiltNpeakBB = OutputFile(""),
                bfiltNpeakHammock = OutputFile(""),
                sigPval = if (inputEl.params.makeSignal) OutputFile("") else null,
                sigFc = if (inputEl.params.makeSignal) OutputFile("") else null,
                fripQc = OutputFile("")
        )
    }
    commandFn {
        val params = inputEl.params
        """
        /app/encode_macs2_atac.py \
            ${inputEl.ta.dockerPath} \
            --out-dir $dockerDataDir/${inputEl.outDir()} \
            --out-prefix output \
            ${if (params.gensz != null) "--genz ${params.gensz}" else ""} \
            --chrsz ${params.chrsz.dockerPath} \
            --cap-num-peak ${params.capNumPeak} \
            --pval-thresh ${params.pvalThresh} \
            --smooth-win ${params.smoothWin} \
            --blacklist ${params.blacklist.dockerPath} \
            ${if (params.makeSignal) "--make-signal" else "" }
            ${if (inputEl.pairedEnd) "--paired-end" else "" }
        """
    }
}

private fun Macs2Input.outDir() = "replicates/${this.repName}/macs2"