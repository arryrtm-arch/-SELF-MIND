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
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

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
    private var listeningForCommand = false
    private var wakeStatus: TextView? = null

    private val handler = Handler(Looper.getMainLooper())
    private val networkExecutor = Executors.newSingleThreadExecutor()

    private val backendUrl =
        "https://jarvis-ai-brain.arryrtm.workers.dev/chat"

    companion object {
        private const val FILE_PICKER_REQUEST = 1001
        private const val MAX_PREVIEW_BYTES = 20 * 1024
        private const val MICROPHONE_REQUEST = 2001
        private const val PREF_MESSAGES = "messages"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Existing local memory name must remain unchanged.
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
        brand.addView(
            label("PERSONAL AI DEVELOPMENT SYSTEM", 9f, muted, true)
        )

        header.addView(
            brand,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        header.addView(
            label("● ONLINE AI", 10f, accent, true)
        )

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
        root.addView(
            spacer,
            LinearLayout.LayoutParams(1, 0, 1f)
        )

        space(root, 10)

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = rounded(panel, 18)
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }

        listOf("Chat", "Brain", "Cyber Lab", "Builder", "Memory")
            .forEach { item ->

                val button = TextView(this).apply {
                    text = when (item) {
                        "Cyber Lab" -> "⌁\nCyber"
                        "Chat" -> "◉\nChat"
                        "Brain" -> "✧\nBrain"
                        "Builder" -> "⌘\nBuild"
                        else -> "▤\nMemory"
                    }

                    gravity = Gravity.CENTER
                    textSize = 10f

                    setTextColor(
                        if (page == item) accent else muted
                    )

                    setPadding(dp(5), dp(3), dp(5), dp(3))

                    setOnClickListener {
                        showPage(item)
                    }
                }

                nav.addView(
                    button,
                    LinearLayout.LayoutParams(0, dp(48), 1f)
                )
            }

        root.addView(nav)
    }

    private fun chatPage() {
        root.addView(
            label("Your command centre", 23f, textColor, true)
        )

        root.addView(
            label(
                "Talk to J.A.R.V.I.S. using online AI.",
                13f,
                muted
            )
        )

        space(root, 12)

        val wakeButton = actionButton(
            if (wakeListening)
                "Stop Jarvis Listening"
            else
                "Start Jarvis Wake Listening"
        )

        root.addView(wakeButton)

        wakeStatus = label(
            if (wakeListening)
                "Listening for: Jarvis"
            else
                "Wake listening is OFF",
            12f,
            accent
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
                ) { startVoiceAssistant() }
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
                "System online. Online AI connection is ready to be tested.",
                false
            )
        } else {
            for (i in 0 until history.length()) {
                val obj = history.optJSONObject(i) ?: continue
                val role = obj.optString("role")

                addBubble(
                    messages,
                    role,
                    obj.optString("content"),
                    role == "You"
                )
            }
        }

        scroll.addView(messages)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

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
            setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(12)
            )
            background = rounded(panel2, 16)
        }

        row.addView(
            input,
            LinearLayout.LayoutParams(0, dp(54), 1f)
        )

        val send = TextView(this).apply {
            text = " SEND "
            gravity = Gravity.CENTER
            setTextColor(bg)
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            background = rounded(accent, 14)

            setPadding(
                dp(12),
                dp(14),
                dp(12),
                dp(14)
            )

            setOnClickListener {
                val prompt = input.text.toString().trim()

                if (prompt.isNotEmpty()) {
                

                    (getSystemService(Context.INPUT_METHOD_SERVICE)
                        as InputMethodManager)
                        .hideSoftInputFromWindow(input.windowToken, 0)

                    sendToJarvis(prompt)
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
        private fun startVoiceAssistant() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(
                this,
                "Speech recognition available nahi hai",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                MICROPHONE_REQUEST
            )
            return
        }

        wakeListening = true
        wakeDetected = false
        listeningForCommand = false
        startWakeListening()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == MICROPHONE_REQUEST) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                startVoiceAssistant()
            } else {
                Toast.makeText(
                    this,
                    "Microphone permission nahi mili",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startWakeListening() {
        if (!wakeListening || wakeDetected) return

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) return

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

            speechRecognizer?.setRecognitionListener(
                object : RecognitionListener {

                    override fun onReadyForSpeech(params: Bundle?) {
                        wakeStatus?.text = "● J.A.R.V.I.S. SUN RAHA HAI"
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        if (listeningForCommand) {
                            listeningForCommand = false
                            wakeDetected = false
                        }

                        if (wakeListening) {
                            handler.postDelayed({
                                restartWakeListening()
                            }, 1000)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                        val spokenText = matches
                            ?.firstOrNull()
                            ?.lowercase(Locale.getDefault())
                            ?: ""

                        if (listeningForCommand) {
                            listeningForCommand = false
                            wakeDetected = false

                            if (spokenText.isNotBlank()) {
                                sendToJarvis(spokenText)
                            }

                            if (wakeListening) {
                                handler.postDelayed({
                                    restartWakeListening()
                                }, 1800)
                            }
                            return
                        }

                        if (spokenText.contains("jarvis") ||
                            spokenText.contains("jervis")
                        ) {
                            wakeDetected = true
                            speak("Ji, Master Arhaan")

                            handler.postDelayed({
                                listenForCommand()
                            }, 1200)
                        } else {
                            restartWakeListening()
                        }
                    }

                    override fun onPartialResults(
                        partialResults: Bundle?
                    ) {}

                    override fun onEvent(
                        eventType: Int,
                        params: Bundle?
                    ) {}
                }
            )

            val intent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "en-IN"
                )
                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    false
                )
            }

            speechRecognizer?.startListening(intent)

        } catch (e: Exception) {
            wakeStatus?.text = "Voice assistant error"
            restartWakeListening()
        }
    }

    private fun listenForCommand() {
        if (!wakeListening) return

        listeningForCommand = true

        try {
            val intent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "en-IN"
                )
                putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    false
                )
            }

            speechRecognizer?.startListening(intent)

        } catch (e: Exception) {
            listeningForCommand = false
            wakeDetected = false
            restartWakeListening()
        }
    }

    private fun restartWakeListening() {
        if (!wakeListening || wakeDetected) return

        handler.postDelayed({
            if (wakeListening && !wakeDetected) {
                startWakeListening()
            }
        }, 700)
    }

    private fun stopVoiceAssistant() {
        wakeListening = false
        wakeDetected = false
        listeningForCommand = false

        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }

        speechRecognizer = null
        wakeStatus?.text = "● VOICE ASSISTANT OFF"
    }
        private fun sendToJarvis(prompt: String) {
        val cleanPrompt = prompt.trim()

        if (cleanPrompt.isBlank()) return

        saveChatMessage("user", cleanPrompt)

        showPage("Chat")

        networkExecutor.execute {
            try {
                val connection = URL(backendUrl)
                    .openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.connectTimeout = 20000
                connection.readTimeout = 60000
                connection.doOutput = true
                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                val requestBody = JSONObject().apply {
                    put("message", cleanPrompt)
                    put("history", buildApiHistory())
                }

                OutputStreamWriter(
                    connection.outputStream,
                    Charsets.UTF_8
                ).use {
                    it.write(requestBody.toString())
                }

                val responseCode = connection.responseCode

                val responseStream =
                    if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                val responseText =
                    responseStream.bufferedReader().use {
                        it.readText()
                    }

                connection.disconnect()

                if (responseCode !in 200..299) {
                    throw Exception("Server error: $responseCode")
                }

                val json = JSONObject(responseText)
                val reply = json.optString(
                    "reply",
                    "Mujhe response nahi mila."
                )

                saveChatMessage("assistant", reply)

                runOnUiThread {
                    if (page == "Chat") {
                        showPage("Chat")
                    }
                    speak(reply)
                }

            } catch (e: Exception) {
                val errorMessage =
                    "Online connection mein problem hai: ${e.message}"

                saveChatMessage("assistant", errorMessage)

                runOnUiThread {
                    if (page == "Chat") {
                        showPage("Chat")
                    }
                }
            }
        }
    }

    private fun saveChatMessage(
        role: String,
        message: String
    ) {
        try {
            val saved = JSONArray(
                prefs.getString(PREF_MESSAGES, "[]")
            )

            val item = JSONObject().apply {
                put("role", role)
                put("content", message)
                put("time", System.currentTimeMillis())
            }

            saved.put(item)

            while (saved.length() > 200) {
                saved.remove(0)
            }

            prefs.edit()
                .putString(PREF_MESSAGES, saved.toString())
                .apply()

        } catch (_: Exception) {
        }
    }

    private fun buildApiHistory(): JSONArray {
        val history = JSONArray()

        try {
            val saved = JSONArray(
                prefs.getString(PREF_MESSAGES, "[]")
            )

            val start = (saved.length() - 11).coerceAtLeast(0)

            // Aakhri item current prompt hai; use alag se bheja ja raha hai.
            for (i in start until (saved.length() - 1).coerceAtLeast(0)) {
                val item = saved.optJSONObject(i) ?: continue

                val role = item.optString("role")
                val content = item.optString("content")

                if (content.isBlank()) continue
                if (role != "user" && role != "assistant") continue

                history.put(
                    JSONObject().apply {
                        put("role", role)
                        put("content", content)
                    }
                )
            }

        } catch (_: Exception) {
        }

        return history
    }
        private fun speak(message: String) {
        if (message.isBlank()) return

        runOnUiThread {
            try {
                tts?.speak(
                    message,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "JARVIS_SPEECH"
                )
            } catch (_: Exception) {
                // Voice output unavailable
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // App background mein jaane par microphone listening band.
        stopVoiceAssistant()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        stopVoiceAssistant()

        try {
            tts?.stop()
            tts?.shutdown()
            networkExecutor.shutdown()
        } catch (_: Exception) {
        }

        super.onDestroy()
    }
        private fun brainPage() {
        root.addView(label("AI Brain", 23f, textColor, true))
        root.addView(label(
            "Core intelligence status and planned integrations.",
            13f, muted
        ))
        space(root, 14)

        infoCard(
            "Reasoning engine",
            "Online connection configured",
            "Advanced reasoning backend ke through chalegi. Internet zaroori hai."
        )

        infoCard(
            "Memory system",
            "Local conversation memory",
            "Chat messages is device par save honge."
        )

        infoCard(
            "Agent orchestrator",
            "Planned",
            "Researcher, coder, tester aur reviewer agents ko coordinate karega."
        )

        infoCard(
            "Self-improvement",
            "Approval-gated",
            "Naya code test aur aapki approval ke baad hi integrate hoga."
        )

        infoCard(
            "Personal file access",
            "User-controlled",
            "Sirf aapke select kiye hue text file ko open kiya jayega."
        )

        val fileButton = actionButton("Access Personal Text File")
        root.addView(fileButton)

        fileButton.setOnClickListener {
            askPermission(
                SensitiveAction.READ_PERSONAL_FILE,
                "J.A.R.V.I.S. aapse select ki hui text file open karna chahta hai. Allow?"
            ) {
                openTextFilePicker()
            }
        }
    }

    private fun cyberPage() {
        root.addView(label("Cyber Intelligence", 23f, textColor, true))
        root.addView(label(
            "Sirf apne systems ya permission wale systems ki security seekhein aur test karein.",
            13f, muted
        ))
        space(root, 14)

        infoCard(
            "Learning path",
            "OWASP • Networking • Linux",
            "Advanced security se pehle basic foundations seekhein."
        )

        infoCard(
            "Practice lab",
            "Not connected",
            "PortSwigger Academy aur OWASP Juice Shop jaise training labs use kar sakte hain."
        )

        infoCard(
            "Safety boundary",
            "Authorized testing only",
            "Bina permission kisi real system ko scan, exploit ya access nahi karna."
        )
    }

    private fun builderPage() {
        root.addView(label("Agent Builder", 23f, textColor, true))
        root.addView(label(
            "Tools aur AI agents ka project planner. Abhi ye active code generator nahi hai.",
            13f, muted
        ))
        space(root, 14)

        infoCard(
            "Research agent",
            "Planned",
            "Sources collect karega, findings summarize karega aur references save karega."
        )

        infoCard(
            "Coding agent",
            "Planned",
            "Code changes draft karega; bina approval app ko modify nahi karega."
        )

        infoCard(
            "Testing agent",
            "Planned",
            "Approved changes se pehle isolated environment mein tests karega."
        )

        infoCard(
            "Security guard",
            "Required",
            "Sensitive actions ke liye aapki permission zaroori hogi."
        )
    }

    private fun memoryPage() {
        root.addView(label("Local Memory", 23f, textColor, true))
        root.addView(label(
            "Conversation history is device par existing self_mind preferences mein save hoti hai.",
            13f, muted
        ))
        space(root, 14)

        val history = loadMessages()

        infoCard(
            "Saved messages",
            history.length().toString(),
            "Ye local chat entries hain, cloud-synced memory nahi."
        )

        val clearButton = actionButton("Clear Conversation History")
        root.addView(clearButton)

        clearButton.setOnClickListener {
            askPermission(
                SensitiveAction.DELETE_FILE,
                "Isse device ki saved J.A.R.V.I.S. conversation history delete ho jayegi. Continue?"
            ) {
                prefs.edit()
                    .remove(PREF_MESSAGES)
                    .apply()

                Toast.makeText(
                    this,
                    "Conversation history cleared.",
                    Toast.LENGTH_SHORT
                ).show()

                showPage("Memory")
            }
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

                if (PermissionManager.consumeApproval(action)) {
                    onApproved()
                }
            }
            .setNegativeButton("Deny") { _, _ ->
                PermissionManager.deny(action)
                PermissionManager.reset(action)

                Toast.makeText(
                    this,
                    "Permission denied. Action blocked.",
                    Toast.LENGTH_SHORT
                ).show()
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
                this,
                "File picker nahi khul saka: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @Deprecated("Using the document picker for this starter implementation")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_PICKER_REQUEST &&
            resultCode == RESULT_OK
        ) {
            val uri = data?.data ?: return
            readSelectedTextFile(uri)
        }
    }

    private fun readSelectedTextFile(uri: Uri) {
        try {
            val fileName = contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    "Selected file"
                }
            } ?: "Selected file"

            val output = ByteArrayOutputStream()

            contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(4096)
                var total = 0

                while (total < MAX_PREVIEW_BYTES) {
                    val count = input.read(
                        buffer,
                        0,
                        minOf(buffer.size, MAX_PREVIEW_BYTES - total)
                    )

                    if (count <= 0) break

                    output.write(buffer, 0, count)
                    total += count
                }
            } ?: throw Exception("File open nahi ho payi.")

            val preview = output.toByteArray().toString(Charsets.UTF_8)

            AlertDialog.Builder(this)
                .setTitle("File Access Successful")
                .setMessage(
                    "File: $fileName\n\n" +
                    "Text preview (maximum 20 KB):\n\n$preview"
                )
                .setPositiveButton("Close", null)
                .show()

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "File read nahi ho payi: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
        private fun loadMessages(): JSONArray {
        return try {
            JSONArray(
                prefs.getString(PREF_MESSAGES, "[]") ?: "[]"
            )
        } catch (_: Exception) {
            JSONArray()
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

            background = rounded(
                if (isUser) Color.rgb(27, 66, 65) else panel,
                14
            )

            setPadding(
                dp(13),
                dp(10),
                dp(13),
                dp(10)
            )
        }

        card.addView(
            label(
                role,
                11f,
                if (isUser) accent else muted,
                true
            )
        )

        space(card, 4)

        card.addView(
            label(message, 14f, textColor)
        )

        val params = LinearLayout.LayoutParams(
            if (isUser) {
                (resources.displayMetrics.widthPixels * 0.82f).toInt()
            } else {
                -1
            },
            -2
        )

        params.gravity = if (isUser) {
            Gravity.END
        } else {
            Gravity.START
        }

        params.bottomMargin = dp(8)

        container.addView(card, params)
    }

    private fun infoCard(
        title: String,
        status: String,
        description: String
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(panel, 16)

            setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(12)
            )
        }

        card.addView(
            label(title, 15f, textColor, true)
        )

        space(card, 4)

        card.addView(
            label(status, 12f, accent, true)
        )

        space(card, 4)

        card.addView(
            label(description, 12f, muted)
        )

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

            setPadding(
                dp(14),
                dp(13),
                dp(14),
                dp(13)
            )
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

            if (bold) {
                setTypeface(null, Typeface.BOLD)
            }

            setLineSpacing(dp(2).toFloat(), 1f)
        }
    }

    private fun space(
        container: LinearLayout,
        heightDp: Int
    ) {
        val spacer = View(this)

        container.addView(
            spacer,
            LinearLayout.LayoutParams(1, dp(heightDp))
        )
    }

    private fun rounded(
        color: Int,
        radiusDp: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density + 0.5f
        ).toInt()
    }
    private fun stopWakeListening() {
    stopVoiceAssistant()
}
}
