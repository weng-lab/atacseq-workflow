package task

import krews.core.*
import krews.file.*
import org.reactivestreams.Publisher

data class IdrParams(
        val chrsz: File,
        val blacklist: File,
        val idrThreshold: Double = 0.05,
        val regexBfiltPeakChrName: String = "chr[\\dXY]+"
)

data class IdrInput(
        val exp: String,
        val repName: String,
        val peaks1: File,
        val peaks2: File,
        val pooledTa: File,
        val pooledPeak: File
)

data class IdrOutput(
        val npeak: File,
        val bfiltNpeak: File,
        val bfiltNpeakBB: File,
        val fripQc: File,
        val idrPlot: File,
        val idrUnthresholdedPeak: File
)

fun WorkflowBuilder.idrTask(i: Publisher<IdrInput>, peak: String) = this.task<IdrInput, IdrOutput>("idr-$peak", i, "idr") {
    val params = taskParams<IdrParams>()

    dockerImage = "genomealmanac/atacseq-idr:1.0.0"

    val prefix = "${input.exp}.${input.repName}"
    val nPrefix = "idr/$prefix.idr${params.idrThreshold}"
    output =
            IdrOutput(
                    npeak = OutputFile("$nPrefix.narrowPeak.gz"),
                    bfiltNpeak = OutputFile("$nPrefix.bfilt.narrowPeak.gz"),
                    bfiltNpeakBB = OutputFile("$prefix.bfilt.narrowPeak.bb"),
                    fripQc = OutputFile("$nPrefix.bfilt.frip.qc"),
                    idrPlot = OutputFile("$nPrefix.unthresholded-peaks.txt.png"),
                    idrUnthresholdedPeak = OutputFile("$nPrefix.unthresholded-peaks.txt.gz")
            )

    command =
            """
            /app/encode_task_idr.py \
                ${input.peaks1.dockerPath} ${input.peaks2.dockerPath} ${input.pooledPeak.dockerPath} \
                --prefix ${prefix} \
                --idr-thresh ${params.idrThreshold} \
                --peak-type narrowPeak \
                --idr-rank p.value \
                --chrsz ${params.chrsz.dockerPath} \
                --blacklist ${params.blacklist.dockerPath} \
                --regex-bfilt-peak-chr-name ${"'"}${params.regexBfiltPeakChrName}${"'"} \
                --ta ${input.pooledTa.dockerPath} \
                --out-dir $outputsDir/idr
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
    return "${displayVal.toFloat()}$displayUnit"
}
