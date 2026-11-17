package com.selfmind.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private val bg = Color.rgb(9, 14, 25)
    private val panel = Color.rgb(18, 27, 43)
    private val panel2 = Color.rgb(24, 36, 55)
    private val accent = Color.rgb(82, 212, 174)
    private val textColor = Color.rgb(237, 244, 250)
    private val muted = Color.rgb(150, 166, 185)

    private lateinit var root: LinearLayout
    private lateinit var prefs: SharedPreferences

    private var page = "Chat"
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var wakeListening = false
    private var wakeDetected = false
    private var wakeStatus: TextView? = null

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val FILE_PICKER_REQUEST = 1001
        private const val MAX_PREVIEW_BYTES = 20 * 1024
        private const val MICROPHONE_REQUEST = 2001
        private const val PREF_MESSAGES = "messages"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("self_mind", MODE_PRIVATE)
        tts = TextToSpeech(this, this)
        showPage("Chat")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                tts?.language = Locale.US
            }
        }
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
        brand.addView(label("J.A.R.V.I.S.", 22f, textColor, true))
        brand.addView(label("PERSONAL AI DEVELOPMENT SYSTEM", 9f, muted, true))
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(label("● LOCAL MVP", 10f, accent, true))
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
            val button = TextView(this).apply {
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
            nav.addView(button, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        root.addView(nav)
    }

    private fun chatPage() {
        root.addView(label("Your command centre", 23f, textColor, true))
        root.addView(label(
            "Give J.A.R.V.I.S. a task. This starter build runs in local demo mode.",
            13f, muted
        ))
        space(root, 12)

        val wakeButton = actionButton(
            if (wakeListening) "Stop Jarvis Listening" else "Start Jarvis Wake Listening"
        )
        root.addView(wakeButton)
        wakeStatus = label(
            if (wakeListening) "Listening for: Jarvis" else "Wake listening is OFF",
            12f, accent
        )
        root.addView(wakeStatus!!)
        space(root, 12)

        wakeButton.setOnClickListener {
            if (wakeListening) {
                stopWakeListening()
                showPage("Chat")
            } else {
                askPermission(
                    SensitiveAction.ACCESS_MICROPHONE,
                    "J.A.R.V.I.S. wants to listen for the wake word Jarvis while this app is open. Allow?"
                ) { checkMicrophonePermission() }
            }
        }

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
                val role = obj.optString("role")
                addBubble(messages, role, obj.optString("content"), role == "You")
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
            setTextColor(textColor)
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
                    saveMessage("J.A.R.V.I.S.", localReply(prompt))
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(input.windowToken, 0)
                    showPage("Chat")
                }
            }
        }
        row.addView(send, LinearLayout.LayoutParams(-2, dp(54)).apply {
            leftMargin = dp(8)
        })
        root.addView(row)
    }

    private fun checkMicrophonePermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startWakeListening()
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), MICROPHONE_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MICROPHONE_REQUEST) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                startWakeListening()
            } else {
                Toast.makeText(this, "Microphone permission denied.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startWakeListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(
                this,
                "Speech recognition is not available on this phone.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        try {
            wakeListening = true
            wakeDetected = false
            updateWakeStatus("Listening for: Jarvis")
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    updateWakeStatus("Listening for: Jarvis")
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    if (wakeListening && !wakeDetected) {
                        if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                            error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                        ) {
                            restartWakeListening(600)
                        } else {
                            stopWakeListening()
                            updateWakeStatus("Listening stopped. Tap Start to try again.")
                        }
                    }
                }
                override fun onResults(results: Bundle?) {
                    if (!wakeListening) return
                    val matches = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    checkWakePhrase(matches?.firstOrNull().orEmpty())
                    if (wakeListening && !wakeDetected) restartWakeListening(500)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    if (!wakeListening || wakeDetected) return
                    val matches = partialResults?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )
                    checkWakePhrase(matches?.firstOrNull().orEmpty())
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            listenForWakeWord()
        } catch (e: Exception) {
            stopWakeListening()
            Toast.makeText(this, "Wake listening error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun listenForWakeWord() {
        if (!wakeListening || isFinishing) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            speechRecognizer?.startListening(intent)
        } catch (_: Exception) {
            stopWakeListening()
            updateWakeStatus("Could not start microphone listening.")
        }
    }

    private fun checkWakePhrase(spoken: String) {
        if (!wakeListening || wakeDetected) return
        val words = spoken.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z ]"), " ")
            .trim()
        if (words.split(Regex("\\s+")).any { it == "jarvis" || it == "jervis" }) {
            wakeDetected = true
            updateWakeStatus("Wake word detected!")
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {
            }
            tts?.speak(
                "Ji, Master Arhaan",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "JARVIS_WAKE_REPLY"
            )
            handler.postDelayed({
                if (wakeListening && !isFinishing) {
                    wakeDetected = false
                    listenForWakeWord()
                }
            }, 1800)
        }
    }

    private fun restartWakeListening(delay: Long) {
        handler.postDelayed({
            if (wakeListening && !wakeDetected && !isFinishing) listenForWakeWord()
        }, delay)
    }

    private fun updateWakeStatus(message: String) {
        wakeStatus?.text = message
    }

    private fun stopWakeListening() {
        wakeListening = false
        wakeDetected = false
        handler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
        updateWakeStatus("Wake listening is OFF")
    }

    override fun onPause() {
        if (wakeListening) stopWakeListening()
        super.onPause()
    }

    private fun brainPage() {
        root.addView(label("AI Brain", 23f, textColor, true))
        root.addView(label("Core intelligence status and planned integrations.", 13f, muted))
        space(root, 14)
        infoCard("Reasoning engine", "Not connected",
            "Choose an AI model endpoint in a future backend setup.")
        infoCard("Memory system", "Local conversation memory",
            "Chat messages are saved on this device.")
        infoCard("Agent orchestrator", "Planned",
            "Will coordinate researcher, coder, tester and reviewer agents.")
        infoCard("Self-improvement", "Approval-gated",
            "New code must be tested in an isolated workspace and approved before integration.")
        infoCard("Personal file access", "User-controlled",
            "Only a text file selected by you can be opened by this feature.")

        val fileButton = actionButton("Access Personal Text File")
        root.addView(fileButton)
        fileButton.setOnClickListener {
            askPermission(
                SensitiveAction.READ_PERSONAL_FILE,
                "J.A.R.V.I.S. wants to open a text file you select. Allow this action?"
            ) { openTextFilePicker() }
        }
    }

    private fun askPermission(
        action: SensitiveAction,
        description: String,
        onApproved: () -> Unit
    ) {
        AlertDialog.Builder(this)
            .setTitle("J.A.R.V.I.S. Permission")
            .setMessage(description)
            .setPositiveButton("Allow") { _, _ ->
                PermissionManager.approve(action)
                if (PermissionManager.consumeApproval(action)) onApproved()
            }
            .setNegativeButton("Deny") { _, _ ->
                PermissionManager.deny(action)
                PermissionManager.reset(action)
                Toast.makeText(this, "Permission denied. Action blocked.", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun openTextFilePicker() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
            }
            startActivityForResult(intent, FILE_PICKER_REQUEST)
        } catch (e: Exception) {
            Toast.makeText(
                this, "File picker could not open: ${e.message}", Toast.LENGTH_LONG
            ).show()
        }
    }

    @Deprecated("Using the document picker for this starter implementation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICKER_REQUEST && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            readSelectedTextFile(uri)
        }
    }

    private fun readSelectedTextFile(uri: Uri) {
        try {
            val fileName = contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else "Selected file"
            } ?: "Selected file"

            val output = ByteArrayOutputStream()
            contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(4096)
                var total = 0
                while (total < MAX_PREVIEW_BYTES) {
                    val count = input.read(buffer, 0, minOf(buffer.size, MAX_PREVIEW_BYTES - total))
                    if (count <= 0) break
                    output.write(buffer, 0, count)
                    total += count
                }
            } ?: throw Exception("Unable to open selected file.")

            val preview = output.toByteArray().toString(Charsets.UTF_8)
            AlertDialog.Builder(this)
                .setTitle("File Access Successful")
                .setMessage("File: $fileName\n\nText preview (maximum 20 KB):\n\n$preview")
                .setPositiveButton("Close", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(
                this, "File could not be opened: ${e.message}", Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun cyberPage() {
        root.addView(label("Cyber Intelligence", 23f, textColor, true))
        root.addView(label(
            "Learn and test security in systems you own or have permission to assess.",
            13f, muted
        ))
        space(root, 14)
        infoCard("Learning path", "OWASP • Networking • Linux",
            "Build foundations before advanced vulnerability research.")
        infoCard("Practice lab", "Not connected",
            "Use intentionally vulnerable training labs such as PortSwigger Academy or OWASP Juice Shop.")
        infoCard("Safety boundary", "Authorized testing only",
            "No real-world scanning, exploitation, or access without explicit authorization.")
    }

    private fun builderPage() {
        root.addView(label("Agent Builder", 23f, textColor, true))
        root.addView(label(
            "Plan tools and agents. This screen is a local project planner, not an active code generator.",
            13f, muted
        ))
        space(root, 14)
        infoCard("Research agent", "Planned",
            "Collects sources, summarizes findings and keeps references.")
        infoCard("Coding agent", "Planned",
            "Drafts code changes for review; it cannot modify this app automatically.")
        infoCard("Testing agent", "Planned",
            "Runs checks in an isolated environment before any approved change.")
        infoCard("Security guard", "Required",
            "Sensitive actions require your approval. Never store API keys inside the APK.")
    }

    private fun memoryPage() {
        root.addView(label("Local Memory", 23f, textColor, true))
        root.addView(label(
            "Conversation history is stored on this device using the existing self_mind preferences.",
            13f, muted
        ))
        space(root, 14)

        val history = loadMessages()
        infoCard("Saved messages", history.length().toString(),
            "These are local chat entries, not cloud-synced memory.")

        val clearButton = actionButton("Clear Conversation History")
        root.addView(clearButton)
        clearButton.setOnClickListener {
            askPermission(
                SensitiveAction.DELETE_FILE,
                "This will delete the saved J.A.R.V.I.S. conversation history on this device. Continue?"
            ) {
                prefs.edit().remove(PREF_MESSAGES).apply()
                Toast.makeText(this, "Conversation history cleared.", Toast.LENGTH_SHORT).show()
                showPage("Memory")
            }
        }
    }

    private fun loadMessages(): JSONArray {
        return try {
            JSONArray(prefs.getString(PREF_MESSAGES, "[]") ?: "[]")
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun saveMessage(role: String, content: String) {
        val messages = loadMessages()
        messages.put(JSONObject().apply {
            put("role", role)
            put("content", content)
        })
        // Keep the most recent 200 entries to limit local storage growth.
        val trimmed = JSONArray()
        val start = (messages.length() - 200).coerceAtLeast(0)
        for (i in start until messages.length()) {
            trimmed.put(messages.getJSONObject(i))
        }
        prefs.edit().putString(PREF_MESSAGES, trimmed.toString()).apply()
    }

    private fun localReply(prompt: String): String {
        val query = prompt.lowercase(Locale.ROOT)
        return when {
            "jarvis" in query -> "Ji. Main J.A.R.V.I.S. hoon. Filhaal local demo mode mein kaam kar raha hoon."
            "hello" in query || "hi" == query -> "Hello. J.A.R.V.I.S. local demo mode mein ready hai."
            "time" in query -> "Main abhi real-time system data se connected nahi hoon."
            "who are you" in query || "tum kon" in query || "who r u" in query ->
                "Main J.A.R.V.I.S. hoon, aapka local AI assistant prototype."
            "help" in query -> "Aap Chat mein message bhej sakte hain, Brain mein file select kar sakte hain, aur Cyber Lab mein security learning plan dekh sakte hain."
            else -> "Command receive ho gaya. Abhi main local demo mode mein hoon; real AI reasoning ke liye secure backend connection abhi add nahi hua hai."
        }
    }

    private fun addBubble(
        container: LinearLayout,
        role: String,
        message: String,
        isUser: Boolean
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(if (isUser) Color.rgb(27, 66, 65) else panel, 14)
            setPadding(dp(13), dp(10), dp(13), dp(10))
        }
        card.addView(label(role, 11f, if (isUser) accent else muted, true))
        space(card, 4)
        card.addView(label(message, 14f, textColor))
        val params = LinearLayout.LayoutParams(
            if (isUser) (resources.displayMetrics.widthPixels * 0.82f).toInt() else -1,
            -2
        )
        params.gravity = if (isUser) Gravity.END else Gravity.START
        params.bottomMargin = dp(8)
        container.addView(card, params)
    }

    private fun infoCard(title: String, status: String, description: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(panel, 16)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        card.addView(label(title, 15f, textColor, true))
        space(card, 4)
        card.addView(label(status, 12f, accent, true))
        space(card, 4)
        card.addView(label(description, 12f, muted))
        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = dp(10)
        root.addView(card, params)
    }

    private fun actionButton(caption: String): TextView {
        return TextView(this).apply {
            text = caption
            gravity = Gravity.CENTER
            textSize = 14f
            setTextColor(bg)
            setTypeface(null, Typeface.BOLD)
            background = rounded(accent, 14)
            setPadding(dp(14), dp(13), dp(14), dp(13))
        }
    }

    private fun label(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            setLineSpacing(dp(2).toFloat(), 1f)
        }
    }

    private fun space(container: LinearLayout, heightDp: Int) {
        val spacer = View(this)
        container.addView(spacer, LinearLayout.LayoutParams(1, dp(heightDp)))
    }

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    override fun onDestroy() {
        stopWakeListening()
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}
