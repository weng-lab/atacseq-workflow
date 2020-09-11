package task

import krews.core.*
import krews.file.*
import org.reactivestreams.Publisher

data class SignalParams(
    val smoothingFactor: Int = 30,
    val forwardShift: Int = 4,
    val reverseShift: Int = -5,
    val signalResolution: Int = 10,
    val chromosomeSizes: File
)

data class SignalInput(
    val bam: File,
    val index: File,
    val repName: String
)

data class SignalOutput(
    val signal: File
)

fun WorkflowBuilder.signalTask(name: String, i: Publisher<SignalInput>) = this.task<SignalInput, SignalOutput>(name, i) {
    val params = taskParams<SignalParams>()

    dockerImage = "genomealmanac/atacseq-zpeaks:v1.1.0"

    output = SignalOutput(
        signal = OutputFile("${input.repName}.bigWig")
    )

    command = """
        _JAVA_OPTIONS=-Xmx128G java -Xmx128G -jar /app/zpeaks.jar \
            -bamIn ${input.bam.dockerPath} \
            -smoothingFactor ${params.smoothingFactor} \
            -forwardShift ${params.forwardShift} \
            -reverseShift ${params.reverseShift} \
            -signalOut $outputsDir/${input.repName}.bedGraph \
            -signalResolution ${params.signalResolution} \
        && cat $outputsDir/${input.repName}.bedGraph | grep -v track | sort -k1,1 -k2,2n > $outputsDir/${input.repName}.sorted.bedGraph \
        && /data/common/tools/ucsc.v350/bedGraphToBigWig \
            $outputsDir/${input.repName}.sorted.bedGraph \
            ${params.chromosomeSizes.dockerPath} \
            $outputsDir/${input.repName}.bigWig
    """
}
