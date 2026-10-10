package net.ccbluex.liquidbounce.web

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import net.ccbluex.liquidbounce.CrossSine
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatRangeValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerRangeValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.KeyBindValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.features.value.OptionValue
import net.ccbluex.liquidbounce.features.value.TextValue
import net.ccbluex.liquidbounce.features.value.TitleValue
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.KeybindHelper
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

object LocalWebServer : MinecraftInstance(), Listenable {

    private var server: HttpServer? = null
    private val toggleQueue = ConcurrentLinkedQueue<String>()
    private val valueQueue  = ConcurrentLinkedQueue<ValueUpdate>()
    const val PORT = 8080

    private data class ValueUpdate(val module: String, val value: String, val raw: String)

    // ── Lifecycle ────────────────────────────────────────────────────────────

    fun start() {
        try {
            server = HttpServer.create(InetSocketAddress("127.0.0.1", PORT), 0).apply {
                createContext("/")              { serveHtml(it) }
                createContext("/api/state")    { serveState(it) }
                createContext("/api/toggle")   { serveToggle(it) }
                createContext("/api/setvalue") { serveSetValue(it) }
                createContext("/favicon.ico")  { it.sendResponseHeaders(204, -1) }
                executor = Executors.newSingleThreadExecutor { r ->
                    Thread(r, "CrossSine-Web").also { it.isDaemon = true }
                }
                start()
            }
            CrossSine.eventManager.registerListener(this)
            ClientUtils.logInfo("[LocalWebServer] Running at http://127.0.0.1:$PORT")
        } catch (e: Exception) {
            ClientUtils.logError("[LocalWebServer] Failed to start on port $PORT: ${e.message}")
        }
    }

    fun stop() {
        server?.stop(0)
        server = null
        ClientUtils.logInfo("[LocalWebServer] Stopped.")
    }

    // ── Main-thread toggle queue ─────────────────────────────────────────────

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        while (toggleQueue.isNotEmpty()) {
            val name = toggleQueue.poll() ?: break
            CrossSine.moduleManager.getModule(name)?.toggle()
        }
        while (valueQueue.isNotEmpty()) {
            val u = valueQueue.poll() ?: break
            applyValueUpdate(u)
        }
    }

    private fun applyValueUpdate(u: ValueUpdate) {
        val mod = CrossSine.moduleManager.getModule(u.module) ?: return
        val v   = mod.values.find { it.name.equals(u.value, ignoreCase = true) } ?: return
        try {
            when (v) {
                is BoolValue         -> v.set(u.raw.equals("true", true) || u.raw == "1")
                is OptionValue       -> v.set(u.raw.equals("true", true) || u.raw == "1")
                is IntegerValue      -> v.set(u.raw.toInt())
                is FloatValue        -> v.set(u.raw.toFloat())
                is ListValue         -> v.set(u.raw)
                is TextValue         -> v.set(u.raw)
                is IntegerRangeValue -> {
                    val parts = u.raw.split(",")
                    if (parts.size == 2) { v.setMin(parts[0].toInt()); v.setMax(parts[1].toInt()) }
                }
                is FloatRangeValue   -> {
                    val parts = u.raw.split(",")
                    if (parts.size == 2) { v.setMin(parts[0].toFloat()); v.setMax(parts[1].toFloat()) }
                }
                else                 -> { /* unsupported */ }
            }
        } catch (e: Throwable) {
            ClientUtils.logError("[LocalWebServer] setvalue failed for ${u.module}.${u.value}: ${e.message}")
        }
    }

    override fun handleEvents() = true

    // ── HTTP handlers ────────────────────────────────────────────────────────

    private fun serveHtml(ex: HttpExchange) {
        if (ex.requestMethod != "GET") { ex.sendResponseHeaders(405, -1); return }
        respond(ex, 200, "text/html; charset=utf-8", buildHtml().toByteArray(Charsets.UTF_8))
    }

    private fun serveState(ex: HttpExchange) {
        cors(ex)
        if (ex.requestMethod == "OPTIONS") { ex.sendResponseHeaders(204, -1); return }
        respond(ex, 200, "application/json; charset=utf-8", buildStateJson().toByteArray(Charsets.UTF_8))
    }

    private fun serveToggle(ex: HttpExchange) {
        cors(ex)
        if (ex.requestMethod == "OPTIONS") { ex.sendResponseHeaders(204, -1); return }
        val name = parseQuery(ex.requestURI.query ?: "")["name"]
        if (name != null) {
            toggleQueue.add(URLDecoder.decode(name, "UTF-8"))
            respond(ex, 200, "application/json", """{"ok":true}""".toByteArray())
        } else {
            ex.sendResponseHeaders(400, -1)
        }
    }

    private fun serveSetValue(ex: HttpExchange) {
        cors(ex)
        if (ex.requestMethod == "OPTIONS") { ex.sendResponseHeaders(204, -1); return }
        val q = parseQuery(ex.requestURI.query ?: "")
        val module = q["module"]?.let { URLDecoder.decode(it, "UTF-8") }
        val value  = q["value"]?.let  { URLDecoder.decode(it, "UTF-8") }
        val raw    = q["set"]?.let    { URLDecoder.decode(it, "UTF-8") }
        if (module != null && value != null && raw != null) {
            valueQueue.add(ValueUpdate(module, value, raw))
            respond(ex, 200, "application/json", """{"ok":true}""".toByteArray())
        } else {
            ex.sendResponseHeaders(400, -1)
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun respond(ex: HttpExchange, code: Int, type: String, body: ByteArray) {
        ex.responseHeaders["Content-Type"] = listOf(type)
        ex.sendResponseHeaders(code, body.size.toLong())
        ex.responseBody.use { it.write(body) }
    }

    private fun cors(ex: HttpExchange) {
        ex.responseHeaders["Access-Control-Allow-Origin"]  = listOf("*")
        ex.responseHeaders["Access-Control-Allow-Methods"] = listOf("GET,POST,OPTIONS")
        ex.responseHeaders["Access-Control-Allow-Headers"] = listOf("Content-Type")
    }

    private fun parseQuery(q: String): Map<String, String> =
        q.split("&").filter { '=' in it }.associate {
            val i = it.indexOf('='); it.substring(0, i) to it.substring(i + 1)
        }

    private fun j(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun buildStateJson(): String {
        val sb = StringBuilder("{\"categories\":[")
        val cats = ModuleCategory.values()
            .map { it to CrossSine.moduleManager.getModuleInCategory(it) }
            .filter { (_, mods) -> mods.isNotEmpty() }
        cats.forEachIndexed { ci, (cat, mods) ->
            if (ci > 0) sb.append(',')
            sb.append("""{"name":"${j(cat.displayName)}","modules":[""")
            mods.forEachIndexed { mi, mod ->
                if (mi > 0) sb.append(',')
                val bind = if (mod.keyBind != 0) KeybindHelper.getDisplayName(mod.keyBind) else ""
                sb.append("""{"name":"${j(mod.name)}","on":${mod.state},"bind":"${j(bind)}","settings":[""")
                val supported = mod.values.filter {
                    try { it.displayable } catch (_: Throwable) { true } && (
                        it is TitleValue || it is BoolValue || it is OptionValue ||
                        it is IntegerValue || it is FloatValue ||
                        it is IntegerRangeValue || it is FloatRangeValue ||
                        it is ListValue || it is TextValue || it is KeyBindValue
                    )
                }
                supported.forEachIndexed { vi, v ->
                    if (vi > 0) sb.append(',')
                    appendValueJson(sb, v)
                }
                sb.append("]}")
            }
            sb.append("]}")
        }
        sb.append("]}")
        return sb.toString()
    }

    private fun appendValueJson(sb: StringBuilder, v: net.ccbluex.liquidbounce.features.value.Value<*>) {
        when (v) {
            is TitleValue        -> sb.append("""{"name":"${j(v.name)}","type":"title"}""")
            is BoolValue         -> sb.append("""{"name":"${j(v.name)}","type":"bool","value":${v.get()}}""")
            is OptionValue       -> sb.append("""{"name":"${j(v.name)}","type":"bool","value":${v.get()}}""")
            is IntegerValue      -> sb.append(
                """{"name":"${j(v.name)}","type":"int","value":${v.get()},"min":${v.minimum},"max":${v.maximum},"suffix":"${j(v.suffix)}"}"""
            )
            is FloatValue        -> sb.append(
                """{"name":"${j(v.name)}","type":"float","value":${v.get()},"min":${v.minimum},"max":${v.maximum}}"""
            )
            is IntegerRangeValue -> {
                val r = v.get()
                sb.append("""{"name":"${j(v.name)}","type":"intrange","valueMin":${r.first},"valueMax":${r.last},"min":${v.minimum},"max":${v.maximum},"suffix":"${j(v.suffix)}"}""")
            }
            is FloatRangeValue   -> {
                val r = v.get()
                sb.append("""{"name":"${j(v.name)}","type":"floatrange","valueMin":${r.start},"valueMax":${r.endInclusive},"min":${v.minimum},"max":${v.maximum},"suffix":"${j(v.suffix)}"}""")
            }
            is ListValue         -> {
                val opts = v.values.joinToString(",") { "\"${j(it)}\"" }
                sb.append("""{"name":"${j(v.name)}","type":"list","value":"${j(v.get())}","options":[$opts]}""")
            }
            is TextValue         -> sb.append("""{"name":"${j(v.name)}","type":"text","value":"${j(v.get())}"}""")
            is KeyBindValue      -> sb.append("""{"name":"${j(v.name)}","type":"key","value":"${j(v.keyName)}"}""")
            else                 -> sb.append("""{"name":"${j(v.name)}","type":"unsupported"}""")
        }
    }

    // ── Embedded web GUI ─────────────────────────────────────────────────────

    /** Embedded ClickGUI HTML, loaded from src/main/resources/web/clickgui.html and cached. */
    private val cachedHtml: String by lazy {
        try {
            LocalWebServer::class.java.getResourceAsStream("/web/clickgui.html")?.use {
                it.bufferedReader(Charsets.UTF_8).readText()
            } ?: "<!DOCTYPE html><html><body><h1>CrossSine</h1><p>Could not load /web/clickgui.html from resources.</p></body></html>"
        } catch (e: Throwable) {
            ClientUtils.logError("[LocalWebServer] Failed to load embedded HTML: " + e.message)
            "<!DOCTYPE html><html><body><h1>CrossSine</h1><p>Error loading UI: " + e.message + "</p></body></html>"
        }
    }

    private fun buildHtml(): String = cachedHtml
}
