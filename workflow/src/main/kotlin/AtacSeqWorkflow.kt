import krews.core.*
import krews.run
import model.*
import reactor.core.publisher.toFlux
import task.*
fun main(args: Array<String>) = run(atacSeqWorkflow, args)

data class AtacSeqParams(
    val experiments: List<Experiment>,
    val tasks: List<String> = listOf("trim-adapter","bowtie2","filter-alignments","bam2ta","macs2")
)

fun filterInput(v: BamReplicate): Bowtie2Output = Bowtie2Output( v.name, v.pairedend, v.bam!!, v.bam, v.bam, v.bam, v.bam )
fun bam2taInput(v: FilteredBamReplicate): FilterOutput = FilterOutput( v.name, v.pairedend, v.bam!!, v.bam, v.bam, v.bam, v.bam, v.bam )
fun macs2Input(v: TagAlignReplicate): Bam2taOutput = Bam2taOutput(v.ta!!, v.name, v.pairedend)

val atacSeqWorkflow = workflow("atac-seq-workflow") {

    val params = params<AtacSeqParams>()
    val forceSingleEnd = true 
    
    val trimAdaptorInputs =  params.experiments.flatMap {
    it.replicates
        .filter { (it is FastqReplicatePE || it is FastqReplicateSE) && ( params.tasks.contains("trim-adapter") ) }
            .map { TrimAdapterInput(it) }
    }.toFlux()
    val trimAdapterOutput = trimAdapterTask( "trim-adapter", trimAdaptorInputs)

    val bowtieInputs = params.experiments.flatMap {
    it.replicates
        .filter { (it is MergedFastqReplicateSE || it is MergedFastqReplicatePE)}
            .map { if(it is MergedFastqReplicateSE) TrimAdapterOutput(it.name, false, it.merged, null) else TrimAdapterOutput(it.name, true, (it as MergedFastqReplicatePE).mergedR1,(it as MergedFastqReplicatePE).mergedR2) }
    }.toFlux()
    
    val bowtie2Input = trimAdapterOutput.concatWith(bowtieInputs).filter { params.tasks.contains("bowtie2") }.map { Bowtie2Input(it.name, it.pairedEnd, it.mergedR1, it.mergedR2) }
    val bowtie2Output = bowtie2Task("bowtie2", bowtie2Input)
    
    val filterBamInput = params.experiments.flatMap {
    it.replicates
        .filter { it is BamReplicate && it.bam !== null }
        .map { filterInput(it as BamReplicate) }
    }.toFlux()

    val filterInput = bowtie2Output.concatWith(filterBamInput).filter {  params.tasks.contains("filter-alignments") }.map { FilterInput(it.bam, it.repName, it.pairedEnd) }
    val filterOutput = filterTask("filter-alignments", filterInput)

    val bam2taTaskInput = params.experiments.flatMap {
    it.replicates
        .filter { it is FilteredBamReplicate && it.bam !== null }
        .map { bam2taInput(it as FilteredBamReplicate) }
    }.toFlux()

    val bam2taInput = filterOutput.concatWith(bam2taTaskInput).filter { params.tasks.contains("bam2ta") }.map { Bam2taInput(it.bam, it.repName, if(forceSingleEnd) false else it.pairedEnd) }
    val bam2taOutput = bam2taTask("bam2ta", bam2taInput)

    val macs2TaskInput = params.experiments.flatMap {
    it.replicates
        .filter { it is TagAlignReplicate && it.ta !== null }
        .map { macs2Input(it as TagAlignReplicate) }
    }.toFlux()
    
    val macs2Input = bam2taOutput.concatWith(macs2TaskInput).filter { params.tasks.contains("macs2") }.map { Macs2Input(it.ta, it.repName, it.pairedEnd) }
    macs2Task("macs2",macs2Input)
}
