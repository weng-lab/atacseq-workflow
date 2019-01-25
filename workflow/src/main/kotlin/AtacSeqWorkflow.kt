import krews.core.*
import krews.run
import model.FastqSamples
import reactor.core.publisher.toFlux
import task.*

fun main(args: Array<String>) = run(atacSeqWorkflow, args)

data class AtacSeqParams(
        val samples: FastqSamples
)

/*data class MacsParams(
        val samples: List<Macs2Input>
)*/

val atacSeqWorkflow = workflow("atac-seq-workflow") {

    val params = params<AtacSeqParams>()
    val forceSingleEnd = true


    val trimAdaptorInputs = params.samples.replicates
            .map { TrimAdapterInput(it) }
            .toFlux()
    val trimAdapterTask = trimAdapterTask(trimAdaptorInputs)

    val bowtie2Input = trimAdapterTask.outputPub
            .map { Bowtie2Input(it.mergedReplicate) }
    val bowtie2Task = bowtie2Task(bowtie2Input)

    val filterInput = bowtie2Task.outputPub
            .map { FilterInput(it.bam, it.repName, it.pairedEnd) }
    val filterTask = filterTask(filterInput)

    val bam2taInput = filterTask.outputPub
            .map { Bam2taInput(it.bam, it.repName, if(forceSingleEnd) false else it.pairedEnd) }
    val bam2taTask = bam2taTask(bam2taInput)

    val macs2Input = bam2taTask.outputPub
            .map { Macs2Input(it.ta, it.repName, it.pairedEnd) }
    macs2Task(macs2Input)

    /*
    val params = params<MacsParams>()
    val macs2Input = params.samples.map {Macs2Input(it.ta,it.repName,it.pairedEnd)}.toFlux()
    macsTask(macs2Input)
    */
}
