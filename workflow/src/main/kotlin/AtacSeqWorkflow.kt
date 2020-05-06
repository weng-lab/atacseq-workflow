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
        val samples: FastqSamples,
        val zpeaks: Boolean = false,
        val idr: Boolean = false
)

val atacSeqWorkflow = workflow("atac-seq-workflow") {
    val params = params<AtacSeqParams>()
    val forceSingleEnd = true

    val trimAdaptorInputs = params.samples.replicates
            .map { TrimAdapterInput(it) }
            .toFlux()
    val trimAdapterOutput = trimAdapterTask(trimAdaptorInputs)

    val bowtie2Input = trimAdapterOutput.map { Bowtie2Input(it.mergedReplicate) }
    val bowtie2Output = bowtie2Task(bowtie2Input)

    val filterInput = bowtie2Output.map { FilterInput(it.bam, it.repName, it.pairedEnd) }
    val filterOutput = filterTask(filterInput)

    val bam2taInput = filterOutput.map { Bam2taInput(it.bam, it.repName, if(forceSingleEnd) false else it.pairedEnd) }
    val bam2taOutput = bam2taTask(bam2taInput)

    // Individual true replicates
    val macs2TrInput = bam2taOutput.map { Macs2Input(it.ta, it.repName, it.pairedEnd) }
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
                        if (pooledTa != null) {
                            throw Exception("Too many pooled tas.")
                        }
                        pooledTa = i.pooledTa
                    }
                    if (i.peaks != null) {
                        if (peaks != null) {
                            throw Exception("Too many peaks.")
                        }
                        peaks = i.peaks
                    }
                    if (i.pooledPeaks != null) {
                        if (pooledPeaks != null) {
                            throw Exception("Too many pooled peaks.")
                        }
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
