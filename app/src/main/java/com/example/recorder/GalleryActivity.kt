package com.example.recorder

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.recorder.databinding.ActivityGalleryBinding
import com.example.recorder.databinding.ActivityMainBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GalleryActivity : AppCompatActivity(), OnItemClickListener{
    private lateinit var records: ArrayList<AudioRecord>
    private lateinit var mAdapter: Adapter
    private lateinit var db: AppDatabase

    private lateinit var binding: ActivityGalleryBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_gallery)

        //add due to depreciated
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        records = ArrayList()
        db= Room.databaseBuilder(
            this,
            AppDatabase::class.java,
        "audioRecords"
        ).build()
        mAdapter = Adapter(records,this)

        binding.recyclerview.apply {
            adapter=mAdapter
            layoutManager = LinearLayoutManager(context)
        }
        fetchAll()
    }

    private fun fetchAll(){
        GlobalScope.launch {
            records.clear()
            var queryResult=db.audioRecordDao().getAll()
            records.addAll(queryResult)

            mAdapter.notifyDataSetChanged()
        }
    }

    override fun onItemClickListener(position: Int) {
        var audioRecord= records[position]
        var intent = Intent(this,AudioPlayerActivity::class.java)

        //match the filepath and name to the activity
        intent.putExtra("filepath",audioRecord.filePath)
        intent.putExtra("filename",audioRecord.filename)
        startActivity(intent)
    }

    override fun onItemLongClickListener(position: Int) {
        TODO("Not yet implemented")
        Toast.makeText(this,"long click",Toast.LENGTH_SHORT).show()
    }
}