package com.aibuilders.lockscreen

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "ai_builders_prefs"
        const val KEY_THEME = "theme"
        const val KEY_RAIN = "rain_enabled"
        const val KEY_DEADLINE = "deadline_iso"
        const val KEY_MILESTONE = "next_milestone"
        const val DEADLINE_DEC2 = "2026-12-02T17:00:00-05:00"
        const val DEADLINE_DEC10 = "2026-12-10T17:00:00-05:00"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(56, 72, 56, 56)
        }

        val header = TextView(this).apply {
            text = "AI Builders Lock Screen Settings"
            textSize = 22f
        }

        val rainSwitch = Switch(this).apply {
            text = "Cyber Matrix Digital Rain"
            isChecked = prefs.getBoolean(KEY_RAIN, true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean(KEY_RAIN, checked).apply()
            }
        }

        val themeSpinner = Spinner(this).apply {
            val themes = listOf("cyber", "glass", "retro")
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, themes)
            setSelection(themes.indexOf(prefs.getString(KEY_THEME, "cyber")).coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                    prefs.edit().putString(KEY_THEME, themes[pos]).apply()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        val deadlineSpinner = Spinner(this).apply {
            val labels = listOf("Dec 2, 2026 · 5:00 PM EST (PNW)", "Dec 10, 2026 · 5:00 PM EST (BeMyApp)")
            val values = listOf(DEADLINE_DEC2, DEADLINE_DEC10)
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, labels)
            val saved = prefs.getString(KEY_DEADLINE, DEADLINE_DEC2)
            setSelection(values.indexOf(saved).coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                    prefs.edit().putString(KEY_DEADLINE, values[pos]).apply()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        val milestoneInput = EditText(this).apply {
            hint = "Next milestone text"
            setText(prefs.getString(KEY_MILESTONE, "Nov 17 — Complete 15-min IBM SkillsBuild intro course"))
        }

        val saveBtn = Button(this).apply {
            text = "Save & Set as Lock Screen Wallpaper"
            setOnClickListener {
                prefs.edit().putString(KEY_MILESTONE, milestoneInput.text.toString()).apply()
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(this@MainActivity, CountdownWallpaperService::class.java)
                    )
                }
                startActivity(intent)
            }
        }

        layout.addView(header)
        layout.addView(rainSwitch)
        layout.addView(themeSpinner)
        layout.addView(deadlineSpinner)
        layout.addView(milestoneInput)
        layout.addView(saveBtn)
        setContentView(layout)
    }
}
