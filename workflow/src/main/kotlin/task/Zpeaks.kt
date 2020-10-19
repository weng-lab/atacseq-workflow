package task

import krews.core.*
import krews.file.*
import org.reactivestreams.Publisher

data class ZPeaksInput(
        val name: String,
        val bamRep1: File,
        val bamRep1Index: File?,
        val bamRep2: File,
        val bamRep2Index: File?
)

data class ZPeaksOutput(
        val peaks: File
        // No signal for replicated peaks
        //val signal: File
)

fun WorkflowBuilder.zpeaksTask(i: Publisher<ZPeaksInput>) = this.task<ZPeaksInput, ZPeaksOutput>("zpeaks", i) {
    dockerImage = "genomealmanac/zpeaks:1.1.0"

    val peaksOut = OutputFile("zpeaks/${input.name}.zpeaks.peaks.bed")
    //val signalOut = OutputFile("zpeaks/${input.name}.zpeaks.signal.bedGraph", optional = true)
    output =
            ZPeaksOutput(
                    peaks = peaksOut
                    //signal = signalOut
            )

    command =
            """
            mkdir -p $(dirname ${peaksOut.dockerPath})
            java -jar /app/zpeaks.jar \
                -bamIn ${input.bamRep1.dockerPath} \
                -bamIn ${input.bamRep2.dockerPath} \
                -peaksOut ${peaksOut.dockerPath} \
                -parallelism 15 \
                -forwardShift 4 \
                -reverseShift -5 \
                -smoothingFactor 50 \
                -signalResolution 8 \
                -replicated
                """
}
