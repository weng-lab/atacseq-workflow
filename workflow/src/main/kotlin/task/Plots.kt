package task

import krews.core.WorkflowBuilder
import krews.file.File
import krews.file.OutputFile
import org.reactivestreams.Publisher

data class PlotsParams(
        val phyloP: File,
        //val cCREs: File, Not needed yet
        val rDHSs: File
)

data class PlotsInput(
        val exp: String,
        val repName: String,
        val bfiltNpeak: File
)

data class PlotsOutput(
        val aggConPlot: File,
        val histogram: File,
        val rankedOverlap: File
)

fun WorkflowBuilder.plotsTask(name:String, i: Publisher<PlotsInput>) = this.task<PlotsInput, PlotsOutput>(name, i) {
    val params = taskParams<PlotsParams>()

    dockerImage = "dockerhub.reimonn.com:443/pcre-code"
    val prefix = "plots/${input.exp}.${input.repName}"

    output =
            PlotsOutput(
                    aggConPlot = OutputFile("${prefix}_AggCon.png"),
                    histogram = OutputFile("${prefix}_Histogram.png"),
                    rankedOverlap = OutputFile("${prefix}_RankedOverlap.png")
            )

    command =
        """
        peak_width_histogram.py \
          -i ${input.bfiltNpeak.dockerPath} \
          -o $outputsDir/${prefix}

        ranked_overlap.py \
          -i ${input.bfiltNpeak.dockerPath} \
          -r ${params.rDHSs.dockerPath} \
          -o $outputsDir/${prefix}

        aggregate_conservation.py \
          -i ${input.bfiltNpeak.dockerPath}\
          -r ${params.rDHSs.dockerPath} \
          -b ${params.phyloP.dockerPath} \
          -o $outputsDir/${prefix}

        ls -lh $outputsDir
        """
}
