
package com.selfmind.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.content.SharedPreferences
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {

    private val bg = Color.rgb(9, 14, 25)
    private val panel = Color.rgb(18, 27, 43)
    private val panel2 = Color.rgb(24, 36, 55)
    private val accent = Color.rgb(82, 212, 174)
    private val text = Color.rgb(237, 244, 250)
    private val muted = Color.rgb(150, 166, 185)

    private lateinit var root: LinearLayout
    private lateinit var prefs: SharedPreferences
    private var page = "Chat"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("self_mind", MODE_PRIVATE)
        showPage("Chat")
    }

    private fun showPage(name: String) {
        page = name

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(18), dp(16), dp(18), dp(12))
        }

        setContentView(root)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        brand.addView(label("J.A.R.V.I.S.", 22f, text, true))
        brand.addView(label("PERSONAL AI DEVELOPMENT SYSTEM", 9f, muted, true))

        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))

        val status = label("● LOCAL MVP", 10f, accent, true)
        header.addView(status)

        root.addView(header)
        space(root, 18)

        when (name) {
            "Chat" -> chatPage()
            "Brain" -> brainPage()
            "Cyber Lab" -> cyberPage()
            "Builder" -> builderPage()
            "Memory" -> memoryPage()
        }

        val spacer = View(this)
        root.addView(spacer, LinearLayout.LayoutParams(1, 0, 1f))
        space(root, 10)

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(panel, 18)
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }

        listOf("Chat", "Brain", "Cyber Lab", "Builder", "Memory").forEach { item ->

            val b = TextView(this).apply {
                this.text = when (item) {
                    "Cyber Lab" -> "⌁\nCyber"
                    "Chat" -> "◉\nChat"
                    "Brain" -> "✧\nBrain"
                    "Builder" -> "⌘\nBuild"
                    else -> "▤\nMemory"
                }

                gravity = Gravity.CENTER
                textSize = 10f
                setTextColor(if (page == item) accent else muted)
                setPadding(dp(5), dp(3), dp(5), dp(3))
                setOnClickListener { showPage(item) }
            }

            nav.addView(b, LinearLayout.LayoutParams(0, dp(48), 1f))
        }

        root.addView(nav)
    }

    private fun chatPage() {
        root.addView(label("Your command centre", 23f, text, true))
        root.addView(
            label(
                "Give J.A.R.V.I.S. a task. This starter build runs in local demo mode.",
                13f,
                muted
            )
        )

        space(root, 14)

        val scroll = ScrollView(this)

        val messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val history = loadMessages()

        if (history.length() == 0) {
            addBubble(
                messages,
                "J.A.R.V.I.S.",
                "System online. I am running in local demo mode. Connect an AI backend in a later build to enable real model reasoning.",
                false
            )
        } else {
            for (i in 0 until history.length()) {
                val obj = history.optJSONObject(i) ?: continue

                addBubble(
                    messages,
                    obj.optString("role"),
                    obj.optString("content"),
                    obj.optString("role") == "You"
                )
            }
        }

        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        space(root, 10)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val input = EditText(this).apply {
            hint = "Message J.A.R.V.I.S.…"
            setHintTextColor(muted)
            setTextColor(this@MainActivity.text)
            textSize = 14f
            setSingleLine(false)
            maxLines = 3
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(panel2, 16)
        }

        row.addView(input, LinearLayout.LayoutParams(0, dp(54), 1f))

        val send = TextView(this).apply {
            this.text = " SEND "
            gravity = Gravity.CENTER
            setTextColor(bg)
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            background = rounded(accent, 14)
            setPadding(dp(12), dp(14), dp(12), dp(14))

            setOnClickListener {
                val prompt = input.text.toString().trim()

                if (prompt.isNotEmpty()) {
                    saveMessage("You", prompt)

                    val reply = localReply(prompt)
                    saveMessage("J.A.R.V.I.S.", reply)

                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(input.windowToken, 0)

                    showPage("Chat")
                }
            }
        }

        row.addView(
            send,
            LinearLayout.LayoutParams(-2, dp(54)).apply {
                leftMargin = dp(8)
            }
        )

        root.addView(row)
    }

    private fun brainPage() {
        root.addView(label("AI Brain", 23f, text, true))
        root.addView(label("Core intelligence status and planned integrations.", 13f, muted))
        space(root, 14)

        infoCard(
            "Reasoning engine",
            "Not connected",
            "Choose an OpenAI-compatible or local model endpoint in a future backend setup."
        )

        infoCard(
            "Memory system",
            "Local conversation memory",
            "Chat messages are saved on this device using app preferences."
        )

        infoCard(
            "Agent orchestrator",
            "Planned",
            "Will coordinate researcher, coder, tester and reviewer agents."
        )

        infoCard(
            "Self-improvement",
            "Approval-gated",
            "New code must be tested in an isolated workspace and approved before integration."
        )
    }

    private fun cyberPage() {
        root.addView(label("Cyber Intelligence", 23f, text, true))
        root.addView(
            label(
                "Learn and test security in systems you own or have permission to assess.",
                13f,
                muted
            )
        )

        space(root, 14)

        infoCard(
            "Learning path",
            "OWASP • Networking • Linux",
            "Build foundations before advanced vulnerability research."
        )

        infoCard(
            "Practice lab",
            "Not connected",
            "Use intentionally vulnerable training labs such as PortSwigger Academy or OWASP Juice Shop."
        )

        infoCard(
            "Safety boundary",
            "Authorized testing only",
            "No real-world scanning, exploitation, credential theft or persistence tools are included in this starter app."
        )

        infoCard(
            "Research notebook",
            "Planned",
            "Store findings with source, affected version, reproduction scope and remediation."
        )
    }

    private fun builderPage() {
        root.addView(label("Self Builder", 23f, text, true))
        root.addView(
            label(
                "Plan new capabilities. Execution is intentionally disabled until a sandbox backend is connected.",
                13f,
                muted
            )
        )

        space(root, 14)

        infoCard(
            "Module factory",
            "Draft mode",
            "Describe a feature and the future builder will create a plan, code changes and tests."
        )

        val field = EditText(this).apply {
            hint = "Example: Create a notes module with search…"
            setHintTextColor(muted)
            setTextColor(this@MainActivity.text)
            textSize = 14f
            minLines = 3
            gravity = Gravity.TOP
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(panel2, 14)
        }

        root.addView(field, LinearLayout.LayoutParams(-1, dp(112)))
        space(root, 10)

        val button = actionButton("Create development plan")
        root.addView(button)

        val result = TextView(this).apply {
            setTextColor(muted)
            textSize = 13f
            setPadding(dp(4), dp(12), dp(4), dp(4))
        }

        root.addView(result)

        button.setOnClickListener {
            val request = field.text.toString().trim()

            result.text = if (request.isBlank()) {
                "Enter a feature request first."
            } else {
                "DRAFT PLAN\n" +
                    "1. Clarify requirements\n" +
                    "2. Design module interface\n" +
                    "3. Generate implementation\n" +
                    "4. Write tests\n" +
                    "5. Run in isolated sandbox\n" +
                    "6. Review diff and request your approval\n\n" +
                    "Requested feature: $request\n\n" +
                    "Status: plan drafted locally; no code was executed."
            }
        }
    }

    private fun memoryPage() {
        root.addView(label("Long-term Memory", 23f, text, true))
        root.addView(label("Your saved local conversation history.", 13f, muted))
        space(root, 14)

        val count = loadMessages().length()

        infoCard(
            "Conversation records",
            count.toString(),
            "Stored locally on this Android device."
        )

        val clear = actionButton("Clear local chat history")
        root.addView(clear)

        clear.setOnClickListener {
            prefs.edit().remove("messages").apply()
            showPage("Memory")
        }

        space(root, 8)

        root.addView(
            label(
                "Privacy: this starter app does not upload your messages to a server.",
                12f,
                muted
            )
        )
    }

private fun testPermissionManager(): String {
    val action = SensitiveAction.DELETE_FILE

    PermissionManager.reset(action)

    val blocked = !PermissionManager.canExecute(action)

    PermissionManager.approve(action)

    val approved = PermissionManager.canExecute(action)

    PermissionManager.reset(action)

    return if (blocked && approved) {
        "Permission Manager test PASSED. Sensitive actions are blocked until approved."
    } else {
        "Permission Manager test FAILED. Please check the module."
    }
}

    private fun localReply(prompt: String): String {
        val p = prompt.lowercase()

        return when {
            listOf("hack", "hacking", "cyber", "security").any { p.contains(it) } ->
                "Cyber learning mode: start with networking, Linux, HTTP, OWASP Top 10 and safe practice labs. I can help explain concepts and plan authorized tests. This offline starter does not run scans or exploit systems."

            listOf("build", "create", "module", "feature", "app").any { p.contains(it) } ->
                "Builder mode is currently a local prototype. Open the Builder tab to draft a development plan. Real code generation and sandbox execution will be added when we connect a backend."

            p.contains("memory") ->
                "Your chat history is stored locally on this device. Open Memory to view the record count or clear it."

            else ->
                "I received your instruction: \"$prompt\". This first build is an offline interface prototype, not yet connected to a reasoning model. The next milestone is a secure backend and model connection."
        }
    }

    private fun saveMessage(role: String, content: String) {
        val arr = loadMessages()
        arr.put(JSONObject().put("role", role).put("content", content))
        prefs.edit().putString("messages", arr.toString()).apply()
    }

    private fun loadMessages(): JSONArray {
        return try {
            JSONArray(prefs.getString("messages", "[]") ?: "[]")
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun addBubble(
        parent: LinearLayout,
        role: String,
        content: String,
        isUser: Boolean
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(
                if (isUser) Color.rgb(24, 65, 61) else panel,
                16
            )
            setPadding(dp(13), dp(10), dp(13), dp(10))
        }

        card.addView(label(role, 11f, if (isUser) accent else muted, true))
        space(card, 4)
        card.addView(label(content, 14f, text))

        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = dp(9)
        parent.addView(card, params)
    }

    private fun infoCard(title: String, value: String, detail: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(panel, 16)
            setPadding(dp(15), dp(13), dp(15), dp(13))
        }

        box.addView(label(title, 12f, muted, true))
        space(box, 4)
        box.addView(label(value, 17f, accent, true))
        space(box, 3)
        box.addView(label(detail, 12f, text))

        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.bottomMargin = dp(10)
        root.addView(box, lp)
    }

    private fun actionButton(title: String): TextView =
        TextView(this).apply {
            text = title
            gravity = Gravity.CENTER
            setTextColor(bg)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            background = rounded(accent, 14)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

    private fun label(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }

    private fun space(parent: LinearLayout, height: Int) {
        parent.addView(
            View(this),
            LinearLayout.LayoutParams(1, dp(height))
        )
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
