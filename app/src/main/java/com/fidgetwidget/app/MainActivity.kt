package com.fidgetwidget.app

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.window.OnBackInvokedDispatcher
import kotlin.random.Random

class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var bubbles: FloatingBubblesView
    private lateinit var startOverlay: View
    private lateinit var statusText: TextView

    /** True while the kids' session is meant to be locked. */
    private var wantLock = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = FrameLayout(this)
        bubbles = FloatingBubblesView(this)
        root.addView(bubbles, FrameLayout.LayoutParams(-1, -1))

        // Parent exit: visible padlock, hold for 5 seconds, then answer a question.
        val hint = TextView(this).apply {
            text = "Keep holding to unlock…"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.WHITE)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(0xCC000000.toInt())
            }
            visibility = View.GONE
        }
        val lockButton = HoldUnlockButton(this,
            onHoldChanged = { holding -> hint.visibility = if (holding) View.VISIBLE else View.GONE },
            onComplete = { showParentGate() })
        val btnParams = FrameLayout.LayoutParams(dp(64), dp(64), Gravity.TOP or Gravity.END)
        val hintParams = FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END)
        root.addView(lockButton, btnParams)
        root.addView(hint, hintParams)

        // Keep the button clear of the status bar / camera cutout even while bars are hidden.
        root.setOnApplyWindowInsetsListener { _, insets ->
            var top = 0
            var right = 0
            if (Build.VERSION.SDK_INT >= 30) {
                val i = insets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                top = i.top; right = i.right
            }
            btnParams.topMargin = top + dp(16)
            btnParams.rightMargin = right + dp(16)
            hintParams.topMargin = top + dp(16) + dp(64) + dp(8)
            hintParams.rightMargin = right + dp(12)
            lockButton.layoutParams = btnParams
            hint.layoutParams = hintParams
            insets
        }
        btnParams.topMargin = dp(48); btnParams.rightMargin = dp(16)
        hintParams.topMargin = dp(48 + 64 + 8); hintParams.rightMargin = dp(12)

        startOverlay = buildStartOverlay()
        root.addView(startOverlay, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)

        // Swallow Back entirely.
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_OVERLAY) { }
        }
    }

    @Deprecated("Back is intentionally disabled")
    @Suppress("MissingSuperCall")
    override fun onBackPressed() { /* disabled */ }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
            // Focus returns after the system "Pin app?" prompt closes; check whether it took.
            if (wantLock) handler.postDelayed({ verifyPinned() }, 600)
        }
    }

    override fun onDestroy() {
        bubbles.release()
        super.onDestroy()
    }

    private fun isPinned(): Boolean {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

    private fun startKidsMode() {
        wantLock = true
        startOverlay.visibility = View.GONE
        try {
            startLockTask()
        } catch (e: Exception) {
            showStart(getString(R.string.app_name) + " couldn't lock the screen: ${e.message}")
            wantLock = false
            return
        }
        // If pinning is turned off in Settings, no prompt appears and nothing happens.
        handler.postDelayed({ if (hasWindowFocus()) verifyPinned() }, 2500)
    }

    private fun verifyPinned() {
        if (wantLock && !isPinned() && hasWindowFocus()) {
            wantLock = false
            showStart(
                "The screen isn't locked yet.\n\nTurn on App pinning: Settings → Security " +
                    "(or search Settings for \"pinning\"). Then tap Start again and choose Pin."
            )
        }
    }

    private fun unlockForParent() {
        wantLock = false
        try { stopLockTask() } catch (_: Exception) { }
        showStart("Unlocked. You can leave the app normally now.")
    }

    private fun showStart(message: String?) {
        statusText.text = message ?: ""
        statusText.visibility = if (message == null) View.GONE else View.VISIBLE
        startOverlay.visibility = View.VISIBLE
    }

    /** A simple grown-up check so small kids can't exit even if they find the corner. */
    private fun showParentGate() {
        val a = Random.nextInt(6, 10)
        val b = Random.nextInt(6, 10)
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Answer"
            gravity = Gravity.CENTER
        }
        val box = FrameLayout(this).apply {
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(input)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Grown-ups only")
            .setMessage("What is $a × $b?")
            .setView(box)
            .setPositiveButton("Unlock") { _, _ ->
                if (input.text.toString().trim() == (a * b).toString()) unlockForParent()
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.setOnDismissListener { hideSystemBars() }
        dialog.show()
    }

    private fun buildStartOverlay(): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(32), dp(32), dp(32))
            setBackgroundColor(0xF21B1640.toInt())
            isClickable = true // block touches reaching the board
        }
        col.addView(text("Fidget Widget", 34f, bold = true))
        col.addView(space(12))
        col.addView(text("Catch and pop the bubbles!", 18f, color = 0xFFFFD23D.toInt()))
        col.addView(space(32))

        val start = Button(this).apply {
            text = "Start  •  lock phone"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(Color.WHITE)
            isAllCaps = false
            background = GradientDrawable().apply {
                cornerRadius = dp(32).toFloat()
                setColor(0xFFFF5C7A.toInt())
            }
            setPadding(dp(32), dp(16), dp(32), dp(16))
            setOnClickListener { startKidsMode() }
        }
        col.addView(start)
        col.addView(space(20))

        statusText = text("", 15f, color = 0xFFFFD23D.toInt()).apply { visibility = View.GONE }
        col.addView(statusText)
        col.addView(space(20))

        col.addView(
            text(
                "To exit: press and hold the padlock (top right) for 5 seconds, then answer the question.\n\n" +
                    "For a stronger lock, turn on \"Ask for PIN before unpinning\" in Settings → Security → App pinning. " +
                    "Then nobody can leave the app without your phone PIN.",
                14f, color = 0xCCFFFFFF.toInt()
            )
        )
        col.addView(space(16))
        val settings = Button(this).apply {
            text = "Open security settings"
            isAllCaps = false
            setOnClickListener {
                try { startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) } catch (_: Exception) { }
            }
        }
        col.addView(settings)
        return col
    }

    private fun text(s: String, sp: Float, bold: Boolean = false, color: Int = Color.WHITE) =
        TextView(this).apply {
            text = s
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun space(h: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }
}
