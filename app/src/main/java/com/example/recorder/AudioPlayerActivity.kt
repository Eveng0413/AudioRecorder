package com.example.recorder

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.chip.Chip

class AudioPlayerActivity : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var btnPlay: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnBackward: ImageButton
    private lateinit var speedChip: Chip
    private lateinit var seekBar: SeekBar

    private lateinit var runnable: Runnable
    private lateinit var handler: Handler
    private var delay = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_player)

        var filePath = intent.getStringExtra("filepath")
        var fileName = intent.getStringExtra("filename")

        //set a media player that get the info for the wanted filePath
        mediaPlayer = MediaPlayer()
        mediaPlayer.apply {
            setDataSource(filePath)
            prepare()
        }

        //set the seekBar running along as the audio

        btnBackward = findViewById(R.id.btnBackward)
        btnForward = findViewById(R.id.btnForward)
        btnPlay = findViewById(R.id.btnPlay)
        speedChip=findViewById(R.id.chip)
        seekBar=findViewById(R.id.seekBar)

        handler= Handler(Looper.getMainLooper())
        runnable= Runnable {
            seekBar.progress = mediaPlayer.currentPosition
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
    }

    //when clicked, change status between play and pause
    private fun playPausePlayer(){
        if(!mediaPlayer.isPlaying){
            mediaPlayer.start()
            btnPlay.background = ResourcesCompat.getDrawable(resources,R.drawable.ic_pause_circle,theme)
            handler.postDelayed(runnable,0)
        }else{
            mediaPlayer.pause()
            btnPlay.background = ResourcesCompat.getDrawable(resources,R.drawable.ic_play_circle_,theme)
            handler.removeCallbacks(runnable)
        }

    }
}