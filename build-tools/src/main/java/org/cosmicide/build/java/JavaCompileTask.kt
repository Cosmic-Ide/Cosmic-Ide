package org.cosmicide.build.java

import com.sun.tools.javac.api.JavacTool
import org.cosmicide.build.BuildReporter
import org.cosmicide.build.Task
import org.cosmicide.project.Project
import org.cosmicide.rewrite.util.FileUtil
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.Locale
import javax.tools.Diagnostic.Kind.ERROR
import javax.tools.Diagnostic.Kind.MANDATORY_WARNING
import javax.tools.Diagnostic.Kind.NOTE
import javax.tools.Diagnostic.Kind.OTHER
import javax.tools.Diagnostic.Kind.WARNING
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.StandardLocation

class JavaCompileTask(val project: Project) : Task {

    val tool by lazy {
        JavacTool.create()
    }

    override fun execute(reporter: BuildReporter) {
        val output = File(project.binDir, "classes")
        val version = "8"
        reporter.reportInfo("Compiling on Java version: $version")

        val diagnostics = DiagnosticCollector<JavaFileObject>()

        try {
            Files.createDirectories(output.toPath())
        } catch (e: Exception) {
            throw RuntimeException("Failed to create output directory", e)
        }

        val javaFiles = getSourceFiles(project.srcDir.invoke())
        val javaFileObjects = javaFiles.map { file ->
            object : SimpleJavaFileObject(file.toURI(), JavaFileObject.Kind.SOURCE) {
                override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence {
                    return file.readText()
                }
            }
        }

        if (javaFileObjects.isEmpty()) {
            return
        }

        val fileManager = tool.getStandardFileManager(diagnostics, Locale.getDefault(), Charset.defaultCharset())

        fileManager.use { fm ->
            fm.setLocation(StandardLocation.CLASS_OUTPUT, listOf(output))
            fm.setLocation(StandardLocation.PLATFORM_CLASS_PATH, getSystemClasspath())
            fm.setLocation(StandardLocation.CLASS_PATH, getClasspath(project))
            fm.setLocation(StandardLocation.SOURCE_PATH, javaFiles)

            val options = listOf(
                "-proc:none",
                "-source",
                version,
                "-target",
                version
            )

            val task = tool.getTask(
                object : Writer() {
                    val sb = StringBuilder()
                    override fun close() = flush()
                    override fun flush() {
                        reporter.reportInfo(sb.toString())
                        sb.clear()
                    }
                    override fun write(cbuf: CharArray?, off: Int, len: Int) {
                        sb.appendRange(cbuf!!, off, off + len)
                        reporter.reportInfo(sb.toString())
                    }

                },
                fm,
                diagnostics,
                options,
                null,
                javaFileObjects
            )

            task.call()

            for (diagnostic in diagnostics.diagnostics) {
                val message = StringBuilder()

                diagnostic.source?.apply {
                    message.append("$name:${diagnostic.lineNumber}: ")
                }

                // We ourselves add the names of the kinds. [INFO, ERROR, WARNING]
                // message.append("${diagnostic.kind.name}: ${diagnostic.getMessage(Locale.getDefault())}")
                message.append(diagnostic.getMessage(Locale.getDefault()))

                when (diagnostic.kind) {
                    ERROR, OTHER -> reporter.reportError(message.toString())
                    NOTE, WARNING, MANDATORY_WARNING -> reporter.reportWarning(message.toString())
                    else -> reporter.reportInfo(message.toString())
                }
            }
        }
    }

    fun getSourceFiles(directory: File): List<File> {
        return directory.listFiles()?.filter {
            it.isFile && it.extension == "java"
        } ?: emptyList()
    }

    fun getClasspath(project: Project): List<File> {
        val classpath = mutableListOf(File(project.binDir, "classes"))
        val libDir = project.libDir
        if (libDir.exists() && libDir.isDirectory()) {
            classpath += libDir.listFiles()?.toList() ?: emptyList()
        }
        return classpath
    }

    fun getSystemClasspath(): List<File> {
        return FileUtil.classpathDir.listFiles()?.toList() ?: emptyList()
    }
}