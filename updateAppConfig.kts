#!/usr/bin/env kotlin
import java.io.File
import kotlin.system.exitProcess

if (args.size < 3) {
    println("Usage: updateAppConfig.kts <packagePrefix> <packageName> <appName>")
    exitProcess(1)
}
val newPackagePrefix = args[0]
val newPackageName = args[1]
val newAppName = args[2]

val projectRoot = File(".").absoluteFile
val gradlePropertiesFile = File(projectRoot, "gradle.properties")
val stringsFile = File(projectRoot, "composeApp/src/main/res/values/strings.xml")

fun log(s: String) = println("[INFO] $s")
fun warn(s: String) = println("[WARN] $s")

// --- 1. Determine old full package ---
fun findFirstPackage(root: File): String? {
    val exts = setOf("kt","kts","java")
    root.walkTopDown()
        .filter { it.isFile && it.extension.lowercase() in exts }
        .forEach { f ->
            val text = f.readText()
            val regex = Regex("^\\s*package\\s+([A-Za-z0-9_.]+)", RegexOption.MULTILINE)
            regex.find(text)?.let { return it.groupValues[1] }
        }
    return null
}

val oldFullPackage = findFirstPackage(projectRoot) ?: run {
    warn("Could not detect old package automatically; replacements may fail")
    ""
}
val oldPackageParts = oldFullPackage.split(".")
val oldPackagePrefix = if (oldPackageParts.size > 1) oldPackageParts.dropLast(1).joinToString(".") else ""
val oldPackageName = oldPackageParts.lastOrNull() ?: ""

// New full package
val newFullPackage = if (newPackagePrefix.isNotEmpty()) "$newPackagePrefix.$newPackageName" else newPackageName

log("Old full package: '$oldFullPackage'")
log("New full package: '$newFullPackage'")

// --- 2. Update gradle.properties safely ---
val lines = if (gradlePropertiesFile.exists()) gradlePropertiesFile.readLines().toMutableList() else mutableListOf()
fun upsert(key: String, value: String) {
    val idx = lines.indexOfFirst { it.startsWith("$key=") }
    val line = "$key=$value"
    if (idx >= 0) lines[idx] = line else lines.add(line)
}
upsert("appPackagePrefix", newPackagePrefix)
upsert("appPackageName", newPackageName)
upsert("appName", newAppName)
gradlePropertiesFile.writeText(lines.joinToString("\n"))
log("Updated gradle.properties")

// --- 3. Update strings.xml safely ---
if (stringsFile.exists()) {
    val xmlText = stringsFile.readText()
    val pattern = Regex("(?s)<string\\s+name\\s*=\\s*\"app_name\"\\s*>.*?</string>")
    val replacement = "<string name=\"app_name\">${Regex.escapeReplacement(newAppName)}</string>"
    val newXml = pattern.replace(xmlText, replacement)
    stringsFile.writeText(newXml)
    log("Updated strings.xml")
}

// --- 4. Build safe replacements map ---
val replacements = linkedMapOf<String,String>()
if (oldFullPackage.isNotBlank()) replacements[oldFullPackage] = newFullPackage
if (oldPackagePrefix.isNotBlank()) replacements[oldPackagePrefix] = newPackagePrefix
if (oldPackageName.isNotBlank()) replacements[oldPackageName] = newPackageName

// --- 5. Scan source files and replace with guardrails ---
val allowedExt = setOf("kt","kts","java","xml","gradle","properties","mf","json","md","txt","yaml","yml","pro")
val excludedDirs = setOf("build",".gradle",".git","node_modules","out")
fun isExcluded(f: File): Boolean {
    var cur: File? = f
    while (cur != null && cur.absolutePath.startsWith(projectRoot.absolutePath)) {
        if (excludedDirs.contains(cur.name)) return true
        cur = cur.parentFile
    }
    return false
}

fun replaceInFile(file: File) {
    val text = file.readText()
    var newText = text

    // 1. Replace full package everywhere (safe)
    if (oldFullPackage.isNotBlank()) {
        newText = newText.replace(oldFullPackage, newFullPackage)
    }

    // 2. Replace package prefix only in package/import lines
    if (oldPackagePrefix.isNotBlank()) {
        val regex = Regex("(?m)^(\\s*(package|import)\\s+)${Regex.escape(oldPackagePrefix)}")
        newText = regex.replace(newText) { it.groupValues[1] + newPackagePrefix }
    }

    // 3. Replace package name only in package/import lines with word boundary
    if (oldPackageName.isNotBlank()) {
        val regex = Regex("(?m)^(\\s*(package|import)\\s+.*\\.)${Regex.escape(oldPackageName)}\\b")
        newText = regex.replace(newText) { it.groupValues[1] + newPackageName }
    }

    // 4. Replace appName only in Gradle/manifest/XML where safe
    if (file.extension in setOf("xml","gradle","properties","mf")) {
        newText = newText.replace(Regex("\\b${Regex.escape(oldPackageName)}\\b"), newPackageName)
        newText = newText.replace(Regex("\\b${Regex.escape(oldFullPackage)}\\b"), newFullPackage)
    }

    if (newText != text) file.writeText(newText)
}

fun scanAndReplace(root: File) {
    root.walkTopDown()
        .filter { it.isFile && allowedExt.contains(it.extension.lowercase()) && !isExcluded(it) }
        .forEach { replaceInFile(it) }
}

log("Replacing package/app references safely...")
scanAndReplace(projectRoot)
log("Text replacements complete.")

// --- 6. Move package directories safely ---
fun movePackageDirs(srcDir: File, oldFull: String, newFull: String) {
    if (oldFull.isBlank()) return
    val oldParts = oldFull.split(".").filter { it.isNotBlank() }
    val newParts = newFull.split(".").filter { it.isNotBlank() }

    val candidateDirs = srcDir.walkTopDown()
        .filter { it.isDirectory && it.relativeTo(srcDir).invariantSeparatorsPath.endsWith(oldParts.joinToString("/")) }
        .toList()

    candidateDirs.forEach { oldDir ->
        val newDir = File(srcDir, newParts.joinToString(File.separator))
        if (!newDir.exists()) newDir.mkdirs()
        oldDir.walkTopDown().forEach { f ->
            if (f.isFile) {
                val rel = f.relativeTo(oldDir).path
                val targetFile = File(newDir, rel)
                targetFile.parentFile.mkdirs()
                f.renameTo(targetFile)
            }
        }
        // Delete old dirs recursively if empty
        var cur: File? = oldDir
        while (cur != null && cur.exists() && cur.listFiles()?.isEmpty() == true) {
            val parent = cur.parentFile
            cur.delete()
            cur = parent
        }
        log("Moved package dir: ${oldDir.path} -> ${newDir.path}")
    }
}

val kotlinRoot = File(projectRoot, "composeApp/src")
kotlinRoot.listFiles()?.filter { it.isDirectory }?.forEach { sourceSetDir ->
    val kotlinDir = File(sourceSetDir, "kotlin")
    if (kotlinDir.exists()) movePackageDirs(kotlinDir, oldFullPackage, newFullPackage)
}

log("Refactoring completed successfully.")
