package com.example.v2rang

import android.net.Uri
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/** Mengubah tautan vmess/vless/trojan/ss menjadi konfigurasi Xray lengkap dengan inbound TUN. */
object ConfigBuilder {
    fun build(link: String): String {
        val out = outbound(link.trim())
        val sniff = JSONObject().put("enabled", true).put("destOverride", JSONArray(listOf("http", "tls", "quic")))
        val tun = JSONObject().put("tag", "tun").put("protocol", "tun")
            .put("settings", JSONObject().put("name", "xray0").put("MTU", 1500).put("userLevel", 8))
            .put("sniffing", sniff)
        val rules = JSONArray().put(
            JSONObject().put("type", "field").put("ip", JSONArray(listOf("geoip:private"))).put("outboundTag", "direct")
        )
        return JSONObject()
            .put("log", JSONObject().put("loglevel", "warning"))
            .put("inbounds", JSONArray().put(tun))
            .put("outbounds", JSONArray().put(out)
                .put(JSONObject().put("tag", "direct").put("protocol", "freedom"))
                .put(JSONObject().put("tag", "block").put("protocol", "blackhole")))
            .put("routing", JSONObject().put("domainStrategy", "AsIs").put("rules", rules))
            .toString()
    }

    private fun b64(s: String): String {
        var t = s.trim().replace('-', '+').replace('_', '/')
        t += "=".repeat((4 - t.length % 4) % 4)
        return String(Base64.decode(t, Base64.DEFAULT))
    }

    private fun outbound(link: String): JSONObject = when (val scheme = link.substringBefore("://").lowercase()) {
        "vmess" -> vmess(link.substringAfter("://"))
        "vless", "trojan" -> uriBased(scheme, Uri.parse(link))
        "ss" -> shadowsocks(link)
        else -> throw IllegalArgumentException("Protokol tidak didukung: $scheme")
    }

    private fun wrap(proto: String, settings: JSONObject, stream: JSONObject?) =
        JSONObject().put("tag", "proxy").put("protocol", proto).put("settings", settings).apply {
            if (stream != null) put("streamSettings", stream)
        }

    private fun vmess(b: String): JSONObject {
        val j = JSONObject(b64(b))
        val add = j.getString("add")
        val s = JSONObject().put("address", add).put("port", j.get("port").toString().toInt())
            .put("id", j.getString("id")).put("security", j.optString("scy").ifEmpty { "auto" }).put("level", 8)
        val q = mapOf(
            "type" to j.optString("net", "tcp"), "host" to j.optString("host"), "path" to j.optString("path"),
            "security" to (if (j.optString("tls") == "tls") "tls" else "none"), "sni" to j.optString("sni"),
            "alpn" to j.optString("alpn"), "fp" to j.optString("fp"), "serviceName" to j.optString("path")
        )
        return wrap("vmess", s, stream(q, add))
    }

    private fun uriBased(proto: String, u: Uri): JSONObject {
        val q = HashMap<String, String>()
        u.queryParameterNames.forEach { q[it] = u.getQueryParameter(it) ?: "" }
        val host = u.host ?: throw IllegalArgumentException("Host tidak ditemukan")
        val s = JSONObject().put("address", host).put("port", if (u.port > 0) u.port else 443).put("level", 8)
        val cred = u.userInfo ?: throw IllegalArgumentException("UUID/password kosong")
        if (proto == "vless") {
            s.put("id", cred).put("encryption", q["encryption"] ?: "none")
            q["flow"]?.takeIf { it.isNotEmpty() }?.let { s.put("flow", it) }
        } else {
            s.put("password", cred)
            if (q["security"].isNullOrEmpty()) q["security"] = "tls"
        }
        return wrap(proto, s, stream(q, host))
    }

    private fun shadowsocks(link: String): JSONObject {
        var body = link.substringAfter("://").substringBefore("#")
        if ("plugin=" in body) throw IllegalArgumentException("Plugin Shadowsocks belum didukung")
        body = body.substringBefore('?')
        val userinfo: String
        val hostport: String
        if ('@' in body) {
            userinfo = body.substringBeforeLast('@'); hostport = body.substringAfterLast('@')
        } else {
            val d = b64(body); userinfo = d.substringBeforeLast('@'); hostport = d.substringAfterLast('@')
        }
        val ui = if (':' in userinfo) Uri.decode(userinfo) else b64(userinfo)
        val s = JSONObject().put("address", hostport.substringBeforeLast(':').trim('[', ']'))
            .put("port", hostport.substringAfterLast(':').toInt())
            .put("method", ui.substringBefore(':')).put("password", ui.substringAfter(':')).put("level", 8)
        return wrap("shadowsocks", s, null)
    }

    private fun stream(q: Map<String, String>, host: String): JSONObject {
        val net = (q["type"] ?: "tcp").ifEmpty { "tcp" }.let { if (it == "splithttp") "xhttp" else it }
        val h = q["host"].orEmpty()
        val path = q["path"].orEmpty().ifEmpty { "/" }
        val st = JSONObject().put("network", net)
        when (net) {
            "tcp" -> {}
            "ws" -> st.put("wsSettings", JSONObject().put("path", path).apply {
                if (h.isNotEmpty()) put("headers", JSONObject().put("Host", h))
            })
            "grpc" -> st.put("grpcSettings", JSONObject().put("serviceName", q["serviceName"].orEmpty()).put("multiMode", q["mode"] == "multi"))
            "httpupgrade" -> st.put("httpupgradeSettings", JSONObject().put("path", path).put("host", h))
            "xhttp" -> st.put("xhttpSettings", JSONObject().put("path", path).put("host", h).put("mode", q["mode"].orEmpty().ifEmpty { "auto" }))
            else -> throw IllegalArgumentException("Transport '$net' belum didukung")
        }
        val sni = q["sni"].orEmpty().ifEmpty { h.ifEmpty { host } }
        val fp = q["fp"].orEmpty().ifEmpty { "chrome" }
        when (q["security"].orEmpty()) {
            "tls" -> st.put("security", "tls").put("tlsSettings", JSONObject()
                .put("serverName", sni).put("fingerprint", fp)
                .put("allowInsecure", q["allowInsecure"] == "1" || q["allowInsecure"] == "true")
                .apply { q["alpn"]?.takeIf { it.isNotEmpty() }?.let { put("alpn", JSONArray(it.split(","))) } })
            "reality" -> st.put("security", "reality").put("realitySettings", JSONObject()
                .put("serverName", sni).put("fingerprint", fp).put("publicKey", q["pbk"].orEmpty())
                .put("shortId", q["sid"].orEmpty()).put("spiderX", q["spx"].orEmpty()))
        }
        return st
    }
}
