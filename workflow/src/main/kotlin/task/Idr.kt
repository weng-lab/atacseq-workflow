package task

import krews.core.*
import krews.file.*
import org.reactivestreams.Publisher

data class IdrParams(
        val chrsz: File,
        val blacklist: File,
        val idrThreshold: Double = 0.01,
        val fraglen: Int
)

data class IdrInput(
        val repName: String,
        val peaks1: File,
        val peaks2: File,
        val pooledTa: File,
        val pooledPeak: File
)

data class IdrOutput(
        val npeak: File,
        // FIXME: was running into errors
        //val bfiltNpeak: File,
        //val bfiltNpeakBB: File,
        val fripQc: File,
        // FIXME: idr task image needs matplotlib
        //val idrPlot: File,
        val idrUnthresholdedPeak: File
)

fun WorkflowBuilder.idrTask(i: Publisher<IdrInput>) = this.task<IdrInput, IdrOutput>("idr", i) {
    val params = taskParams<IdrParams>()

    dockerImage = "genomealmanac/chipseq-idr:v1.0.5"

    val prefix = "${input.repName}"
    val nPrefix = "idr/$prefix.idr${params.idrThreshold}"
    output =
            IdrOutput(
                    npeak = OutputFile("$nPrefix.narrowPeak.gz"),
                    //bfiltNpeak = OutputFile("$prefix.bfilt.narrowPeak.gz"),
                    //bfiltNpeakBB = OutputFile("$prefix.bfilt.narrowPeak.bb"),
                    fripQc = OutputFile("$nPrefix.bfilt.frip.qc"),
                    //idrPlot = OutputFile("$nPrefix.unthresholded-peaks.txt.png"),
                    idrUnthresholdedPeak = OutputFile("$nPrefix.unthresholded-peaks.txt.gz")
            )

    // FIXME: task was having problems with trying to unzip blacklist
    // in input directory
    command =
            """
            cp ${params.blacklist.dockerPath} $outputsDir/blacklist.bed.gz
            java -jar /app/chipseq.jar \
                -peak1 ${input.peaks1.dockerPath} \
                -peak2 ${input.peaks2.dockerPath} \
                -pooledPeak ${input.pooledPeak.dockerPath} \
                -outputPrefix ${prefix} \
                -idrthresh ${params.idrThreshold} \
                -peakType narrowPeak \
                -blacklist $outputsDir/blacklist.bed.gz \
                -ta ${input.pooledTa.dockerPath} \
                -chrsz ${params.chrsz.dockerPath} \
                -outputDir $outputsDir/idr
            ls -l
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