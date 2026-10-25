package com.example.v2rang

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private lateinit var store: Store
    private var pending: Server? = null

    private val perm = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) pending?.let { s -> startVpn(s) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        setContent { App(store) { toggle(it) } }
    }

    private fun toggle(s: Server?) {
        if (VpnState.running) {
            startService(Intent(this, V2RayVpnService::class.java).setAction(V2RayVpnService.STOP))
            return
        }
        if (s == null) return
        pending = s
        val i = VpnService.prepare(this)
        if (i != null) perm.launch(i) else startVpn(s)
    }

    private fun startVpn(s: Server) {
        startService(Intent(this, V2RayVpnService::class.java).putExtra("link", s.link).putExtra("dns", store.dns))
    }
}
