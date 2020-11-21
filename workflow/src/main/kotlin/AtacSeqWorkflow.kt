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
    // What does this section do?
    val experiments: List<Experiment>,
    // ommitted: spr, idr, zpeaks
    val tasks: List<String> = listOf("trim-adapter","bowtie2","filter-alignments","bam2ta","macs2")
)


fun filterInput(exp: String, v: BamReplicate): Bowtie2Output = Bowtie2Output( exp, v.name, v.pairedend, v.bam!!, null, null)
fun bam2taInput(exp: String, v: FilteredBamReplicate): FilterOutput = FilterOutput( exp, v.name, v.pairedend, v.bam!!, null, null, null, null)
fun tsseInput(exp: String, v: FilteredBamReplicate): FilterOutput = FilterOutput( exp, v.name, v.pairedend, v.bam!!, null, null, null, null)
fun macs2Input(exp: String, v: TagAlignReplicate): Bam2taOutput = Bam2taOutput(exp, v.ta!!, v.name, v.pairedend)
// Jack, I don't know what these functions do, and I'm having trouble figuring out how to make inputs for the Plots task
//fun plotsInput(exp: String, v: bfiltNpeak): IdrOutput = IdrOutput(exp, v.name, v.npeak, v.bfiltNpeak, v.bfiltNpeakBB, v.fripQc, v.idrPlot, v.idrUnthresholdedPeak)

val atacSeqWorkflow = workflow("atac-seq-workflow") {
    val params = params<AtacSeqParams>()

    // TRIM ADAPTER TASK
    val trimAdaptorInputs =  params.experiments.flatMap { exp ->
        exp.replicates
            .filter { (it is FastqReplicatePE || it is FastqReplicateSE) && ( params.tasks.contains("trim-adapter") ) }
            .map { TrimAdapterInput(exp.name, it) }
    }.toFlux()
    val trimAdapterOutput = trimAdapterTask( "trim-adapter", trimAdaptorInputs)


    // BOWTIE2 TASK
    val bowtieInputs = params.experiments.flatMap { exp ->
        exp.replicates
            .filter { (it is MergedFastqReplicateSE || it is MergedFastqReplicatePE)}
            .map { if(it is MergedFastqReplicateSE) TrimAdapterOutput(exp.name, it.name, false, it.merged, null) else TrimAdapterOutput(exp.name, it.name, true, (it as MergedFastqReplicatePE).mergedR1,(it as MergedFastqReplicatePE).mergedR2) }
    }.toFlux()
    // Run bowtie2 alignment
    val bowtie2Input = trimAdapterOutput.concatWith(bowtieInputs).filter { params.tasks.contains("bowtie2") }.map { Bowtie2Input(it.exp, it.repName, it.pairedEnd, it.mergedR1, it.mergedR2) }
    val bowtie2Output = bowtie2Task("bowtie2", bowtie2Input)
    // Run mito_only bowtie2 alignment
    val mitoBowtie2Input = trimAdapterOutput.concatWith(bowtieInputs).filter { params.tasks.contains("mito-bowtie2") }.map { Bowtie2Input(it.exp, "${it.repName}-mito", it.pairedEnd, it.mergedR1, it.mergedR2) }
    val mitoBowtie2Output = bowtie2Task("mito-bowtie2", mitoBowtie2Input)


    // FILTER TASK
    val filterBamInput = params.experiments.flatMap { exp ->
        exp.replicates
            .filter { it is BamReplicate && it.bam !== null }
            .map { filterInput(exp.name, it as BamReplicate) }
    }.toFlux()
    val filterInput = bowtie2Output.concatWith(filterBamInput).filter {  params.tasks.contains("filter-alignments") }.map { FilterInput(it.exp, it.bam, it.repName, it.pairedEnd) }
    val filterOutput = filterTask("filter-alignments", filterInput)

    //BAM2TA task
    val bam2taTaskInput = params.experiments.flatMap { exp ->
        exp.replicates
            .filter { it is FilteredBamReplicate && it.bam !== null }
            .map { bam2taInput(exp.name, it as FilteredBamReplicate) }
    }.toFlux()
    val bam2taInput = filterOutput.concatWith(bam2taTaskInput).filter { params.tasks.contains("bam2ta") }.map { Bam2taInput(it.exp, it.bam, it.repName, it.pairedEnd) }
    val bam2taOutput = bam2taTask("bam2ta", bam2taInput)

    // TSS ENRICHMENT TASK
    val tsseTaskInput = params.experiments.flatMap { exp ->
        exp.replicates
            .filter { it is FilteredBamReplicate && it.bam !== null }
            .map { tsseInput(exp.name, it as FilteredBamReplicate) }
    }.toFlux()
    val tsseInput = filterOutput.concatWith(tsseTaskInput).filter { params.tasks.contains("tsse") }.map { TsseInput(it.exp, it.bam, it.repName, it.pairedEnd) }
    val tsseOutput = tsseTask("tsse", tsseInput)

    // MACS2 TASK
    val macs2TaskInput = params.experiments.flatMap { exp ->
        exp.replicates
            .filter { it is TagAlignReplicate && it.ta !== null }
            .map { macs2Input(exp.name, it as TagAlignReplicate) }
    }.toFlux()


    // Individual true replicates
    val macs2TrInput = bam2taOutput.concatWith(macs2TaskInput).filter { params.tasks.contains("macs2") || params.tasks.contains("macs2-tr") }.map { Macs2Input(it.exp, it.ta, it.repName) }
    val macs2TrOutput = macs2Task(macs2TrInput, "tr")

    val sprInput = bam2taOutput.filter { params.tasks.contains("spr") }.map { SprInput(it.exp, it.repName, it.ta, it.pairedEnd) }
    val sprOut = sprTask(sprInput)

    // A note for much of the code below:
    // We process each individual replicate separate for the first couple steps
    // (i.e. bowtie2, filter, bam2ta), but need to combine them. We can do this
    // with `groupBy` based on the condition name while also requiring two
    // replicates.

    // Another note for below:
    // IDR needs (for ATAC) true replicate peaks, pooled replicate peaks,
    // and pooled tagAlign files (for FRIP QC). Unfortunately, there's not a
    // `zipByKey` flux operator (and we can't just use `zip` since order
    // isn't guaranteed here). Instead, we create a merged Flux of a single
    // type, and group by the condition.

    if (params.tasks.contains("idr")) {
        val macs2CombinedOutput: Flux<Pair<String, Pair<String, Pair<File, File>>>> = macs2TrOutput
            // Flux<Macs2Output> -> Flux<GroupedFlux<String, Macs2Output>>
            .groupBy { it.exp }
            // ... -> Flux<List<Macs2Output>>
            .flatMap { it.collectList() }
            // ... -> Flux<Triple<exp, combination, Pair<File, File>>>
            .flatMap {
                elementPairs(it).map {
                    val (first, second) = if (it.first.repName.compareTo(it.second.repName) < 0) {
                        Pair(it.first, it.second)
                    } else {
                        Pair(it.second, it.first)
                    }
                    Pair(first.exp, Pair("${first.repName}-${second.repName}", Pair(first.npeak, second.npeak)))
                }.toFlux()
            }

        val poolTaInput: Flux<PoolTaInput> = bam2taOutput
            .groupBy { it.exp }
            .flatMap { it.collectList() }
            .handle { it, sink ->
                if (it.size > 1) {
                    val sorted = it.sortedBy { it.repName }
                    sink.next(PoolTaInput(it[0].exp, it[0].pairedEnd, it[0].repName, sorted.map { it.ta }, "rep"))
                }
            }
        val poolTaOutput = poolTaTask(poolTaInput)

        val macs2PooledInput = poolTaOutput.map { Macs2Input(it.exp, it.pooledTa, "pooled") }
        // Combined pooled replicates
        val macs2PooledOutput = macs2Task(macs2PooledInput, "pooled")

        // There may be multiple pairs of true replicates per exp
        val allPeaks: MutableMap<String, MutableList<Pair<String, Pair<File, File>>>> = mutableMapOf()
        val allPooledTa: MutableMap<String, File> = mutableMapOf()
        val allPooledPeaks: MutableMap<String, File> = mutableMapOf()
        val idrInput: Flux<IdrInput> = Flux.merge(
            macs2CombinedOutput.map { IdrInputValue(it.first, peaks = it.second) },
            poolTaOutput.map { IdrInputValue(it.exp, pooledTa = it.pooledTa) },
            macs2PooledOutput.map { IdrInputValue(it.exp, pooledPeaks = it.npeak) }
        )
            .handle { it, sink ->
                if (it.peaks != null) {
                    val map = allPeaks.getOrPut(it.exp) { mutableListOf() }
                    map.add(it.peaks)
                }
                if (it.pooledTa != null) {
                    allPooledTa.put(it.exp, it.pooledTa)
                }
                if (it.pooledPeaks != null) {
                    allPooledPeaks.put(it.exp, it.pooledPeaks)
                }
                val pooledTa = allPooledTa.get(it.exp)
                val pooledPeaks = allPooledPeaks.get(it.exp)
                if (pooledTa != null && pooledPeaks != null) {
                    allPeaks
                        .getOrPut(it.exp) { mutableListOf() }
                        .takeWhile { true }
                        .forEach { peak ->
                            sink.next(IdrInput(it.exp, peak.first, peak.second.first, peak.second.second, pooledTa, pooledPeaks))
                        }
                }
            }
        val idrOutputTr = idrTask(idrInput, "tr")
        val overlapOutputTr = overlapTask(idrInput, "tr")

        val macs2PrInput = sprOut.flatMap {
            listOf(
                Macs2Input(it.exp, it.prOne, "${it.repName}-pr1"),
                Macs2Input(it.exp, it.prTwo, "${it.repName}-pr2")
            ).toFlux()
        }
        // Pseudo replicates
        val macs2PrOutput = macs2Task(macs2PrInput, "pr")

        val macs2PrOutputMerged: Flux<Pair<String, Pair<String, Pair<File, File>>>> = macs2PrOutput
            .groupBy {
                val repName = it.repName.split("-")[0]
                "${it.exp}-${repName}"
            }
            .flatMap { it.collectList() }
            .handle { it, sink ->
                // If not two, then one of the macs2 tasks failed; should be logged
                if (it.size == 2) {
                    val sorted = it.sortedBy { it.repName }
                    sink.next(Pair("${it[0].exp}.${it[0].repName.split("-")[0]}", Pair("pr", Pair(sorted[0].npeak, sorted[1].npeak))))
                }

            }

        // There will only be pair of pseudo-reps here, so we don't need to
        // keep the pooled-ta and pooled-peaks around once their used
        val allPeaksPr: MutableMap<String, Pair<File, File>> = mutableMapOf()
        val allPooledTaPr: MutableMap<String, File> = mutableMapOf()
        val allPooledPeaksPr: MutableMap<String, File> = mutableMapOf()
        val idrInputPr: Flux<IdrInput> = Flux.merge(
            macs2PrOutputMerged.map { IdrInputValue(it.first, peaks = it.second) },
            bam2taOutput.map { IdrInputValue("${it.exp}.${it.repName}", pooledTa = it.ta) },
            macs2TrOutput.map { IdrInputValue("${it.exp}.${it.repName}", pooledPeaks = it.npeak) }
        )
            .handle { it, sink ->
                if (it.peaks != null) {
                    allPeaksPr.put(it.exp, it.peaks.second)
                }
                if (it.pooledTa != null) {
                    allPooledTaPr.put(it.exp, it.pooledTa)
                }
                if (it.pooledPeaks != null) {
                    allPooledPeaksPr.put(it.exp, it.pooledPeaks)
                }
                if (allPeaksPr.get(it.exp) != null && allPooledTaPr.get(it.exp) != null && allPooledPeaksPr.get(it.exp) != null) {
                    val peaks = allPeaksPr.remove(it.exp)!!
                    val pooledTa = allPooledTaPr.remove(it.exp)!!
                    val pooledPeaks = allPooledPeaksPr.remove(it.exp)!!
                    sink.next(IdrInput(it.exp, "pr", peaks.first, peaks.second, pooledTa, pooledPeaks))
                }
            }
        val idrOutputPr = idrTask(idrInputPr, "pr")
        val overlapOutputPr = overlapTask(idrInputPr, "pr")
    }

    if (params.tasks.contains("zpeaks")) {
        val zpeaksInput = filterOutput
            .map {
                Triple(it.exp, it.repName, it)
            }
            .groupBy { it.first }
            .flatMap { it.collectList() }
            .flatMap {
                elementPairs(it).map {
                    val first = it.first
                    val second = it.second
                    ZPeaksInput("${first.first}.${first.second}-${second.second}", first.third.bam, first.third.bai, second.third.bam, second.third.bai)
                }.toFlux()
            }
        zpeaksTask(zpeaksInput)
    }

}

data class IdrInputValue(
    val exp: String,
    val pooledTa: File? = null,
    val peaks: Pair<String, Pair<File, File>>? = null,
    val pooledPeaks: File? = null
)

data class NoDupBam(
    val name: String,
    val bam1: LocalInputFile,
    val bam1Index: LocalInputFile,
    val bam2: LocalInputFile,
    val bam2Index: LocalInputFile
)

fun <T> elementPairs(arr: List<T>): Sequence<Pair<T, T>> = sequence {
    for(i in 0 until arr.size-1)
        for(j in i+1 until arr.size)
            yield(arr[i] to arr[j])
}
