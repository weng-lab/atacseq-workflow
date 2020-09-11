import krews.core.*
import krews.run
import krews.file.LocalInputFile
import krews.file.File

import model.*
import reactor.core.publisher.toFlux
import reactor.core.publisher.Flux
import task.*
import java.nio.file.Files
import java.nio.file.Paths
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

fun main(args: Array<String>) = run(atacSeqWorkflow, args)

data class AtacSeqParams(
    val experiments: List<Experiment>,
    val tasks: List<String> = listOf("trim-adapter","bowtie2","filter-alignments","bam2ta","macs2"),
    val zpeaks: Boolean = false,
    val idr: Boolean = false
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
  
    // Individual true replicates
    val macs2TrInput = bam2taOutput.concatWith(macs2TaskInput).filter { params.tasks.contains("macs2") }.map { Macs2Input(it.ta, it.repName, it.pairedEnd) }
    val macs2TrOutput = macs2Task(macs2TrInput, "tr")

    // If we want to run idr or zpeaks, we require that the rep name be
    // in the format "<condition>-repX" for reps 1 or 2

    // A note for much of the code below:
    // We process each individual replicate separate for the first couple steps
    // (i.e. bowtie2, filter, bam2ta), but need to combine them. We can do this
    // with `groupBy` based on the condition name while also requiring two
    // replicates.
    // FIXME: It might be better to check that we have the replicates we
    // need before the workflow starts.

    if (params.idr) {
        val macs2CombinedOutput: Flux<Pair<String, Pair<File, File>>> = macs2TrOutput
            .map {
                val split = it.repName.split("-")
                Triple(split[0], split[1], it)
            }
            .groupBy { it.first }
            .flatMap { it.collectList() }
            .map {
                if (it.size != 2) {
                    log.error { "Invalid reps for ${it[0].first}" }
                    null
                } else {
                    Pair(it[0].first, Pair(it[0].third.npeak, it[1].third.npeak))
                }
            }
            .filter { it != null }
            .map { it!! }

        val poolTaInput = bam2taOutput
            .map {
                val split = it.repName.split("-")
                Triple(split[0], split[1], it)
            }
            .groupBy { it.first }
            .flatMap { it.collectList() }
            .map {
                if (it.size != 2) {
                    log.error { "Invalid reps for ${it[0].first}" }
                    null
                } else {
                    PoolTaInput(it[0].first, it[0].third.pairedEnd, it[0].third.ta, it[1].third.ta)
                }
            }
            .filter { it != null }
            .map { it!! }
        val poolTaOutput = poolTaTask(poolTaInput)

        val macs2PrInput = poolTaOutput.map { Macs2Input(it.pooledTa, it.repName, if(forceSingleEnd) false else it.pairedEnd) }
        // Combined pooled replicates
        val macs2PrOutput = macs2Task(macs2PrInput, "pr")

        // IDR needs (for ATAC) true replicate peaks, pooled replicate peaks,
        // and pooled tagAlign files (for FRIP QC). Unfortunately, there's not a
        // `zipByKey` flux operator (and we can't just use `zip` since order
        // isn't guaranteed here). Instead, we create a merged Flux of a single
        // type, and group by the condition.

        val idrInput = Flux.merge(
                poolTaOutput.map { IdrInputValue(it.repName, pooledTa = it.pooledTa) },
                macs2CombinedOutput.map { IdrInputValue(it.first, peaks = it.second) },
                macs2PrOutput.map { IdrInputValue(it.repName, pooledPeaks = it.npeak)}
            )
            .groupBy { it.repName }
            .flatMap { it.collectList() }
            .map {
                val repName = it[0].repName
                var pooledTa: File? = null
                var peaks: Pair<File, File>? = null
                var pooledPeaks: File? = null
                for (i in it) {
                    // FIXME: These preconditions we could check before we
                    // start the workflow.
                    if (i.pooledTa != null) {
                        if (pooledTa != null) throw Exception("Too many pooled tas.")
                        pooledTa = i.pooledTa
                    }
                    if (i.peaks != null)
                        if (peaks != null) throw Exception("Too many peaks.")
                        peaks = i.peaks
                    }
                    if (i.pooledPeaks != null) {
                        if (pooledPeaks != null) throw Exception("Too many pooled peaks.")
                        pooledPeaks = i.pooledPeaks
                    }
                }
                // Any of these *could* occur if any of the previous tasks fail.
                if (pooledTa == null) {
                    log.error { "No pooled ta for $repName." }
                    null
                } else if (peaks == null) {
                    log.error { "No peaks for $repName." }
                    null
                } else if (pooledPeaks == null) {
                    log.error { "No pooled peaks for $repName." }
                    null
                } else {
                    IdrInput(repName, peaks.first, peaks.second, pooledTa, pooledPeaks)
                }
            }
            .filter { it != null }
            .map { it!! }
        idrTask(idrInput)
    }
 
    if (params.zpeaks) {
        val zpeaksInput = filterOutput
            .map {
                val split = it.repName.split("-")
                Triple(split[0], split[1], it)
            }
            .groupBy { it.first }
            .flatMap { it.collectList() }
            .map {
                if (it.size != 2) {
                    log.error { "Invalid reps for ${it[0].first}" }
                    null
                } else {
                    ZPeaksInput(it[0].first, it[0].third.bam, it[0].third.bai, it[1].third.bam, it[1].third.bai)
                }
            }
            .filter { it != null}
            .map { it!! }
        zpeaksTask(zpeaksInput)
    }

}

data class IdrInputValue(
    val repName: String,
    val pooledTa: File? = null,
    val peaks: Pair<File, File>? = null,
    val pooledPeaks: File? = null
)

data class NoDupBam(
    val name: String,
    val bam1: LocalInputFile,
    val bam1Index: LocalInputFile,
    val bam2: LocalInputFile,
    val bam2Index: LocalInputFile
)
