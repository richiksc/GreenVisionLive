package codes.richiksc.greenvisionlive

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.URLUtil
import androidx.appcompat.app.AppCompatActivity
import com.github.niqdev.mjpeg.DisplayMode
import com.github.niqdev.mjpeg.Mjpeg
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*


class MainActivity : AppCompatActivity() {

    private val IPCAM_URL = "http://10.18.16.16:3030/video_feed"
    private val TIMEOUT = 5

    private var mjpgUrl: String = IPCAM_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        val prefs = getPreferences(Context.MODE_PRIVATE) ?: return
        mjpgUrl = prefs.getString(getString(R.string.mjpg_url_key), IPCAM_URL).toString()
        mjpg_url_input.setText(mjpgUrl)
        mjpg_url_input.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE) {
                updateMjpgUrl()
            }
            false
        }
    }

    override fun onConfigurationChanged(config: Configuration) {
        super.onConfigurationChanged(config)
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                    // Set the content to appear under the system bars so that the
                    // content doesn't resize when the system bars hide and show.
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    // Hide the nav bar and status bar
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
            supportActionBar?.hide()
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            supportActionBar?.show()
        }
    }

    private fun calculateDisplayMode(): DisplayMode {
        val orientation = resources.configuration.orientation
        return if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            DisplayMode.FULLSCREEN
        else
            DisplayMode.BEST_FIT
    }

    private fun initLiveView() {
        Mjpeg.newInstance()
            .open(mjpgUrl, TIMEOUT)
            .subscribe(
                { inputStream ->
                    camera_view.setSource(inputStream)
                    camera_view.setDisplayMode(calculateDisplayMode())
                    camera_view.showFps(true)
                    play_pause_button.show()
                },
                { throwable ->
                    Log.e(javaClass.name, "mjpeg error", throwable)
                    Snackbar.make(
                        findViewById(R.id.content_view),
                        "Error fetching stream",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            )
    }

    @Suppress("UNUSED_PARAMETER")
    fun updateMjpgUrl(view: View? = null) {
        mjpg_url_input.clearFocus()
        val textContent = mjpg_url_input.text.toString()
        if (URLUtil.isValidUrl(textContent)) {
            mjpg_url_input_layout.error = null
            mjpgUrl = textContent
            initLiveView()
            val prefs = getPreferences(Context.MODE_PRIVATE)
            with (prefs.edit()) {
                putString(getString(R.string.mjpg_url_key), mjpgUrl)
                commit()
            }
        } else {
            Log.e(javaClass.name, "mjpeg error - invalid url")
            mjpg_url_input_layout.error = getString(R.string.mjpg_url_input_error)
        }
    }

    fun togglePlayback(view: View) {
        if (camera_view.isStreaming) {
            camera_view.stopPlayback()
            if (view is FloatingActionButton) {
                view.setImageResource(R.drawable.ic_pause_play)
                val drawable = view.drawable;
                if (drawable is Animatable) {
                    drawable.start()
                }
            }
        } else {
            initLiveView()
            if (view is FloatingActionButton) {
                view.setImageResource(R.drawable.ic_play_pause)
                val drawable = view.drawable;
                if (drawable is Animatable) {
                    drawable.start()
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        initLiveView()
    }

    override fun onPause() {
        super.onPause()
        camera_view.stopPlayback()
    }
}
