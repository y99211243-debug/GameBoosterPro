package ir.gamebooster.net

import ir.gamebooster.core.Shell

object NetOptimizer {

    fun fastDns(): List<String> {
        val log = mutableListOf<String>()
        Shell.run("settings put global private_dns_mode hostname")
        Shell.run("settings put global private_dns_specifier 1.1.1.1")
        log += "DNS: Cloudflare 1.1.1.1"
        if (Shell.hasRoot()) {
            Shell.run("setprop net.dns1 1.1.1.1")
            Shell.run("setprop net.dns2 1.0.0.1")
        }
        return log
    }

    fun tuneTcp(): List<String> {
        val log = mutableListOf<String>()
        if (!Shell.hasRoot()) return listOf("TCP فقط با روت")
        val avail = Shell.read("/proc/sys/net/ipv4/tcp_available_congestion_control")
        if (avail.contains("bbr")) {
            Shell.write("/proc/sys/net/ipv4/tcp_congestion_control", "bbr")
            log += "BBR فعال"
        }
        Shell.write("/proc/sys/net/core/rmem_max", "25165824")
        Shell.write("/proc/sys/net/core/wmem_max", "25165824")
        Shell.write("/proc/sys/net/ipv4/tcp_fastopen", "3")
        Shell.write("/proc/sys/net/ipv4/tcp_low_latency", "1")
        Shell.write("/proc/sys/net/ipv4/tcp_slow_start_after_idle", "0")
        Shell.write("/proc/sys/net/ipv4/tcp_tw_reuse", "1")
        log += "TCP low-latency"
        return log
    }

    fun wifiOptimize(): List<String> {
        val log = mutableListOf<String>()
        Shell.run("settings put global wifi_scan_throttle_enabled 0")
        if (Shell.hasRoot()) {
            Shell.run("iw dev wlan0 set power_save off 2>/dev/null")
        }
        log += "WiFi بهینه شد"
        return log
    }

    fun ping(host: String = "1.1.1.1"): Float {
        val out = Shell.run("ping -c 3 -W 2 " + host)
        return Regex("avg[^0-9]*([0-9.]+)").find(out)?.groupValues?.get(1)?.toFloatOrNull() ?: -1f
    }

    fun foregroundPkg(): String? {
        val out = Shell.run("dumpsys activity activities | grep -E 'ResumedActivity'")
        return Regex("([a-zA-Z0-9_.]+)/").find(out)?.groupValues?.get(1)
    }
}
