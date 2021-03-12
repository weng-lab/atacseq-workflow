package task

import krews.core.WorkflowBuilder
import krews.file.File
import krews.file.OutputFile
import org.reactivestreams.Publisher

data class PlotsParams(
        val phylop: File,
        val ccre: File,
        val rdhs: File
)

data class PlotsInput(
        val exp: String,
        val repName: String,
        val bfiltNpeak: File
)

data class PlotsOutput(
        val aggConPlot: File,
        val histogram: File,
        val rankedOverlap: File,
        val peakQc: File
)

fun WorkflowBuilder.plotsTask(name:String, i: Publisher<PlotsInput>) = this.task<PlotsInput, PlotsOutput>(name, i) {
    val params = taskParams<PlotsParams>()

    dockerImage = "dockerhub.reimonn.com:443/plots:1.0.1"
    val prefix = "plots/${input.exp}.${input.repName}"

    output =
            PlotsOutput(
                    aggConPlot = OutputFile("${prefix}_AggCon.png"),
                    histogram = OutputFile("${prefix}_Histogram.png"),
                    rankedOverlap = OutputFile("${prefix}_RankedOverlap.png"),
                    peakQc = OutputFile("${prefix}_Statistics.qc")
            )

    command =
        """
        mkdir -p $outputsDir/plots

        peak_width_histogram.py \
          -i ${input.bfiltNpeak.dockerPath} \
          -o $outputsDir/${prefix}

        ranked_overlap.py \
          -i ${input.bfiltNpeak.dockerPath} \
          -r ${params.rdhs.dockerPath} \
          -o $outputsDir/${prefix}

        aggregate_conservation.py \
          -i ${input.bfiltNpeak.dockerPath} \
          -r ${params.rdhs.dockerPath} \
          -b ${params.phylop.dockerPath} \
          -o $outputsDir/${prefix}

        peak_stats.py \
          -i ${input.bfiltNpeak.dockerPath} \
          -r ${params.rdhs.dockerPath} \
          -c ${params.ccre.dockerPath} \
          -o $outputsDir/${prefix}

        ls -lh $outputsDir/plots
        """
}
