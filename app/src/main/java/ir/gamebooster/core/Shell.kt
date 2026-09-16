package ir.gamebooster.core

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object Shell {
    enum class Mode { ROOT, SHIZUKU, NONE }
    @Volatile var mode: Mode = Mode.NONE
        private set

    fun detect(): Mode {
        mode = when {
            tryRoot() -> Mode.ROOT
            tryShizuku() -> Mode.SHIZUKU
            else -> Mode.NONE
        }
        return mode
    }

    private fun tryRoot(): Boolean = try {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        out.contains("uid=0")
    } catch (_: Throwable) { false }

    private fun tryShizuku(): Boolean = try {
        Shizuku.pingBinder() &&
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) { false }

    fun run(cmd: String): String {
        return try {
            val proc = when (mode) {
                Mode.ROOT -> Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                Mode.SHIZUKU -> Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
                Mode.NONE -> return ""
            }
            val out = proc.inputStream.bufferedReader().readText()
            val err = proc.errorStream.bufferedReader().readText()
            proc.waitFor()
            if (out.isNotBlank()) out else err
        } catch (e: Throwable) { "" }
    }

    fun read(path: String): String = run("cat $path").trim()

    fun write(path: String, value: String): Boolean {
        val r = run("echo '" + value + "' > " + path + " 2>&1")
        return !r.contains("denied")
    }

    fun exists(path: String): Boolean = run("[ -e " + path + " ] && echo 1 || echo 0").trim() == "1"
    fun hasRoot() = mode == Mode.ROOT
    fun hasShizuku() = mode == Mode.SHIZUKU
    fun hasAny() = mode != Mode.NONE
}
