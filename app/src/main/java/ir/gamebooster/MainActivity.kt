package ir.gamebooster

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import ir.gamebooster.boost.Boosters
import ir.gamebooster.core.Shell
import ir.gamebooster.net.NetOptimizer
import ir.gamebooster.service.BoostService
import rikka.shizuku.Shizuku
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val REQ = 1001
    private val shizukuListener = Shizuku.OnRequestPermissionResultListener { c, r ->
        if (c == REQ && r == PackageManager.PERMISSION_GRANTED) Shell.detect()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Shizuku.addRequestPermissionResultListener(shizukuListener)
        askShizuku()
        Shell.detect()

        val svc = Intent(this, BoostService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(svc) else startService(svc)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val status = TextView(this).apply {
            text = "دسترسی: " + when(Shell.mode) {
                Shell.Mode.ROOT -> "ROOT"
                Shell.Mode.SHIZUKU -> "SHIZUKU"
                else -> "هیچکدام"
            }
            textSize = 18f
        }

        val log = TextView(this).apply {
            text = "آماده"
            textSize = 12f
        }

        val btnBoost = Button(this).apply { text = "شروع بوست کامل" }
        val btnNet = Button(this).apply { text = "بهینه سازی نت" }
        val btnRestore = Button(this).apply { text = "بازگشت" }

        btnBoost.setOnClickListener {
            thread {
                val logs = mutableListOf<String>()
                logs += Boosters.cpuBoost()
                logs += Boosters.gpuBoost()
                logs += Boosters.memoryBoost()
                val fg = NetOptimizer.foregroundPkg()
                logs += Boosters.processBoost(fg)
                runOnUiThread { log.text = logs.joinToString("\n") }
            }
        }

        btnNet.setOnClickListener {
            thread {
                val logs = mutableListOf<String>()
                logs += NetOptimizer.fastDns()
                logs += NetOptimizer.tuneTcp()
                logs += NetOptimizer.wifiOptimize()
                val p = NetOptimizer.ping()
                logs += "پینگ: " + (if (p > 0) p.toString() + " ms" else "ناموفق")
                runOnUiThread { log.text = logs.joinToString("\n") }
            }
        }

        btnRestore.setOnClickListener {
            thread { Boosters.restore(); runOnUiThread { log.text = "بازگشت انجام شد" } }
        }

        layout.addView(status)
        layout.addView(btnBoost)
        layout.addView(btnNet)
        layout.addView(btnRestore)
        layout.addView(ScrollView(this).apply { addView(log) })

        setContentView(layout)
    }

    private fun askShizuku() {
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED)
                Shizuku.requestPermission(REQ)
        } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(shizukuListener)
        super.onDestroy()
    }
}
