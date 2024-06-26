package com.example.recorder

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.app.ActivityCompat
//import kotlinx.android.synthetic.main.activity_main.btnRecord
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import com.example.recorder.R
import com.example.recorder.databinding.ActivityMainBinding
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.room.Room
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.io.OutputStream


const val REQUEST_CODE =220
class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {
    private lateinit var amplitudes: ArrayList<Float>
    private var permission = arrayOf(android.Manifest.permission.RECORD_AUDIO)
    private var permissionGranted=false

    //add due to depreciated
    private lateinit var binding: ActivityMainBinding

    private lateinit var recorder: MediaRecorder
    private var dirPath = ""
    private var filename = ""
    private var isRecording=false
    private var isPaused = false

    //create the timer
    private lateinit var vibrator: Vibrator
    private lateinit var timer: Timer
    private var duration = ""

    //for bottom sheet
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    //val filenameInput = findViewById<EditText>(R.id.filenameInput)

    // Declare filenameInput as a class-level property
    private lateinit var filenameInput: EditText

    //Declare for database
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)

        //add due to depreciated
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        filenameInput = findViewById<EditText>(R.id.filenameInput)

        val btnCancel = findViewById<Button>(R.id.btnCancel)
        val btnConfirm = findViewById<Button>(R.id.btnConfirm)

        permissionGranted = ActivityCompat.checkSelfPermission(this,permission[0]) == PackageManager.PERMISSION_GRANTED
        if(!permissionGranted)
            ActivityCompat.requestPermissions(this,permission, REQUEST_CODE)

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            name = "audioRecords"
        ).build()

        //bottom Sheet visibility
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottomSheet))
        bottomSheetBehavior.peekHeight=0
        bottomSheetBehavior.state=BottomSheetBehavior.STATE_COLLAPSED

        //initial the timer
        timer = Timer(this)
        vibrator=getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        binding.btnRecord.setOnClickListener {
            when{
                isPaused -> resumeRecorder()
                isRecording -> pauseRecorder()
                else -> startRecording()
            }
            vibrator.vibrate(VibrationEffect.createOneShot(50,VibrationEffect.DEFAULT_AMPLITUDE))
        }

        binding.btnList.setOnClickListener {
            //Toast.makeText(this,"List Button",Toast.LENGTH_SHORT).show()
            startActivity(Intent(this,GalleryActivity::class.java))
        }

        //btn actions

        binding.btnDone.setOnClickListener {
            stopRecorder()
            //bottomsheet visible
            binding.bottomSheetBG.visibility=View.VISIBLE
            bottomSheetBehavior.state=BottomSheetBehavior.STATE_EXPANDED
            filenameInput.setText(filename)

        }

        btnCancel.setOnClickListener {
            File("$dirPath$filename.mp3").delete()
            dismiss()
        }

        btnConfirm.setOnClickListener{
            dismiss()
            save()
            Toast.makeText(this,"Audio Saved",Toast.LENGTH_SHORT).show()

        }

        binding.btnDelete.setOnClickListener {
            stopRecorder()
            File("$dirPath$filename.mp3").delete()
            Toast.makeText(this,"Audio Deleted",Toast.LENGTH_SHORT).show()
        }

        binding.bottomSheetBG.setOnClickListener{
            File("$dirPath$filename.mp3").delete()
            dismiss()
        }

        binding.btnDelete.isClickable=false

    }

    // Method to start AudioPlayerActivity with the necessary data
    /*private fun playRecording() {
        val intent = Intent(this, AudioPlayerActivity::class.java).apply {
            putExtra("filepath", "$dirPath$filename.mp3")  // Path to the audio file
            putExtra("filename", "$filename")  // Path to the filename
            putExtra("ampsPath", "$dirPath$filename.amplitudes")  // Path to the amplitude data file
        }
        startActivity(intent)
    }*/

    private fun save(){
        val newFilename = filenameInput.text.toString()
        if(newFilename != filename){
            var newFile = File("$dirPath$newFilename.mp3")
            File("$dirPath$filename.mp3").renameTo(newFile)
        }

        var filePath = "$dirPath$newFilename.mp3"
        var timestamp = Date().time
        var ampsPath = "$dirPath$newFilename"

        try{
            var fos = FileOutputStream(ampsPath)
            var out = ObjectOutputStream(fos)
            out.writeObject(amplitudes)
            fos.close()
            out.close()
        }catch (e :IOException){}

        var record = AudioRecord(newFilename, filePath, timestamp, duration, ampsPath)

        GlobalScope.launch {
            db.audioRecordDao().insert(record)
        }

        /*val newFilename = filenameInput.text.toString()
        val originalFilePath = "$dirPath$filename.mp3"
        val newFilePath = "$dirPath$newFilename.mp3"
        val ampsPath = "$dirPath$newFilename.amplitudes"

        // Rename the audio file if the filename has been changed
        if (newFilename != filename && newFilename.isNotEmpty()) {
            File(originalFilePath).renameTo(File(newFilePath))
        } else {
            filename = newFilename // Update the filename to reflect the user's input
        }

        // Save the amplitude data
        try {
            FileOutputStream(ampsPath).use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(amplitudes)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // Create and insert the record into the database
        var record = AudioRecord(newFilename, newFilePath, Date().time, duration, ampsPath)
        GlobalScope.launch {
            db.audioRecordDao().insert(record)
        }*/
    }

    private fun dismiss(){
        binding.bottomSheetBG.visibility=View.GONE
        hidekeyboard(filenameInput)

        Handler(Looper.getMainLooper()).postDelayed({
            bottomSheetBehavior.state=BottomSheetBehavior.STATE_COLLAPSED
        },100)
    }

    private fun hidekeyboard(view: View){
        val imm=getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken,0)
    }

    //permission for recording
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE)
            permissionGranted =grantResults[0]==PackageManager.PERMISSION_GRANTED
    }

    private fun pauseRecorder(){
        recorder.pause()
        isPaused = true
        binding.btnRecord.setImageResource(R.drawable.ic_record)
        timer.pause()
    }

    private fun resumeRecorder(){
        recorder.resume()
        isPaused = false
        binding.btnRecord.setImageResource(R.drawable.ic_pause)
        timer.start()
    }

    private fun startRecording(){
        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this, permission, REQUEST_CODE)
            return
        }

        recorder = MediaRecorder()
        dirPath = "${externalCacheDir?.absolutePath}/"

        var simpleDateFormat = SimpleDateFormat("yyyy.MM.DD_hh.mm.ss")
        var date = simpleDateFormat.format(Date())
        filename = "audio_record_$date"

        recorder.apply{
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$filename.mp3")

            try {
                prepare()
            }catch (e: IOException){}

            start()
        }

        binding.btnRecord.setImageResource(R.drawable.ic_pause)
        isRecording = true
        isPaused = false

        timer.start()

        binding.btnDelete.isClickable = true
        binding.btnDelete.setImageResource(R.drawable.ic_delete)

        binding.btnList.visibility = View.GONE
        binding.btnDone.visibility = View.VISIBLE



        /*// Prepare for a new recording
        dirPath = "${externalCacheDir?.absolutePath}/"
        filename = "audio_recorder_${SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(Date())}"
        amplitudes = ArrayList() // Initialize the amplitude list


        //start recording
        recorder = MediaRecorder()
        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$filename.mp3")
            try{
                prepare()
            }catch(e:IOException){}
            start()
        }

        binding.btnRecord.setImageResource(R.drawable.ic_pause)
        isRecording = true
        isPaused = false

        timer.start()

        binding.btnDelete.isClickable=true
        binding.btnDelete.setImageResource(R.drawable.ic_delete)

        binding.btnList.visibility=View.GONE
        binding.btnDone.visibility=View.VISIBLE*/

    }

    private fun stopRecorder(){
        timer.stop()

        recorder.apply {
            stop()
            release()
        }

        isPaused=false
        isRecording=false

        binding.btnList.visibility= View.VISIBLE
        binding.btnDone.visibility= View.GONE

        binding.btnDelete.isClickable=false
        binding.btnDelete.setImageResource(R.drawable.ic_delete_disabled)

        binding.btnRecord.setImageResource(R.drawable.ic_record)

        binding.tvTimer.text="00:00.00"

        // Clear the amplitude data and reset the waveform view
        amplitudes = binding.waveforView.clear()

        // Save the recording and amplitude data
        //save()
    }

    //handle recording completion for AudioPlayerActivity
    private fun onRecordingCompleted() {
        stopRecorder()
        //playRecording()  // Call this method to start playback
    }

    override fun onTimerTick(duration: String) {
        binding.tvTimer.text=timer.format()
        this.duration = duration.dropLast(3)
        binding.waveforView.addAmplitude(recorder.maxAmplitude.toFloat())
    }
}