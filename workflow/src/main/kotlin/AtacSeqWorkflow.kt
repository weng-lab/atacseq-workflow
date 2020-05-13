import krews.core.*
import krews.run
import model.*
import reactor.core.publisher.toFlux
import task.*

fun main(args: Array<String>) = run(atacSeqWorkflow, args)

data class AtacSeqParams(
        val fastqsamples: FastqSamples?,
        val bamsamples: BamSamples?
)

/*data class MacsParams(
        val samples: List<Macs2Input>
)*/

val atacSeqWorkflow = workflow("atac-seq-workflow") {

    val params = params<AtacSeqParams>()
    val forceSingleEnd = true
    if(params.fastqsamples!==null) {       

        val trimAdaptorInputs = params.fastqsamples.replicates
                .map { TrimAdapterInput(it) }
                .toFlux()
        val trimAdapterOutput = trimAdapterTask(trimAdaptorInputs)

        val bowtie2Input = trimAdapterOutput.map { Bowtie2Input(it.mergedReplicate) }
        val bowtie2Output = bowtie2Task(bowtie2Input)

        val filterInput = bowtie2Output.map { FilterInput(it.bam, it.repName, it.pairedEnd) }
        val filterOutput = filterTask(filterInput)

        val bam2taInput = filterOutput.map { Bam2taInput(it.bam, it.repName, if(forceSingleEnd) false else it.pairedEnd) }
        val bam2taOutput = bam2taTask(bam2taInput)

        val macs2Input = bam2taOutput.map { Macs2Input(it.ta, it.repName, it.pairedEnd) }
        macs2Task(macs2Input)

    } else if(params.bamsamples!==null) {

        val filterInputs = params.bamsamples.alignments
                .map { value -> 
                    val bamFile:BamAlignmentFiles = value as BamAlignmentFiles
                    FilterInput(bamFile.bam,bamFile.name,bamFile.pairedend)                    
                }
                .toFlux()

        val filterOutput = filterTask(filterInputs)
        val bam2taInput = filterOutput.map { Bam2taInput(it.bam, it.repName, if(forceSingleEnd) false else it.pairedEnd) }
        val bam2taOutput = bam2taTask(bam2taInput)

        val macs2Input = bam2taOutput.map { Macs2Input(it.ta, it.repName, it.pairedEnd) }
        macs2Task(macs2Input)

    }
    /*
    val params = params<MacsParams>()
    val macs2Input = params.samples.map {Macs2Input(it.ta,it.repName,it.pairedEnd)}.toFlux()
    macsTask(macs2Input)
    */
}
