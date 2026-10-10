package net.ccbluex.liquidbounce.utils

import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.CrossSine
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

object ClientUpdater {

    private const val VERSION_URL = "https://crosssine.github.io/version.json"
    private const val DOWNLOADS_PAGE = "https://crosssine.github.io/downloads.html"
    private const val FILE_BASE = "https://crosssine.github.io/file/"
    private const val AGENT = "CrossSine-Updater/${CrossSine.CLIENT_VERSION}"

    enum class Status {
        IDLE,
        CHECKING,
        UP_TO_DATE,
        AVAILABLE,
        DOWNLOADING,
        READY_RESTART,
        FAILED
    }

    @Volatile var status: Status = Status.IDLE
        private set
    @Volatile var latestVersion: String = ""
        private set
    @Volatile var changelog: String = ""
        private set
    @Volatile var downloadUrl: String = ""
        private set
    @Volatile var htmlUrl: String = DOWNLOADS_PAGE
        private set
    @Volatile var message: String = ""
        private set
    /** 0f..1f while downloading */
    @Volatile var progress: Float = 0f
        private set

    /** Kept for GuiMainMenu label compatibility. */
    val latestTag: String
        get() = latestVersion

    private val busy = AtomicBoolean(false)
    private var downloadedFile: File? = null
    private var applyScript: File? = null

    val updateAvailable: Boolean
        get() = status == Status.AVAILABLE || status == Status.READY_RESTART

    fun checkAsync(force: Boolean = false) {
        if (!force && (status == Status.CHECKING || status == Status.DOWNLOADING || status == Status.AVAILABLE || status == Status.READY_RESTART)) {
            return
        }
        if (!busy.compareAndSet(false, true)) return
        status = Status.CHECKING
        message = "Checking for updates…"
        thread(name = "CrossSine-UpdateCheck", isDaemon = true) {
            try {
                checkLatest()
            } catch (t: Throwable) {
                ClientUtils.logError("Update check failed", t)
                status = Status.FAILED
                message = t.message ?: "Update check failed"
            } finally {
                busy.set(false)
            }
        }
    }

    fun startDownloadAsync() {
        if (status != Status.AVAILABLE && status != Status.FAILED) return
        if (downloadUrl.isEmpty()) {
            status = Status.FAILED
            message = "No download URL"
            return
        }
        if (!busy.compareAndSet(false, true)) return
        status = Status.DOWNLOADING
        progress = 0f
        message = "Downloading ${latestVersion}…"
        thread(name = "CrossSine-UpdateDownload", isDaemon = true) {
            try {
                downloadAndPrepare()
            } catch (t: Throwable) {
                ClientUtils.logError("Update download failed", t)
                status = Status.FAILED
                message = t.message ?: "Download failed"
                progress = 0f
            } finally {
                busy.set(false)
            }
        }
    }

    /**
     * Applies the downloaded jar via an external script and closes Minecraft.
     */
    fun applyAndRestart() {
        val script = applyScript
        if (script == null || !script.exists()) {
            status = Status.FAILED
            message = "Update script missing — re-download"
            return
        }
        try {
            val cmd = if (isWindows()) {
                arrayOf("cmd", "/c", "start", "\"CrossSine Updater\"", script.absolutePath)
            } else {
                arrayOf("sh", script.absolutePath)
            }
            Runtime.getRuntime().exec(cmd)
            ClientUtils.logInfo("Update script launched — shutting down")
            thread(name = "CrossSine-UpdateExit", isDaemon = true) {
                Thread.sleep(400)
                mc.shutdown()
            }
        } catch (t: Throwable) {
            ClientUtils.logError("Failed to launch updater script", t)
            status = Status.FAILED
            message = "Could not launch updater"
        }
    }

    private fun checkLatest() {
        val conn = URL(VERSION_URL).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 10000
        conn.setRequestProperty("User-Agent", AGENT)
        conn.setRequestProperty("Accept", "application/json")
        conn.instanceFollowRedirects = true
        conn.useCaches = false

        val code = conn.responseCode
        if (code != 200) {
            throw IllegalStateException("version.json HTTP $code — upload version.json to the website")
        }

        val json = JsonParser().parse(conn.inputStream.reader()).asJsonObject
        val remote = json.get("version")?.asString?.trim().orEmpty()
        if (remote.isEmpty()) throw IllegalStateException("version.json missing \"version\"")

        latestVersion = remote
        changelog = json.get("changelog")?.asString?.trim().orEmpty()
        htmlUrl = json.get("page")?.asString?.takeIf { it.isNotBlank() } ?: DOWNLOADS_PAGE

        var jar = json.get("download")?.asString?.trim().orEmpty()
        if (jar.isEmpty()) {
            jar = FILE_BASE + "CrossSine-$remote.jar"
        }
        downloadUrl = jar

        val cmp = compareSemver(remote, CrossSine.CLIENT_VERSION)
        if (cmp > 0) {
            status = Status.AVAILABLE
            message = "Update available: $remote"
            ClientUtils.logInfo("Update available: $remote (local ${CrossSine.CLIENT_VERSION})")
        } else {
            status = Status.UP_TO_DATE
            message = "Up to date (${CrossSine.CLIENT_VERSION})"
            ClientUtils.logInfo("Client is up to date (${CrossSine.CLIENT_VERSION}, remote $remote)")
        }
    }

    private fun downloadAndPrepare() {
        val current = resolveCurrentJar()
        val modsDir = current?.parentFile ?: File(mc.mcDataDir, "mods")
        if (!modsDir.exists()) modsDir.mkdirs()

        val safeName = latestVersion.replace(Regex("[^0-9A-Za-z._-]"), "_")
        val dest = File(modsDir, "CrossSine-$safeName-update.jar")
        if (dest.exists()) dest.delete()

        downloadWithProgress(downloadUrl, dest)
        downloadedFile = dest

        val script = writeApplyScript(modsDir, current, dest, safeName)
        applyScript = script

        status = Status.READY_RESTART
        progress = 1f
        message = "Downloaded $latestVersion — click Restart"
        ClientUtils.logInfo("Update ready at ${dest.absolutePath}")
    }

    private fun downloadWithProgress(url: String, file: File) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 180000
        conn.setRequestProperty("User-Agent", AGENT)
        conn.instanceFollowRedirects = true

        val total = conn.contentLengthLong.coerceAtLeast(0L)
        conn.inputStream.use { input ->
            FileOutputStream(file).use { output ->
                val buf = ByteArray(64 * 1024)
                var readTotal = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    output.write(buf, 0, n)
                    readTotal += n
                    progress = if (total > 0L) (readTotal.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                    message = if (total > 0L) {
                        "Downloading… ${(progress * 100).toInt()}%"
                    } else {
                        "Downloading… ${readTotal / (1024 * 1024)} MB"
                    }
                }
            }
        }
        if (file.length() < 1024L) {
            file.delete()
            throw IllegalStateException("Downloaded file too small")
        }
    }

    private fun writeApplyScript(modsDir: File, currentJar: File?, newJar: File, version: String): File {
        val finalName = "CrossSine-$version.jar"
        val finalFile = File(modsDir, finalName)

        return if (isWindows()) {
            val bat = File(modsDir, "apply-crosssine-update.bat")
            val oldPath = currentJar?.absolutePath?.replace("/", "\\") ?: ""
            val newPath = newJar.absolutePath.replace("/", "\\")
            val finalPath = finalFile.absolutePath.replace("/", "\\")
            val modsPath = modsDir.absolutePath.replace("/", "\\")
            bat.writeText(
                """
                @echo off
                setlocal
                echo CrossSine updater — waiting for game to close...
                ping 127.0.0.1 -n 4 >nul
                if not "$oldPath"=="" if exist "$oldPath" del /f /q "$oldPath"
                for %%F in ("$modsPath\CrossSine*.jar") do (
                  if /I not "%%~nxF"=="${newJar.name}" if /I not "%%~nxF"=="$finalName" del /f /q "%%F"
                )
                if exist "$finalPath" del /f /q "$finalPath"
                move /y "$newPath" "$finalPath" >nul
                del /f /q "%~f0"
                echo Update applied: $finalName
                endlocal
                """.trimIndent()
            )
            bat
        } else {
            val sh = File(modsDir, "apply-crosssine-update.sh")
            val oldPath = currentJar?.absolutePath ?: ""
            sh.writeText(
                """
                #!/bin/sh
                echo "CrossSine updater — waiting..."
                sleep 3
                [ -n "$oldPath" ] && rm -f "$oldPath"
                mv -f "${newJar.absolutePath}" "${finalFile.absolutePath}"
                rm -f "${sh.absolutePath}"
                echo "Update applied: $finalName"
                """.trimIndent()
            )
            sh.setExecutable(true)
            sh
        }
    }

    private fun resolveCurrentJar(): File? {
        return try {
            val loc = CrossSine::class.java.protectionDomain?.codeSource?.location ?: return null
            val file = File(loc.toURI())
            if (file.isFile && file.name.endsWith(".jar", true)) file else null
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Compare semver-like strings (e.g. 26.0.2). Returns >0 if [a] newer than [b].
     */
    fun compareSemver(a: String, b: String): Int {
        val pa = a.trim().removePrefix("v").removePrefix("V").split('.', '-', '_')
        val pb = b.trim().removePrefix("v").removePrefix("V").split('.', '-', '_')
        val n = maxOf(pa.size, pb.size)
        for (i in 0 until n) {
            val ai = pa.getOrNull(i)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
            val bi = pb.getOrNull(i)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
            if (ai != bi) return ai - bi
        }
        return 0
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name", "").lowercase().contains("win")
}
