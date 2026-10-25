package com.example.v2rang

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder

data class Server(val name: String, val proto: String, val host: String, val link: String)

object Parser {
    /** Membaca tautan vmess/vless/trojan/ss menjadi Server (hanya metadata tampilan + tautan asli). */
    fun parse(raw: String): Server? {
        val s = raw.trim()
        val m = Regex("^(vmess|vless|trojan|ss)://(.+)$", RegexOption.IGNORE_CASE).find(s) ?: return null
        val proto = m.groupValues[1].lowercase()
        var r = m.groupValues[2]
        var name = ""
        val host: String
        if (proto == "vmess") {
            val j = try { JSONObject(String(Base64.decode(r, Base64.DEFAULT))) } catch (e: Exception) { return null }
            name = j.optString("ps")
            host = j.optString("add", "?") + ":" + j.optString("port", "?")
        } else {
            val f = r.indexOf('#')
            if (f >= 0) {
                name = try { URLDecoder.decode(r.substring(f + 1), "UTF-8") } catch (e: Exception) { r.substring(f + 1) }
                r = r.substring(0, f)
            }
            val at = r.indexOf('@')
            if (at >= 0) r = r.substring(at + 1)
            host = r.substringBefore('?').substringBefore('/').ifEmpty { "?" }
        }
        try { ConfigBuilder.build(s) } catch (e: Exception) { return null }
        return Server(name.ifEmpty { host }, proto, host, s)
    }
}

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("v2rang", Context.MODE_PRIVATE)

    fun load(): List<Server> {
        val a = JSONArray(sp.getString("servers", "[]"))
        return List(a.length()) {
            val o = a.getJSONObject(it)
            Server(o.getString("name"), o.getString("proto"), o.getString("host"), o.getString("link"))
        }
    }

    fun save(list: List<Server>) {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("name", it.name).put("proto", it.proto).put("host", it.host).put("link", it.link)) }
        sp.edit().putString("servers", a.toString()).apply()
    }

    var dns: String
        get() = sp.getString("dns", "1.1.1.1") ?: "1.1.1.1"
        set(v) { sp.edit().putString("dns", v).apply() }
}
