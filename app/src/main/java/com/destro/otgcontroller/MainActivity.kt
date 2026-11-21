package com.destro.otgcontroller

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.InputDevice
import android.view.KeyEvent
import android.view.View
import android.widget.*
import android.content.res.ColorStateList

class MainActivity : Activity() {
    private lateinit var store: ProfileStore
    private var profiles = mutableListOf<GameProfile>()
    private var selected = 0
    private lateinit var status: TextView
    private lateinit var devices: TextView
    private lateinit var spinner: Spinner
    private lateinit var seek: SeekBar
    private lateinit var seekText: TextView

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun text(s: String, size: Int = 14, color: Int = Color.WHITE, bold: Boolean = false) =
        TextView(this).apply { text = s; textSize = size.toFloat(); setTextColor(color); if (bold) setTypeface(null, 1); setPadding(0, dp(6), 0, dp(6)) }
    private fun button(s: String, action: () -> Unit) = Button(this).apply {
        text = s; isAllCaps = false; setTextColor(Color.WHITE)
        backgroundTintList = ColorStateList.valueOf(0xFF263449.toInt())
        setOnClickListener { action() }
    }
    private fun heading(s: String) = text(s, 15, 0xFF35D0A0.toInt(), true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = ProfileStore(this); profiles = store.load()
        if (profiles.isEmpty()) profiles.add(GameProfile("Default"))
        val page = LinearLayout(this).apply { orientation = 1; setPadding(dp(18), dp(18), dp(18), dp(18)); setBackgroundColor(0xFF0A0E16.toInt()) }
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = 1 }
        scroll.addView(body); page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(page)

        body.addView(text("OTG GAMING CONTROLLER", 24, Color.WHITE, true))
        body.addView(text("V1.0  •  USB input diagnostics & visual profile editor", 13, 0xFF9EACC1.toInt()))
        body.addView(heading("DEVICE STATUS"))
        devices = text("Checking connected input devices…")
        body.addView(devices)
        body.addView(button("Refresh connected devices") { refreshDevices() })

        body.addView(heading("KEYBOARD TEST"))
        status = text("Press a key on a connected keyboard or controller.", 14, 0xFF35D0A0.toInt())
        body.addView(status)

        body.addView(heading("MOUSE / TOUCH TEST"))
        val test = InputCaptureView(this).apply {
            layoutParams = LinearLayout.LayoutParams(-1, dp(180))
            onStatus = { status.text = it }
        }
        body.addView(test)

        body.addView(heading("GAME PROFILES"))
        spinner = Spinner(this); body.addView(spinner)
        val row = LinearLayout(this).apply { orientation = 0 }
        row.addView(button("Add") { addProfile() }, LinearLayout.LayoutParams(0, dp(48), 1f))
        row.addView(button("Rename") { renameProfile() }, LinearLayout.LayoutParams(0, dp(48), 1f))
        row.addView(button("Delete") { deleteProfile() }, LinearLayout.LayoutParams(0, dp(48), 1f))
        body.addView(row)

        body.addView(heading("SENSITIVITY REFERENCE"))
        seekText = text("Reference value: 50")
        body.addView(seekText)
        seek = SeekBar(this).apply {
            max = 100; progress = profiles[0].sensitivity
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, v: Int, user: Boolean) {
                    seekText.text = "Reference value: $v"
                    if (user) profiles[selected] = profiles[selected].copy(sensitivity = v)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) { save() }
            })
        }
        body.addView(seek)

        body.addView(heading("VISUAL CONTROL LAYOUT"))
        body.addView(text("Markers are saved as visual references only. They do not map keys or inject touch input into games.", 13, 0xFFB6C2D2.toInt()))
        body.addView(button("Add visual marker") { addMarker() })
        body.addView(button("Show saved markers") { showMarkers() })
        body.addView(button("Reset visual layout") { profiles[selected] = profiles[selected].copy(controls = emptyList()); save(); toast("Layout cleared") })

        body.addView(heading("IMPORTANT"))
        body.addView(text("This app tests input only while it is open. Android and each game decide whether external keyboard or mouse input is supported. No touch injection, gameplay automation, anti-cheat bypass or game-setting changes.", 13, 0xFFB6C2D2.toInt()))
        page.addView(button("Save profile") { save(); toast("Profile saved") })
        refreshProfiles(); refreshDevices()
    }

    private fun save() = store.save(profiles)
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun refreshProfiles() {
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, profiles.map { it.name })
        spinner.setSelection(selected.coerceIn(0, profiles.lastIndex))
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, pos: Int, id: Long) {
                selected = pos
                seek.progress = profiles[pos].sensitivity
                seekText.text = "Reference value: ${profiles[pos].sensitivity}"
            }
        }
    }

    private fun addProfile() {
        val input = EditText(this); input.hint = "Profile name"
        android.app.AlertDialog.Builder(this).setTitle("New profile").setView(input)
            .setPositiveButton("Add") { _, _ ->
                profiles.add(GameProfile(input.text.toString().trim().ifBlank { "Profile ${profiles.size + 1}" }))
                selected = profiles.lastIndex; save(); refreshProfiles()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun renameProfile() {
        val input = EditText(this); input.setText(profiles[selected].name)
        android.app.AlertDialog.Builder(this).setTitle("Rename profile").setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) { profiles[selected] = profiles[selected].copy(name = name); save(); refreshProfiles() }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun deleteProfile() {
        if (profiles.size < 2) { toast("At least one profile is required"); return }
        android.app.AlertDialog.Builder(this).setMessage("Delete ${profiles[selected].name}?")
            .setPositiveButton("Delete") { _, _ -> profiles.removeAt(selected); selected = 0; save(); refreshProfiles() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun addMarker() {
        val input = EditText(this); input.hint = "Marker name"
        android.app.AlertDialog.Builder(this).setTitle("Add visual marker").setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().trim().ifBlank { "Marker" }
                profiles[selected] = profiles[selected].copy(controls = profiles[selected].controls + ControlItem(name, .5f, .5f))
                save(); toast("Marker saved")
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showMarkers() {
        val message = profiles[selected].controls.mapIndexed { i, c -> "${i + 1}. ${c.label} (${(c.x * 100).toInt()}%, ${(c.y * 100).toInt()}%)" }
            .ifEmpty { listOf("No visual markers saved.") }.joinToString("\n")
        android.app.AlertDialog.Builder(this).setTitle("Saved markers").setMessage(message).setPositiveButton("Close", null).show()
    }

    private fun refreshDevices() {
        val list: List<InputDevice> = InputDevice.getDeviceIds()
            .map { deviceId -> InputDevice.getDevice(deviceId) }
            .filterNotNull()

        devices.text = if (list.isEmpty()) {
            "No external input devices detected. Connect a USB OTG device and refresh."
        } else {
            list.joinToString("\\n") { device ->
                val sources = device.sources
                val types = mutableListOf<String>()

                if ((sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD) types.add("Keyboard")
                if ((sources and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE) types.add("Mouse")
                if ((sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) types.add("Gamepad")
                if ((sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) types.add("Joystick")

                "${device.name} • ${types.ifEmpty { listOf("Other input") }.joinToString(", ")}"
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (::status.isInitialized && event.action == KeyEvent.ACTION_DOWN) {
            val source = InputDevice.getDevice(event.deviceId)?.name ?: "Unknown device"
            status.text = "Key: ${KeyEvent.keyCodeToString(event.keyCode)} • Device: $source"
        }
        return super.dispatchKeyEvent(event)
    }
}
