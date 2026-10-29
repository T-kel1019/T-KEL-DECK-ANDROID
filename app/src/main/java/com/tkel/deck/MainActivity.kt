package com.tkel.deck

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

data class DeckTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val uri: Uri
)

class MainActivity : Activity() {

    private val blue = Color.rgb(0, 102, 255)
    private val dark = Color.rgb(15, 23, 42)
    private val tracks = mutableListOf<DeckTrack>()

    private lateinit var songContainer: LinearLayout
    private lateinit var statusText: TextView
    private var mediaPlayer: MediaPlayer? = null
    private var currentTrackId: Long? = null

    private val audioPermission: String
        get() = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = dark
        window.navigationBarColor = dark

        buildScreen()

        if (hasAudioPermission()) {
            scanMusic()
        } else {
            requestPermissions(arrayOf(audioPermission), 100)
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245, 248, 255))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 26)
            setBackgroundColor(dark)
        }

        val brand = TextView(this).apply {
            text = "T-KEL DECK"
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }

        val slogan = TextView(this).apply {
            text = "Stay Real, Stay kelz."
            textSize = 15f
            setTextColor(Color.LTGRAY)
            setPadding(0, 6, 0, 0)
        }

        val subtitle = TextView(this).apply {
            text = "YOUR MUSIC. YOUR DECK."
            textSize = 12f
            setTextColor(Color.rgb(120, 175, 255))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 18, 0, 0)
        }

        header.addView(brand)
        header.addView(slogan)
        header.addView(subtitle)

        statusText = TextView(this).apply {
            text = "Preparing your music library..."
            textSize = 15f
            setTextColor(dark)
            setPadding(20, 20, 20, 12)
        }

        val scanButton = Button(this).apply {
            text = "SCAN MY MUSIC"
            setTextColor(Color.WHITE)
            setBackgroundColor(blue)
            setOnClickListener {
                if (hasAudioPermission()) {
                    scanMusic()
                } else {
                    requestPermissions(arrayOf(audioPermission), 100)
                }
            }
        }

        val sectionTitle = TextView(this).apply {
            text = "MY MUSIC LIBRARY"
            textSize = 18f
            setTextColor(dark)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(20, 18, 20, 10)
        }

        songContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 0, 14, 20)
        }

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            addView(songContainer)
        }

        root.addView(header)
        root.addView(statusText)

        root.addView(
            scanButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                54
            ).apply {
                setMargins(20, 0, 20, 8)
            }
        )

        root.addView(sectionTitle)
        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun hasAudioPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            checkSelfPermission(audioPermission) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
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

        if (requestCode == 100) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                scanMusic()
            } else {
                statusText.text =
                    "Music access is needed to scan audio stored on your phone."
                Toast.makeText(
                    this,
                    "Please allow music access to continue.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun scanMusic() {
        tracks.clear()
        songContainer.removeAllViews()

        statusText.text = "Scanning music stored on your phone..."

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor: Cursor? = contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idColumn =
                    it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn =
                    it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn =
                    it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationColumn =
                    it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val title = it.getString(titleColumn) ?: "Unknown title"
                    val artist = it.getString(artistColumn) ?: "Unknown artist"
                    val duration = it.getLong(durationColumn)

                    val uri = ContentUris.withAppendedId(collection, id)

                    tracks.add(
                        DeckTrack(
                            id = id,
                            title = title,
                            artist = artist,
                            duration = duration,
                            uri = uri
                        )
                    )
                }
            }

            displayTracks()
        } catch (e: Exception) {
            statusText.text = "Unable to scan music. Please try again."
            Toast.makeText(
                this,
                "Music scan failed: ${e.message ?: "Unknown error"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun displayTracks() {
        songContainer.removeAllViews()

        if (tracks.isEmpty()) {
            statusText.text =
                "No music found. Save audio files on your phone and scan again."

            val emptyText = TextView(this).apply {
                text = "Your music library is empty.\n\n" +
                    "Tap SCAN MY MUSIC to search again."
                textSize = 16f
                setTextColor(Color.DKGRAY)
                gravity = Gravity.CENTER
                setPadding(24, 48, 24, 48)
            }

            songContainer.addView(emptyText)
            return
        }

        statusText.text = "${tracks.size} song(s) found on your phone."

        tracks.forEach { track ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 14, 16, 14)
                setBackgroundColor(Color.WHITE)
            }

            val title = TextView(this).apply {
                text = track.title
                textSize = 17f
                setTextColor(dark)
                typeface = Typeface.DEFAULT_BOLD
            }

            val artist = TextView(this).apply {
                text = "${track.artist} • ${formatDuration(track.duration)}"
                textSize = 13f
                setTextColor(Color.DKGRAY)
                setPadding(0, 5, 0, 10)
            }

            val playButton = Button(this).apply {
                text = "▶  PLAY"
                setTextColor(Color.WHITE)
                setBackgroundColor(blue)

                setOnClickListener {
                    playTrack(track)
                }
            }

            card.addView(title)
            card.addView(artist)
            card.addView(playButton)

            val cardParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12)
            }

            songContainer.addView(card, cardParams)
        }
    }

    private fun playTrack(track: DeckTrack) {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            currentTrackId = track.id

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@MainActivity, track.uri)

                setOnPreparedListener {
                    it.start()
                    Toast.makeText(
                        this@MainActivity,
                        "Now playing: ${track.title}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                setOnCompletionListener {
                    it.release()
                    if (mediaPlayer === it) {
                        mediaPlayer = null
                        currentTrackId = null
                    }
                }

                setOnErrorListener { player, _, _ ->
                    player.release()
                    if (mediaPlayer === player) {
                        mediaPlayer = null
                        currentTrackId = null
                    }

                    Toast.makeText(
                        this@MainActivity,
                        "Unable to play this track.",
                        Toast.LENGTH_SHORT
                    ).show()

                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            mediaPlayer?.release()
            mediaPlayer = null
            currentTrackId = null

            Toast.makeText(
                this,
                "Playback failed. Please try another track.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return String.format(
            Locale.getDefault(),
            "%d:%02d",
            minutes,
            seconds
        )
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}
