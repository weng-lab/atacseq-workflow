package task

import krews.core.WorkflowBuilder
import krews.file.File
import krews.file.OutputFile
import org.reactivestreams.Publisher

data class Bt2SamstatsParams(
        val sortThreadMem: Int = 8,
        val nth: Int = 4
)

data class Bt2SamstatsInput(
        val exp: String,
        val bam: File,
        val repName: String,
        val pairedEnd: Boolean
)

data class Bt2SamstatsOutput(
        val exp: String,
        val repName: String,
        val pairedEnd: Boolean,
        val samstatsQC: File
)

fun WorkflowBuilder.bt2SamstatsTask(name:String, i: Publisher<Bt2SamstatsInput>) = this.task<Bt2SamstatsInput, Bt2SamstatsOutput>(name, i) {
    val params = taskParams<Bt2SamstatsParams>()

    dockerImage = "genomealmanac/atacseq-filter-alignments:2.0.10"
    val prefix = "bowtie2/${input.exp}.${input.repName}"

    output =
            Bt2SamstatsOutput(
                    exp = input.exp,
                    repName = input.repName,
                    pairedEnd = input.pairedEnd,
                    samstatsQC = OutputFile("$prefix.samstats.qc")
            )

    command =
        """
        mkdir -p $outputsDir/bowtie2

        samtools sort -n -@ ${params.nth} -m ${params.sortThreadMem}G -O sam -T /tmp/srt-temp-bam ${input.bam.dockerPath}| \
          SAMstats --sorted_sam_file - --outf $outputsDir/bowtie2/${input.exp}.${input.repName}.samstats.qc

        ls -lh $outputsDir/bowtie2/
        """
}
