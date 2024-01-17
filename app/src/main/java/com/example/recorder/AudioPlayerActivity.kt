package com.example.recorder

import android.media.MediaPlayer
import android.media.PlaybackParams
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.Chip
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.ObjectInputStream
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Objects

class AudioPlayerActivity : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var toolBar:MaterialToolbar
    private lateinit var tvFileName:TextView

    private lateinit var btnPlay: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnBackward: ImageButton
    private lateinit var speedChip: Chip
    private lateinit var seekBar: SeekBar

    private lateinit var tvTrackProgress:TextView
    private lateinit var tvTrackDuration:TextView

    private lateinit var recordedWaveformView:WaveformView

    private lateinit var runnable: Runnable
    private lateinit var handler: Handler
    private var delay = 1000L
    private var jumpValue = 1000
    private var playbackspeed = 1.0f

    private lateinit var ampsFilePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_player)


        // Retrieve the paths from the intent
        var filePath = intent.getStringExtra("filepath")
        var fileName = intent.getStringExtra("filename")
        var ampsPath = intent.getStringExtra("ampsPath")


        toolBar = findViewById(R.id.toolBar)
        tvFileName = findViewById(R.id.tvFilename)


        tvTrackProgress = findViewById(R.id.tvTrackProgress)
        tvTrackDuration = findViewById(R.id.tvTrackDuration)


        //add the back button inside toolBar
        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolBar.setNavigationOnClickListener {
            onBackPressed()
        }

        tvFileName.text=fileName


        //set a media player that get the info for the wanted filePath
        mediaPlayer = MediaPlayer()
        mediaPlayer.apply {
            setDataSource(filePath)
            prepare()
            //start()
        }

        //set the progress and duration for media player under seek bar
        tvTrackDuration.text =dateFormat(mediaPlayer.duration)



        //set the seekBar running along as the audio

        btnBackward = findViewById(R.id.btnBackward)
        btnForward = findViewById(R.id.btnForward)
        btnPlay = findViewById(R.id.btnPlay)
        speedChip=findViewById(R.id.chip)
        seekBar=findViewById(R.id.seekBar)
        recordedWaveformView = findViewById<WaveformView>(R.id.recordedWaveformView)


        handler= Handler(Looper.getMainLooper())
        runnable= Runnable {
            val currentPosition = mediaPlayer.currentPosition
            seekBar.progress = currentPosition
            tvTrackProgress.text = dateFormat(currentPosition)

            // Update the waveform view
            // Calculate the playback position as a ratio
            //val progressRatio = currentPosition.toFloat() / mediaPlayer.duration
            //recordedWaveformView.setPlaybackPosition(progressRatio)

            handler.postDelayed(runnable,delay)
        }

        btnPlay.setOnClickListener {
            playPausePlayer()
        }

        playPausePlayer()
        seekBar.max=mediaPlayer.duration

        //set the player back to play when finished
        mediaPlayer.setOnCompletionListener{
            btnPlay.background = ResourcesCompat.getDrawable(resources,R.drawable.ic_play_circle_,theme)
            handler.removeCallbacks(runnable)
        }
        btnForward.setOnClickListener {
            mediaPlayer.seekTo(mediaPlayer.currentPosition + jumpValue)
        }
        btnForward.setOnClickListener {
            mediaPlayer.seekTo(mediaPlayer.currentPosition + jumpValue)
            seekBar.progress += jumpValue
        }
        btnBackward.setOnClickListener {
            mediaPlayer.seekTo(mediaPlayer.currentPosition - jumpValue)
            seekBar.progress += jumpValue
        }

        speedChip.setOnClickListener{
            if(playbackspeed != 2f){
                playbackspeed += 0.5f
            }else{
                playbackspeed = 0.5f
            }
            mediaPlayer.playbackParams = PlaybackParams().setSpeed(playbackspeed)
            speedChip.text ="x $playbackspeed"
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if(p2){
                    mediaPlayer.seekTo(p1)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }

        })
    }

    private fun loadWaveform(ampsPath: String?){

        if (ampsPath != null) {
            val amplitudes = readAmplitudesFromFile(ampsPath)
            // Use the amplitudes as needed
            recordedWaveformView.setAmplitudes(amplitudes)

        } else {
            Log.e("AudioPlayerActivity", "ampsPath is null")
        }
    }

    private fun readAmplitudesFromFile(filePath: String): List<Float> {
        // Read the file at filePath, parse it to get a List<Float>
        val amplitudes = mutableListOf<Float>()
        try {
            File(filePath).forEachLine { line ->
                amplitudes.add(line.toFloatOrNull() ?: 0f) // Convert line to Float, default to 0 if conversion fails
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return amplitudes
    }


    //when clicked, change status between play and pause
    private fun playPausePlayer(){
        if(!mediaPlayer.isPlaying){
            mediaPlayer.start()
            btnPlay.background = ResourcesCompat.getDrawable(resources,R.drawable.ic_pause_circle,theme)
            handler.postDelayed(runnable,delay)
        }else{
            mediaPlayer.pause()
            btnPlay.background = ResourcesCompat.getDrawable(resources,R.drawable.ic_play_circle_,theme)
            handler.removeCallbacks(runnable)
        }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        mediaPlayer.stop()
        mediaPlayer.release()
        handler.removeCallbacks(runnable)
    }

    private fun dateFormat(duration: Int):String{
        var d = duration/1000
        var s = d%60
        var m = (d/60%60)
        var h = ((d-m*60)/3600).toInt()

        val f: NumberFormat = DecimalFormat("00") // Format minutes with two digits

        var str = "${f.format(m)}:${f.format(s.toDouble())}"

        //if there is the hour
        if(h>0)
            str= "$h:$str"
        return str
    }
}