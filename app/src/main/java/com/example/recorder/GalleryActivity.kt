package com.example.recorder

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.textservice.TextInfo
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.recorder.databinding.ActivityGalleryBinding
import com.example.recorder.databinding.ActivityMainBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GalleryActivity : AppCompatActivity(), OnItemClickListener{
    private lateinit var records: ArrayList<AudioRecord>
    private lateinit var mAdapter: Adapter
    private lateinit var db: AppDatabase

    private var allChecked=false

    private lateinit var toolbar: MaterialToolbar
    private lateinit var editBar: View
    private lateinit var btnClose:Button
    private lateinit var btnSelectAll: Button

    private lateinit var searchInput:TextInputEditText
    //bottomSheet
    private lateinit var bottomSheet: LinearLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private lateinit var btnRename:ImageButton
    private lateinit var btnDelete:ImageButton
    private lateinit var tvRename:TextView
    private lateinit var tvDelete:TextView

    private lateinit var binding: ActivityGalleryBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_gallery)

        //add due to depreciated
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        toolbar=findViewById(R.id.toolBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        btnRename=findViewById(R.id.btnEdit)
        btnDelete=findViewById(R.id.btnDelete)
        tvRename=findViewById(R.id.tvEdit)
        tvDelete=findViewById(R.id.tvDelete)

        editBar=findViewById(R.id.editBar)
        btnClose=findViewById(R.id.btnClose)
        btnSelectAll=findViewById(R.id.btnSelectAll)

        bottomSheet=findViewById(R.id.bottomSheet)
        bottomSheetBehavior= BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

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

        searchInput=findViewById(R.id.search_input)
        searchInput.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                var query =p0.toString()
                searchDatabase(query)
            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })

        btnClose.setOnClickListener {
            leaveEditMode()
        }

        btnSelectAll.setOnClickListener {
            allChecked=!allChecked
            records.map{it.isChecked=allChecked}
            mAdapter.notifyDataSetChanged()

            if(allChecked){
                disableRename()
                enableDelete()
            }else{
                disableDelete()
                disableRename()
            }
        }

        btnDelete.setOnClickListener {
            val builder=AlertDialog.Builder(this)
            builder.setTitle("Delete Record?")
            val numRecords=records.count { it.isChecked }
            builder.setMessage("Are you sure to delete $numRecords record(s)?")
            builder.setPositiveButton("Delete"){_,_ ->
                val toDelete= records.filter { it.isChecked }.toTypedArray()
                GlobalScope.launch {
                    db.audioRecordDao().delete(toDelete)
                    runOnUiThread{
                        records.removeAll(toDelete)
                        mAdapter.notifyDataSetChanged()
                        leaveEditMode()
                    }
                }
            }
            builder.setNegativeButton("Cancel"){_,_ ->
                //nothing
            }

            val dialog=builder.create()
            dialog.show()
        }

        btnRename.setOnClickListener {
            val builder=AlertDialog.Builder(this)
            val dialogView=this.layoutInflater.inflate(R.layout.rename_layout, null)
            builder.setView(dialogView)
            val dialog =builder.create()
            val record=records.filter { it.isChecked }.get(0)
            val textInput= dialogView.findViewById<TextInputEditText>(R.id.filenameInput)
            textInput.setText(record.filename)

            dialogView.findViewById<Button>(R.id.btnSave).setOnClickListener{
                val input=textInput.text.toString()
                if(input.isEmpty()){
                    Toast.makeText(this,"Name is required", Toast.LENGTH_LONG).show()
                }else{
                    record.filename=input
                    GlobalScope.launch {
                        db.audioRecordDao().update(record)
                        runOnUiThread{
                            mAdapter.notifyItemChanged(records.indexOf(record))
                            dialog.dismiss()
                            leaveEditMode()
                        }
                    }
                }
            }

            dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener{
                dialog.dismiss()
            }


            dialog.show()
        }

    }

    private fun leaveEditMode(){
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        editBar.visibility=View.GONE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        records.map{it.isChecked=false}
        mAdapter.setEditMode(false)
    }

    private fun disableRename(){
        btnRename.isClickable=false
        btnRename.backgroundTintList=ResourcesCompat.getColorStateList(resources,R.color.greyDarkDisabled,theme)
        tvRename.setTextColor(ResourcesCompat.getColorStateList(resources,R.color.greyDarkDisabled,theme))
    }

    private fun disableDelete(){
        btnDelete.isClickable=false
        btnDelete.backgroundTintList=ResourcesCompat.getColorStateList(resources,R.color.greyDarkDisabled,theme)
        tvDelete.setTextColor(ResourcesCompat.getColorStateList(resources,R.color.greyDarkDisabled,theme))
    }

    private fun enableRename(){
        btnRename.isClickable=true
        btnRename.backgroundTintList=ResourcesCompat.getColorStateList(resources,R.color.greyDark,theme)
        tvRename.setTextColor(ResourcesCompat.getColorStateList(resources,R.color.greyDarkDisabled,theme))
    }

    private fun enableDelete(){
        btnDelete.isClickable=true
        btnDelete.backgroundTintList=ResourcesCompat.getColorStateList(resources,R.color.greyDark,theme)
        tvDelete.setTextColor(ResourcesCompat.getColorStateList(resources,R.color.greyDarkDisabled,theme))
    }

    private fun searchDatabase(query: String) {
        GlobalScope.launch {
            records.clear()
            var queryResult=db.audioRecordDao().searchDatabase("%$query%")
            records.addAll(queryResult)

            runOnUiThread{
                mAdapter.notifyDataSetChanged()
            }
        }
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
        // Handle regular click
        var audioRecord= records[position]
        if(mAdapter.isEditMode()){
            records[position].isChecked=!records[position].isChecked
            mAdapter.notifyItemChanged(position)

            var numSelected= records.count{it.isChecked}
            when(numSelected){
                0->{
                    disableDelete()
                    disableRename()
                }
                1->{
                    enableDelete()
                    enableRename()
                }
                else->{
                    enableDelete()
                    disableRename()
                }
            }


        }else{
            var intent = Intent(this@GalleryActivity,AudioPlayerActivity::class.java)

            //match the filepath and name to the activity
            intent.putExtra("filepath",audioRecord.filePath)
            intent.putExtra("filename",audioRecord.filename)
            //intent.putExtra("ampsPath", audioRecord.ampsPath)
            startActivity(intent)
        }
    }

    override fun onItemLongClickListener(position: Int) {
        // Handle long click
        mAdapter.setEditMode(true)
        records[position].isChecked=!records[position].isChecked
        mAdapter.notifyItemChanged(position)

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        if(mAdapter.isEditMode() && editBar.visibility==View.GONE){
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.setDisplayShowHomeEnabled(false)
            editBar.visibility=View.VISIBLE

            enableDelete()
            enableRename()
        }
    }
}