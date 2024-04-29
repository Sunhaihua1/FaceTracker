package com.example.bluetooth_test.ui.main

import android.graphics.Color
import android.graphics.DashPathEffect
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.bluetooth_test.BluetoothActivity
import com.example.bluetooth_test.ConnectedThread
import com.example.bluetooth_test.R
import com.example.bluetooth_test.Sensor_data
import com.example.bluetooth_test.databinding.FragmentPlotactivityBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.text.DecimalFormat
import java.util.LinkedList
import java.util.Queue


/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {
    val arr_region = arrayOf(arrayOf(1, 2, 3, 4, 5),arrayOf(4, 5, 6, 7), arrayOf(8, 9),arrayOf(6, 7))

    private lateinit var pageViewModel: PageViewModel
    private var _binding: FragmentPlotactivityBinding? = null
    private var Fragment_id: Int? = null;
    var set1: LineDataSet?=null;

    val arrList : MutableList<LineChart> = mutableListOf<LineChart>()
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
     var messageHandler:MessageHandler? = MessageHandler();
    private var _Test: EditText? = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)

        }
        var messageHandler = MessageHandler();
        Fragment_id = arguments?.getInt(ARG_SECTION_NUMBER);
    }

    public fun setData(chart: LineChart) {
        val values = ArrayList<Entry>();
        val Data: Queue<Sensor_data> = LinkedList(ConnectedThread.data_sensor)
        var idx = 0;

        while (Data.size > 0){

            Data.poll()
                ?.let { Entry(idx ++.toFloat(), it.x
                    /*, resources.getDrawable(R.drawable.star)*/) }
                ?.let { values.add(it) }
        }
//        Log.e("TAG", "setData: " + Data.size,)
//        Log.e("TAG", "setData: " + Data2.size,)

        if (chart.data != null && chart.data.dataSetCount > 0) {
            set1 = chart.data.getDataSetByIndex(0) as LineDataSet
            set1!!.values = values
            set1!!.notifyDataSetChanged()
            chart.data.notifyDataChanged()
            chart.notifyDataSetChanged()
        } else {
            // create a dataset and give it a type
            set1 = LineDataSet(values, "x",)
            set1!!.setDrawIcons(false)

            // draw dashed line
//            set1.enableDashedLine(10f, 5f, 0f)

            // black lines and points
            set1!!.color = Color.RED
            set1!!.setCircleColor(Color.BLACK)

            // line thickness and point size
            set1!!.lineWidth = 1f
            set1!!.circleRadius = 1f

            // draw points as solid circles
            set1!!.setDrawCircleHole(false)

            // customize legend entry
            set1!!.formLineWidth = 1f
//            set1.formLineDashEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
            set1!!.formSize = 15f

            // text size of values
            set1!!.valueTextSize = 9f
            set1!!.setValueFormatter(DefaultValueFormatter(2));//座標點數字的小數位數1位

            // draw selection line as dashed
            set1!!.enableDashedHighlightLine(10f, 5f, 0f)
            // set the filled area
//            set1.setDrawFilled(true)
//            set1.fillFormatter = IFillFormatter { dataSet, dataProvider -> chart.axisLeft.axisMinimum }

            // set color of filled area
            /*if (Utils.getSDKInt() >= 18) {
                // drawables only supported on api level 18 and above
                val drawable = ContextCompat.getDrawable(this, R.drawable.fade_red)
                set1.fillDrawable = drawable
            } else {
                set1.fillColor = Color.BLACK
            }*/

            val dataSets = ArrayList<ILineDataSet>()
            dataSets.add(set1!!) // add the data sets

            // create a data object with the data sets
            val data = LineData(dataSets)
            chart.data = data

            data.notifyDataChanged()
            // set data
            chart.notifyDataSetChanged()

            chart.invalidate()

        }
    }




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentPlotactivityBinding.inflate(inflater, container, false)
        val root = binding.root;
        var textView: TextView = binding.sectionLabel;
//        textView.setText("Hello from " + ConnectedThread.data);
//        pageViewModel.text.observe(viewLifecycleOwner, Observer {
//            textView.text = it
//        })

        for (i in arr_region[Fragment_id!!]) {
            var mchart = LineChart(context);
            mchart.id = View.generateViewId();
            var layout = binding.linearPlot
            layout.addView(mchart)
            val layoutParams = mchart.layoutParams
            layoutParams.height = 400
            layoutParams.width = LayoutParams.MATCH_PARENT;
            arrList.add(mchart)
            var description = Description()
            description.text = "Sensor" + i.toString()
            mchart.description = description
            mchart.isLogEnabled = true
        }
        for ((i, plotchart) in arrList.withIndex()) {
            plotchart.apply {

                setTouchEnabled(false);
                description.isEnabled = true;
                xAxis.isEnabled = false;
                xAxis.isEnabled = false;
                axisLeft.isEnabled = false;
                axisRight.isEnabled =false;
            }
            setData(plotchart);
        }
        Thread {
            while (true){
                try{
                    Thread.sleep(1000)
                    var message = Message()
                    message.obj = ConnectedThread.data;
                    messageHandler?.sendMessage(message);

                } catch (t: Throwable) {
                    // 发生异常，通过handleCoroutineExceptionImpl方法处理
                    break
                }
            }
        }.start();

        return root
    }
    inner class MessageHandler: Handler(){
        override fun handleMessage(msg: Message) {
            try{

                super.handleMessage(msg)

                activity?.runOnUiThread {
                    var data = arrList[0].data;
                    var set = data.getDataSetByIndex(0)
//                    set.clear()
                    data.addEntry(Entry(set.entryCount.toFloat(),3.3f),0)
                    Log.e("TAG", set.entryCount.toString())

                    data.notifyDataChanged()
                    arrList[0].notifyDataSetChanged()
                    arrList[0].invalidate()

                    binding.sectionLabel.setText(ConnectedThread.data.toString())

                }
            } catch (t: Throwable) {
                return
            }

        }
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int): PlaceholderFragment {
            return PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}