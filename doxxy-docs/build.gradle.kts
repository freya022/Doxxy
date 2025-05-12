plugins {
    id("doxxy-conventions")
    alias(libs.plugins.jmh)
}

dependencies {
    implementation(projects.doxxy.doxxyCommons)

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.bundles.slf4j)

    implementation(libs.okhttp)

    implementation(libs.jsoup)

    implementation(libs.javaparser.core)

    implementation(libs.jda)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.bytebuddy)

    testImplementation(libs.bundles.slf4j)
    testImplementation(libs.logback.classic)

    testImplementation(libs.ktor.server.core)
    testImplementation(libs.ktor.server.netty)
}

jmh {
//    includes = listOf("JsoupSelectorCachingBenchmark.parse") // include pattern (regular expression) for benchmarks to be executed
//    excludes = ['some regular expression'] // exclude pattern (regular expression) for benchmarks to be executed
//    iterations = 10 // Number of measurement iterations to do.
//    benchmarkMode = ['thrpt','ss'] // Benchmark mode. Available modes are: [Throughput/thrpt, AverageTime/avgt, SampleTime/sample, SingleShotTime/ss, All/all]
//    batchSize = 1 // Batch size: number of benchmark method calls per operation. (some benchmark modes can ignore this setting)
//    fork = 2 // How many times to forks a single benchmark. Use 0 to disable forking altogether
//    failOnError = false // Should JMH fail immediately if any benchmark had experienced the unrecoverable error?
//    forceGC = false // Should JMH force GC between iterations?
//    jvm = 'myjvm' // Custom JVM to use when forking.
//    jvmArgs = ['Custom JVM args to use when forking.']
//    jvmArgsAppend = ['Custom JVM args to use when forking (append these)']
//    jvmArgsPrepend =[ 'Custom JVM args to use when forking (prepend these)']
//    humanOutputFile = project.file("${project.buildDir}/reports/jmh/human.txt") // human-readable output file
//    resultsFile = project.file("${project.buildDir}/reports/jmh/results.txt") // results file
//    operationsPerInvocation = 10 // Operations per invocation.
//    benchmarkParameters =  [:] // Benchmark parameters.
//    profilers = [] // Use profilers to collect additional data. Supported profilers: [cl, comp, gc, stack, perf, perfnorm, perfasm, xperf, xperfasm, hs_cl, hs_comp, hs_gc, hs_rt, hs_thr, async]
//    timeOnIteration = '1s' // Time to spend at each measurement iteration.
//    resultFormat = 'CSV' // Result format type (one of CSV, JSON, NONE, SCSV, TEXT)
//    synchronizeIterations = false // Synchronize iterations?
//    threads = 4 // Number of worker threads to run with.
//    threadGroups = [2,3,4] //Override thread group distribution for asymmetric benchmarks.
//    jmhTimeout = '1s' // Timeout for benchmark iteration.
//    timeUnit = 'ms' // Output time unit. Available time units are: [m, s, ms, us, ns].
//    verbosity = 'NORMAL' // Verbosity mode. Available modes are: [SILENT, NORMAL, EXTRA]
//    warmup = '1s' // Time to spend at each warmup iteration.
//    warmupBatchSize = 10 // Warmup batch size: number of benchmark method calls per operation.
//    warmupForks = 0 // How many warmup forks to make for a single benchmark. 0 to disable warmup forks.
//    warmupIterations = 1 // Number of warmup iterations to do.
//    warmupMode = 'INDI' // Warmup mode for warming up selected benchmarks. Warmup modes are: [INDI, BULK, BULK_INDI].
//    warmupBenchmarks = ['.*Warmup'] // Warmup benchmarks to include in the run in addition to already selected. JMH will not measure these benchmarks, but only use them for the warmup.
//
//    zip64 = true // Use ZIP64 format for bigger archives
//    jmhVersion = '1.37' // Specifies JMH version
//    includeTests = true // Allows to include test sources into generate JMH jar, i.e. use it when benchmarks depend on the test classes.
//    duplicateClassesStrategy = DuplicatesStrategy.FAIL // Strategy to apply when encountring duplicate classes during creation of the fat jar (i.e. while executing jmhJar task)
}