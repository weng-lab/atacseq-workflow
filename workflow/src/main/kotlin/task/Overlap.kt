package task

import krews.core.*
import krews.file.*
import org.reactivestreams.Publisher

data class OverlapParams(
        val chrsz: File,
        val blacklist: File,
        val regexBfiltPeakChrName: String = "chr[\\dXY]+"
)

// Note: Use same input as idr: IdrInput

data class OverlapOutput(
        val npeak: File,
        val bfiltNpeak: File,
        val bfiltNpeakBB: File,
        val fripQc: File
        //val bfiltHammock: File
)


fun WorkflowBuilder.overlapTask(i: Publisher<IdrInput>, peak: String) = this.task<IdrInput, OverlapOutput>("overlap-$peak", i, "overlap") {
    val params = taskParams<OverlapParams>()

    // NOTE: Same as Tsse task, this docker image should be shrunk and updated
    dockerImage = "dockerhub.reimonn.com:443/atac-seq-pipeline:v1.8.0"

    val prefix = "${input.exp}.${input.repName}"
    output =
            OverlapOutput(
                    npeak = OutputFile("overlap/$prefix.overlap.narrowPeak.gz"),
                    bfiltNpeak = OutputFile("overlap/$prefix.overlap.bfilt.narrowPeak.gz"),
                    bfiltNpeakBB = OutputFile("overlap/$prefix.overlap.bfilt.narrowPeak.bb"),
                    fripQc = OutputFile("overlap/$prefix.overlap.bfilt.frip.qc")
                    //bfiltHammock = OutputFile("overlap/$prefix.overlap.bfilt.narrowPeak.hammock.gz")
            )

    command =
            """
            encode_task_overlap.py \
                ${input.peaks1.dockerPath} ${input.peaks2.dockerPath} ${input.pooledPeak.dockerPath} \
                --prefix ${prefix} \
                --peak-type narrowPeak \
                --chrsz ${params.chrsz.dockerPath} \
                --blacklist ${params.blacklist.dockerPath} \
                --regex-bfilt-peak-chr-name ${"'"}${params.regexBfiltPeakChrName}${"'"} \
                --ta ${input.pooledTa.dockerPath} \
                --out-dir $outputsDir/overlap
            """
}
