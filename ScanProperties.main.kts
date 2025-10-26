import java.io.File
import kotlin.system.exitProcess

val projectRoot = File(".")

val packagePrefix = args.getOrNull(0) ?: run {
    println("Missing package prefix argument")
    exitProcess(1)
    "" // Unreachable, but required for type
}

val packageName = args.getOrNull(1) ?: run {
    println("Missing package name argument")
    exitProcess(1)
    ""
}

val appName = args.getOrNull(2) ?: run {
    println("Missing app name argument")
    exitProcess(1)
    ""
}

val propertiesToFind = listOf(packagePrefix, packageName, appName)

fun loadGitignorePatterns(dir: File): Set<String> {
    val patterns = mutableSetOf<String>()
    val gitignore = File(dir, ".gitignore")
    if (gitignore.exists()) {
        gitignore.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .forEach { pattern ->
                patterns.add(File(dir, pattern).relativeTo(projectRoot).path)
            }
    }
    dir.listFiles()?.filter { it.isDirectory }?.forEach {
        patterns.addAll(loadGitignorePatterns(it))
    }
    return patterns
}

val ignoredPatterns = loadGitignorePatterns(projectRoot)

fun isIgnored(file: File): Boolean {
    val relativePath = file.relativeTo(projectRoot).path
    return ignoredPatterns.any { pattern ->
        relativePath == pattern || relativePath.startsWith(pattern)
    }
}

fun scanFile(file: File) {
    file.useLines { lines ->
        lines.forEachIndexed { index, line ->
            propertiesToFind.forEach { prop ->
                if (line.contains(prop)) {
                    println("${file.relativeTo(projectRoot)}:${index + 1}: $line")
                }
            }
        }
    }
}

fun scanDirectory(dir: File) {
    dir.listFiles()?.forEach { file ->
        if (isIgnored(file)) return@forEach
        when {
            file.isDirectory -> scanDirectory(file)
            file.extension in listOf("kt", "kts") -> scanFile(file)
        }
    }
}

scanDirectory(projectRoot)
