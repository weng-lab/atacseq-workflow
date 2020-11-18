package task

import krews.core.WorkflowBuilder
import krews.file.File
import krews.file.OutputFile
import org.reactivestreams.Publisher

data class TsseParams(
        val readLen: Int = 100,
        val chrsz: File,
        val tss: File
)

data class TsseInput(
        val exp: String,
        val bam: File,
        //val bai: File,
        val repName: String,
        val pairedEnd: Boolean
)

data class TsseOutput(
        val exp: String,
        val repName: String,
        val pairedEnd: Boolean,
        val tssEnrichQc: File,
        val tssEnrichPng: File,
        val tssEnrichLargPng: File
)

fun WorkflowBuilder.tsseTask(name: String, i: Publisher<TsseInput>) = this.task<TsseInput, TsseOutput>(name, i) {
    val params = taskParams<TsseParams>()

    // NOTE: This image should probably be updated to a local smaller image under /tasks
    // This pulls the entire atac-seq-pipeline image created by the DCC
    dockerImage = "encodedcc/atac-seq-pipeline:v1.8.0"

    val prefix = "tsse/${input.exp}.${input.repName}"
    output =
            TsseOutput(
                    exp = input.exp,
                    repName = input.repName,
                    pairedEnd = input.pairedEnd,
                    tssEnrichQc = OutputFile("$prefix.tss_enrich.qc"),
                    tssEnrichPng = OutputFile("$prefix.tss_enrich.png"),
                    tssEnrichLargPng = OutputFile("$prefix.large_tss_enrich.png")
            )
    // This task needs a bam index, and it writes a new even if you copy the .bam.bai file
    // However /inputs is a read-only filesystem, so it has trouble writing the bai
    // As a hacky work-around, I copy the input to /tmp so that it can happily write a .bai file
    command =
            """
            cp ${input.bam.dockerPath} /tmp/nodup-temp-alignments.bam

            encode_task_tss_enrich.py \
                --read-len ${params.readLen} \
                --nodup-bam /tmp/nodup-temp-alignments.bam \
                --chrsz ${params.chrsz.dockerPath} \
                --out-dir $outputsDir/tsse \
                --tss ${params.tss.dockerPath}

            mv $outputsDir/tsse/nodup-temp-alignments.large_tss_enrich.png $outputsDir/tsse/${input.exp}.${input.repName}.large_tss_enrich.png
            mv $outputsDir/tsse/nodup-temp-alignments.tss_enrich.png $outputsDir/tsse/${input.exp}.${input.repName}.tss_enrich.png
            mv $outputsDir/tsse/nodup-temp-alignments.tss_enrich.qc $outputsDir/tsse/${input.exp}.${input.repName}.tss_enrich.qc
            """
}
