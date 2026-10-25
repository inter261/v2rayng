package com.example.v2rang

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import go.Seq
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import java.io.File
import java.util.UUID

object VpnState {
    var status by mutableStateOf("Terputus")
    var running by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
}

/** Menjalankan Xray-core (libv2ray.aar) dengan file descriptor TUN dari VpnService. */
class XrayRunner(private val ctx: Context) {
    var lastError = ""
        private set
    private var controller: CoreController? = null

    fun start(link: String, tunFd: Int): Boolean = try {
        init(ctx)
        val cfg = ConfigBuilder.build(link)
        val cc = Libv2ray.newCoreController(object : CoreCallbackHandler {
            override fun startup(): Long = 0L
            override fun shutdown(): Long = 0L
            override fun onEmitStatus(code: Long, msg: String?): Long = 0L
        })
        cc.startLoop(cfg, tunFd)
        controller = cc
        if (cc.isRunning) true else { lastError = "Inti gagal berjalan. Periksa konfigurasi server."; false }
    } catch (e: Throwable) {
        lastError = e.message ?: e.toString()
        false
    }

    fun stop() {
        try { controller?.stopLoop() } catch (_: Throwable) {}
        controller = null
    }

    companion object {
        @Volatile private var inited = false

        @Synchronized
        fun init(ctx: Context) {
            if (inited) return
            val app = ctx.applicationContext
            Seq.setContext(app)
            val dir = File(app.filesDir, "assets").apply { mkdirs() }
            listOf("geoip.dat", "geosite.dat").forEach { n ->
                val f = File(dir, n)
                if (!f.exists()) app.assets.open(n).use { i -> f.outputStream().use { o -> i.copyTo(o) } }
            }
            val sp = app.getSharedPreferences("v2rang", Context.MODE_PRIVATE)
            val id = sp.getString("device_id", null)
                ?: UUID.randomUUID().toString().also { sp.edit().putString("device_id", it).apply() }
            Libv2ray.initCoreEnv(dir.absolutePath, id)
            inited = true
        }
    }
}
