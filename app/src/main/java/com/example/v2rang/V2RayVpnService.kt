package com.example.v2rang

import android.content.Intent
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor

class V2RayVpnService : VpnService() {
    private var tun: ParcelFileDescriptor? = null
    private var runner: XrayRunner? = null
    private val main = Handler(Looper.getMainLooper())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { stopVpn(); return START_NOT_STICKY }
        val link = intent?.getStringExtra("link") ?: return START_NOT_STICKY
        val dns = intent.getStringExtra("dns") ?: "1.1.1.1"
        VpnState.status = "Menyambung…"
        VpnState.error = null
        try {
            tun = Builder().setSession("V2Rang").setMtu(1500)
                .addAddress("10.10.14.1", 30).addRoute("0.0.0.0", 0).addDnsServer(dns)
                .addDisallowedApplication(packageName) // trafik inti proxy tidak boleh masuk TUN lagi
                .establish()
        } catch (e: Exception) { fail(e.message ?: "Gagal membuat TUN"); return START_NOT_STICKY }
        val fd = tun?.fd
        if (fd == null) { fail("TUN tidak terbentuk (izin VPN ditolak?)"); return START_NOT_STICKY }
        val r = XrayRunner(applicationContext).also { runner = it }
        Thread {
            val ok = r.start(link, fd)
            main.post {
                if (ok) { VpnState.running = true; VpnState.status = "Tersambung" } else fail(r.lastError)
            }
        }.start()
        return START_STICKY
    }

    private fun fail(msg: String) { stopVpn(); VpnState.error = msg }

    private fun stopVpn() {
        runner?.stop(); runner = null
        tun?.close(); tun = null
        VpnState.running = false
        VpnState.status = "Terputus"
        stopSelf()
    }

    override fun onDestroy() { stopVpn(); super.onDestroy() }

    companion object { const val STOP = "com.example.v2rang.STOP" }
}
