package ir.gamebooster.boost

import ir.gamebooster.core.Shell

object Boosters {

    fun cpuBoost(): List<String> {
        val log = mutableListOf<String>()
        val cores = Runtime.getRuntime().availableProcessors()
        var ok = 0
        for (i in 0 until cores) {
            if (Shell.write("/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_governor", "performance")) ok++
        }
        log += "CPU performance (" + ok + "/" + cores + ")"
        if (Shell.hasRoot()) {
            for (i in 0 until cores) {
                val max = Shell.read("/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq")
                if (max.isNotBlank()) Shell.write("/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_min_freq", max)
            }
            log += "فرکانس CPU قفل شد"
        }
        return log
    }

    fun gpuBoost(): List<String> {
        val log = mutableListOf<String>()
        val paths = listOf(
            "/sys/class/kgsl/kgsl-3d0/devfreq/governor",
            "/sys/class/kgsl/kgsl-3d0/pwrscale/trustzone/governor"
        )
        for (p in paths) if (Shell.exists(p)) Shell.write(p, "performance")
        if (Shell.exists("/sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost"))
            Shell.write("/sys/class/kgsl/kgsl-3d0/devfreq/adrenoboost", "3")
        Shell.run("setprop debug.sf.hw 1")
        Shell.run("setprop debug.egl.hw 1")
        Shell.run("setprop debug.composition.type gpu")
        log += "GPU بهینه شد"
        return log
    }

    fun memoryBoost(): List<String> {
        val log = mutableListOf<String>()
        Shell.run("pm trim-caches 999G")
        log += "کش پاک شد"
        if (Shell.hasRoot()) {
            Shell.run("sync; echo 3 > /proc/sys/vm/drop_caches")
            Shell.write("/proc/sys/vm/swappiness", "10")
            log += "drop_caches"
        }
        val out = Shell.run("pm list packages -3")
        var n = 0
        out.lines().map { it.removePrefix("package:").trim() }
            .filter { it.isNotBlank() && it != "ir.gamebooster" }
            .forEach { Shell.run("am force-stop " + it); n++ }
        log += n.toString() + " اپ بسته شد"
        return log
    }

    fun processBoost(gamePkg: String?): List<String> {
        val log = mutableListOf<String>()
        if (gamePkg == null) return listOf("بازی مشخص نشد")
        val pid = Shell.run("pidof " + gamePkg).trim()
        if (pid.isNotBlank()) {
            Shell.run("renice -n -20 -p " + pid)
            if (Shell.hasRoot()) {
                Shell.write("/proc/" + pid + "/oom_score_adj", "-1000")
                Shell.write("/dev/cpuset/top-app/tasks", pid)
                log += "بازی priority حداکثر"
            }
        }
        Shell.run("cmd game mode 2 " + gamePkg)
        log += "Game Mode فعال"
        return log
    }

    fun restore() {
        val cores = Runtime.getRuntime().availableProcessors()
        for (i in 0 until cores) {
            val min = Shell.read("/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_min_freq")
            if (min.isNotBlank()) Shell.write("/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_min_freq", min)
            Shell.write("/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_governor", "schedutil")
        }
    }
}
