package com.example.v2rang

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun App(store: Store, onToggle: (Server?) -> Unit) {
    CompositionLocalProvider(LocalClay provides if (isSystemInDarkTheme()) darkClay else lightClay) {
        val c = LocalClay.current
        val servers = remember { mutableStateListOf<Server>().apply { addAll(store.load()) } }
        var sel by remember { mutableIntStateOf(0) }
        var tab by remember { mutableIntStateOf(0) }
        val cur = servers.getOrNull(sel)
        Column(Modifier.fillMaxSize().background(c.bg).padding(20.dp)) {
            Text("V2Rang", color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> Home(cur) { onToggle(cur) }
                    1 -> Servers(servers, sel, { sel = it }, { i ->
                        servers.removeAt(i)
                        if (sel >= servers.size) sel = maxOf(0, servers.size - 1)
                        store.save(servers)
                    }, { t ->
                        var n = 0
                        t.split(Regex("\\s+")).forEach { Parser.parse(it)?.let { s -> servers.add(s); n++ } }
                        if (n > 0) store.save(servers)
                        n
                    })
                    else -> Settings(store)
                }
            }
            BottomBar(tab) { tab = it }
        }
    }
}

@Composable
fun Home(server: Server?, onToggle: () -> Unit) {
    val c = LocalClay.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(210.dp).clay(105.dp, 12.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(150.dp).clay(75.dp, 8.dp, if (VpnState.running) c.mint else if (VpnState.status.startsWith("Meny")) c.tint else c.card).clip(CircleShape).clickable(onClick = onToggle), contentAlignment = Alignment.Center) {
                val ic = if (VpnState.running) Color.White else c.accent
                Box(Modifier.size(56.dp).drawBehind {
                    val sw = 6.dp.toPx(); val s = size.minDimension
                    drawArc(ic, -60f, 300f, false, Offset(sw, sw), Size(s - 2 * sw, s - 2 * sw), style = Stroke(sw, cap = StrokeCap.Round))
                    drawLine(ic, Offset(s / 2, sw / 2), Offset(s / 2, s * 0.45f), strokeWidth = sw, cap = StrokeCap.Round)
                })
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(VpnState.status, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        VpnState.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = c.rose, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(28.dp))
        Column(Modifier.fillMaxWidth().padding(12.dp).clay().padding(18.dp)) {
            Text("SERVER AKTIF", color = c.muted, fontSize = 12.sp)
            Text(server?.name ?: "Belum ada server", color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun Servers(list: List<Server>, sel: Int, onSel: (Int) -> Unit, onDel: (Int) -> Unit, onAdd: (String) -> Int) {
    val c = LocalClay.current
    var text by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    val clip = LocalClipboardManager.current
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (list.isEmpty()) item { Text("Belum ada server. Tempel tautan konfigurasi di bawah.", color = c.muted, fontSize = 13.sp) }
        itemsIndexed(list) { i, s ->
            Row(
                Modifier.fillMaxWidth().let { if (i == sel) it.clay(color = c.tint) else it.clay() }.clickable { onSel(i) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(s.name, color = c.text, fontWeight = FontWeight.SemiBold)
                    Text("${s.proto.uppercase()} · ${s.host}", color = c.muted, fontSize = 12.sp)
                }
                Text("✕", color = c.muted, modifier = Modifier.clickable { onDel(i) }.padding(8.dp))
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clay().padding(16.dp)) {
                Text("IMPOR DARI TAUTAN", color = c.muted, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().dent(14.dp).padding(12.dp)) {
                    BasicTextField(
                        text, { text = it },
                        textStyle = TextStyle(color = c.text, fontSize = 14.sp),
                        cursorBrush = SolidColor(c.accent),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Btn("Tambah") {
                        val n = onAdd(text)
                        msg = if (n > 0) "$n server ditambahkan." else "Tautan tidak dikenali."
                        if (n > 0) text = ""
                    }
                    Btn("Tempel") { text = clip.getText()?.text ?: "" }
                }
                if (msg.isNotEmpty()) { Spacer(Modifier.height(10.dp)); Text(msg, color = c.muted, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
fun Btn(label: String, onClick: () -> Unit) {
    val c = LocalClay.current
    Text(
        label, color = c.accent, fontWeight = FontWeight.Bold,
        modifier = Modifier.clay(14.dp, 5.dp).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp)
    )
}

@Composable
fun Settings(store: Store) {
    val c = LocalClay.current
    var dns by remember { mutableStateOf(store.dns) }
    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        Text("DNS JARAK JAUH", color = c.muted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().dent(14.dp).padding(14.dp)) {
            BasicTextField(
                dns, { dns = it; store.dns = it },
                textStyle = TextStyle(color = c.text, fontSize = 14.sp),
                cursorBrush = SolidColor(c.accent), singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Mode: VPN untuk semua aplikasi (kecuali V2Rang sendiri).", color = c.muted, fontSize = 12.sp)
    }
}

@Composable
fun BottomBar(tab: Int, onTab: (Int) -> Unit) {
    val c = LocalClay.current
    val items = listOf("● Beranda", "☰ Server", "⚙ Atur")
    Row(Modifier.fillMaxWidth().padding(top = 12.dp).clay(24.dp, 8.dp).padding(8.dp)) {
        items.forEachIndexed { i, t ->
            Box(
                Modifier.weight(1f).let { if (i == tab) it.clay(18.dp, 4.dp, c.tint) else it }.clickable { onTab(i) }.padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) { Text(t, color = if (i == tab) c.accent else c.muted, fontSize = 13.sp) }
        }
    }
}
