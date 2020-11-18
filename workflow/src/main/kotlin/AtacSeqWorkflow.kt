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
    // ommitted: spr, idr, zpeaks
    val tasks: List<String> = listOf("trim-adapter","bowtie2","filter-alignments","bam2ta","macs2")
)

fun filterInput(exp: String, v: BamReplicate): Bowtie2Output = Bowtie2Output( exp, v.name, v.pairedend, v.bam!!)
fun bam2taInput(exp: String, v: FilteredBamReplicate): FilterOutput = FilterOutput( exp, v.name, v.pairedend, v.bam!!, v.bai!!, v.bam, v.bai, null)
fun tsseInput(exp: String, v: FilteredBamReplicate): FilterOutput = FilterOutput( exp, v.name, v.pairedend, v.bam!!, v.bai!!, v.bam, v.bai, null)
fun macs2Input(exp: String, v: TagAlignReplicate): Bam2taOutput = Bam2taOutput(exp, v.ta!!, v.name, v.pairedend)

val atacSeqWorkflow = workflow("atac-seq-workflow") {
    val params = params<AtacSeqParams>()


    val trimAdaptorInputs =  params.experiments.flatMap { exp ->
        exp.replicates
            .filter { (it is FastqReplicatePE || it is FastqReplicateSE) && ( params.tasks.contains("trim-adapter") ) }
            .map { TrimAdapterInput(exp.name, it) }
    }.toFlux()
    val trimAdapterOutput = trimAdapterTask( "trim-adapter", trimAdaptorInputs)

    val bowtieInputs = params.experiments.flatMap { exp ->
        exp.replicates
            .filter { (it is MergedFastqReplicateSE || it is MergedFastqReplicatePE)}
            .map { if(it is MergedFastqReplicateSE) TrimAdapterOutput(exp.name, it.name, false, it.merged, null) else TrimAdapterOutput(exp.name, it.name, true, (it as MergedFastqReplicatePE).mergedR1,(it as MergedFastqReplicatePE).mergedR2) }
    }.toFlux()


    val bowtie2Input = trimAdapterOutput.concatWith(bowtieInputs).filter { params.tasks.contains("bowtie2") }.map { Bowtie2Input(it.exp, it.repName, it.pairedEnd, it.mergedR1, it.mergedR2) }
    val bowtie2Output = bowtie2Task("bowtie2", bowtie2Input)

    val filterBamInput = params.experiments.flatMap { exp ->
        exp.replicates
            .filter { it is BamReplicate && it.bam !== null }
            .map { filterInput(exp.name, it as BamReplicate) }
    }.toFlux()


    val filterInput = bowtie2Output.concatWith(filterBamInput).filter {  params.tasks.contains("filter-alignments") }.map { FilterInput(it.exp, it.bam, it.repName, it.pairedEnd) }
    val filterOutput = filterTask("filter-alignments", filterInput)

    val bam2taTaskInput = params.experiments.flatMap { exp ->
        exp.replicates
            .filter { it is FilteredBamReplicate && it.bam !== null }
            .map { bam2taInput(exp.name, it as FilteredBamReplicate) }
    }.toFlux()
    val bam2taInput = filterOutput.concatWith(bam2taTaskInput).filter { params.tasks.contains("bam2ta") }.map { Bam2taInput(it.exp, it.bam, it.repName, it.pairedEnd) }
    val bam2taOutput = bam2taTask("bam2ta", bam2taInput)


    val tsseTaskInput = params.experiments.flatMap { exp ->
        exp.replicates
            .filter { it is FilteredBamReplicate && it.bam !== null }
            .map { tsseInput(exp.name, it as FilteredBamReplicate) }
    }.toFlux()
    val tsseInput = filterOutput.concatWith(tsseTaskInput).filter { params.tasks.contains("tsse") }.map { TsseInput(it.exp, it.bam, it.repName, it.pairedEnd) }
    val tsseOutput = tsseTask("tsse", tsseInput)

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
        val macs2CombinedOutput: Flux<Triple<String, String, Pair<File, File>>> = macs2TrOutput
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
                    Triple(first.exp, "${first.repName}-${second.repName}", Pair(first.npeak, second.npeak))
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

        val idrInput = Flux.merge(
            macs2CombinedOutput.map { IdrInputValue(it.first, peaks = Pair(it.second, it.third)) },
            poolTaOutput.map { IdrInputValue(it.exp, pooledTa = it.pooledTa) },
            macs2PooledOutput.map { IdrInputValue(it.exp, pooledPeaks = it.npeak) }
        )
            .groupBy { it.exp }
            .flatMap { it.collectList() }
            .flatMap {
                val exp = it[0].exp
                var peaks: MutableList<Pair<String, Pair<File, File>>> = mutableListOf()
                var pooledTa: File? = null
                var pooledPeaks: File? = null
                for (i in it) {
                    // FIXME: These preconditions we could check before we
                    // start the workflow.
                    if (i.peaks != null) {
                        peaks.add(i.peaks)
                    }
                    if (i.pooledTa != null) {
                        if (pooledTa != null) throw Exception("Too many pooled tas.")
                        pooledTa = i.pooledTa
                    }
                    if (i.pooledPeaks != null) {
                        if (pooledPeaks != null) throw Exception("Too many pooled peaks.")
                        pooledPeaks = i.pooledPeaks
                    }
                }
                // Any of these *could* occur if any of the previous tasks fail.
                if (peaks.isEmpty()) {
                    log.error { "No peaks for $exp." }
                    Flux.empty()
                } else if (pooledTa == null) {
                    log.error { "No pooled ta for $exp." }
                    Flux.empty()
                } else if (pooledPeaks == null) {
                    log.error { "No pooled peaks for $exp." }
                    Flux.empty()
                } else {
                    peaks.map { peak -> IdrInput(exp, peak.first, peak.second.first, peak.second.second, pooledTa!!, pooledPeaks!!) }.toFlux()
                }
            }
        idrTask(idrInput, "tr")
        overlapTask(idrInput, "tr")

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

        val idrInputPr: Flux<IdrInput> = Flux.merge(
            macs2PrOutputMerged.map { IdrInputValue(it.first, peaks = it.second) },
            bam2taOutput.map { IdrInputValue("${it.exp}.${it.repName}", pooledTa = it.ta) },
            macs2TrOutput.map { IdrInputValue("${it.exp}.${it.repName}", pooledPeaks = it.npeak) }
        )
            .groupBy { it.exp }
            .flatMap { it.collectList() }
            .handle { it, sink ->
                val exp = it[0].exp
                var peaks: Pair<File, File>? = null
                var pooledTa: File? = null
                var pooledPeaks: File? = null
                for (i in it) {
                    // FIXME: These preconditions we could check before we
                    // start the workflow.
                    if (i.peaks != null) {
                        if (peaks != null) throw Exception("Invalid state. Should only have a single pair of pr peaks per exp.rep")
                        peaks = i.peaks.second
                    }
                    if (i.pooledTa != null) {
                        if (pooledTa != null) throw Exception("Too many pooled tas.")
                        pooledTa = i.pooledTa
                    }
                    if (i.pooledPeaks != null) {
                        if (pooledPeaks != null) throw Exception("Too many pooled peaks.")
                        pooledPeaks = i.pooledPeaks
                    }
                }
                // Any of these *could* occur if any of the previous tasks fail.
                if (peaks == null) {
                    log.error { "No peaks for $exp." }
                } else if (pooledTa == null) {
                    log.error { "No pooled ta for $exp." }
                } else if (pooledPeaks == null) {
                    log.error { "No pooled peaks for $exp." }
                } else {
                    sink.next(IdrInput(exp, "pr", peaks.first, peaks.second, pooledTa!!, pooledPeaks!!))
                }
            }
        idrTask(idrInputPr, "pr")
        overlapTask(idrInputPr, "pr")
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
