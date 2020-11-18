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

    command =
            """
            cp ${input.bam.dockerPath} /tmp/nodup-temp-alignments.bam

            cd /tmp

            encode_task_tss_enrich.py \
                --read-len ${params.readLen} \
                --nodup-bam /tmp/nodup-temp-alignments.bam \
                --chrsz ${params.chrsz.dockerPath} \
                --out-dir $outputsDir/tsse \
                --tss ${params.tss.dockerPath}

            cp $outputsDir/tsse/nodup-temp-alignments.large_tss_enrich.png $outputsDir/tsse/${input.exp}.${input.repName}.large_tss_enrich.png
            cp $outputsDir/tsse/nodup-temp-alignments.tss_enrich.png $outputsDir/tsse/${input.exp}.${input.repName}.tss_enrich.png
            cp $outputsDir/tsse/nodup-temp-alignments.tss_enrich.qc $outputsDir/tsse/${input.exp}.${input.repName}.tss_enrich.qc
            """
}
