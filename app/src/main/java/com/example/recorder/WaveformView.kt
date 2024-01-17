package com.example.recorder
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class WaveformView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var paint = Paint()
    private var amplitudes = ArrayList<Float>()
    private var spikes = ArrayList<RectF>()

    private var radius = 6f
    private var w = 9f
    private var d = 6f

    private var sw = 0f
    private var sh = 400f

    private var maxSpikes = 0

    init {
        paint.color = Color.rgb(244, 81, 30)

        sw = resources.displayMetrics.widthPixels.toFloat()

        maxSpikes = (sw / (w+d)).toInt()
    }


    fun addAmplitude(amp: Float){
        var norm = Math.min(amp.toInt()/7, 400).toFloat()
        amplitudes.add(norm)


        spikes.clear()
        var amps = amplitudes.takeLast(maxSpikes)
        for(i in amps.indices){

            var left = i*(w+d)
            var top = sh/2 - amps[i]/2
            var right = left + w
            var bottom = top + amps[i]
            spikes.add(RectF(left, top, right, bottom))
        }


        invalidate()
    }

    fun setAmplitudes(amps: List<Float>) {
        amplitudes.clear()
        amplitudes.addAll(amps)
        regenerateSpikes()
        invalidate()
    }

    private fun regenerateSpikes() {
        spikes.clear()
        val ampsToShow = amplitudes.takeLast(maxSpikes)
        for (i in ampsToShow.indices) {
            val left = i * (w + d)
            val top = sh / 2 - ampsToShow[i] / 2
            val right = left + w
            val bottom = top + ampsToShow[i]
            spikes.add(RectF(left, top, right, bottom))
        }
    }

    fun clear() : ArrayList<Float>{
        var amps = amplitudes.clone() as ArrayList<Float>
        amplitudes.clear()
        spikes.clear()
        invalidate()

        return amps
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        spikes.forEach {
            canvas?.drawRoundRect(it, radius, radius, paint)
        }
    }
}