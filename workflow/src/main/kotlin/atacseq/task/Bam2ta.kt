package atacseq.task

import krews.core.*
import krews.file.*
import org.reactivestreams.Publisher

data class Bam2taParams(
        val disableTn5Shift: Boolean,
        val regexGrepVTA: String = "chrM",
        val subsample: Int = 0,
        @CacheIgnored val numThreads: Int = 1
)

data class Bam2taInput(
        val bam: File,
        val repName: String,
        val pairedEnd: Boolean,
        val params: Bam2taParams
)

data class Bam2taOutput(
        val ta: File,
        val repName: String,
        val pairedEnd: Boolean
)

fun WorkflowBuilder.bam2taTask(i: Publisher<Bam2taInput>) = this.task<Bam2taInput, Bam2taOutput>("bam2ta") {
    dockerImage = "genomealmanac/atacseq-bam2ta:1.0.0"
    input = i
    outputFn {
        Bam2taOutput(
                ta = OutputFile("bam2ta/${inputEl.repName}.tagAlign.gz"),
                repName = inputEl.repName,
                pairedEnd = inputEl.pairedEnd
        )
    }
    commandFn {
        val params = inputEl.params
        """
        /app/encode_bam2ta.py \
            ${inputEl.bam.dockerPath} \
            --out-dir $dockerDataDir/bam2ta \
            --out-prefix ${inputEl.repName} \
            ${if (inputEl.pairedEnd) "--paired-end" else ""} \
            ${if (params.disableTn5Shift) "--disable-tn5-shift" else ""} \
            --regex-grep-v-ta ${params.regexGrepVTA} \
            --subsample ${params.subsample} \
            --nth ${params.numThreads}
        """
    }
}