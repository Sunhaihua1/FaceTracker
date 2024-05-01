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
import com.example.bluetooth_test.databinding.FragmentAllBinding
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
class AllFragment : Fragment() {
    val arr_region = arrayOf(arrayOf(1, 2, 3, 4, 5),arrayOf(4, 5, 6, 7), arrayOf(8, 9),arrayOf(6, 7))

    private lateinit var pageViewModel: PageViewModel
    private var _binding: FragmentAllBinding? = null
    private var Fragment_id: Int? = null;

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
        val values = ArrayList<ArrayList<Entry>>(3).apply {
            for (i in 1..3) {
                add(ArrayList<Entry>())
            }
        }
//        Log.e("TAG", "setData: " + values.size, )
        val Data: Queue<Sensor_data> = LinkedList(ConnectedThread.data_sensor[0])
        var idx = 0;
        while (Data.size > 0){
            val tmp = Data.poll();
            values[0].add(Entry(idx.toFloat(), tmp.x))
            values[1].add(Entry(idx.toFloat(), tmp.y))
            values[2].add(Entry(idx.toFloat(), tmp.z))
            idx ++
        }
//        Log.e("TAG", "setData: " + Data.size,)
//        Log.e("TAG", "setData: " + Data2.size,)
        val dataSets = ArrayList<ILineDataSet>()

        for (i in 0..2) {
            var set1: LineDataSet?=null;
            if (chart.data != null && chart.data.dataSetCount > i) {
                set1 = chart.data.getDataSetByIndex(i) as LineDataSet
                set1!!.values = values[i]
                set1!!.notifyDataSetChanged()
                chart.data.notifyDataChanged()
                chart.notifyDataSetChanged()
            } else {

                // create a dataset and give it a type
                set1 = LineDataSet(values[i], (('x'.toInt() + i).toChar()).toString(),)
                set1!!.setDrawIcons(false)
                // draw dashed line
//            set1.enableDashedLine(10f, 5f, 0f)
                if (i == 0) {
                    set1!!.color = Color.RED
                }
                else if (i == 1) {
                    set1.color = Color.BLUE
                }
                else if (i == 2) {
                    set1.color = Color.CYAN
                }

                // black lines and points
                set1.setDrawValues(false)
                // line thickness and point size
                set1!!.lineWidth = 1f
                set1!!.circleRadius = 1f

                // draw points as solid circles
                set1!!.setDrawCircleHole(false)
                set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                // customize legend entry
                set1!!.formLineWidth = 1f
//            set1.formLineDashEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
                set1!!.formSize = 15f
                // text size of values
                set1!!.valueTextSize = 9f
                set1!!.setValueFormatter(DefaultValueFormatter(2));//座標點數字的小數位數1位

                // draw selection line as dashed
                set1!!.enableDashedHighlightLine(10f, 5f, 0f)
                dataSets.add(set1!!) // add the data sets

            }

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

        _binding = FragmentAllBinding.inflate(inflater,container, false)
        val root = binding.root;
        var textView: TextView = binding.sectionLabel;
//        textView.setText("Hello from " + ConnectedThread.data);
//        pageViewModel.text.observe(viewLifecycleOwner, Observer {
//            textView.text = it
//        })

        for (i in 1..9) {
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
            mchart.isLogEnabled = false
        }
        for ((i, plotchart) in arrList.withIndex()) {
            plotchart.apply {

                setTouchEnabled(false);
                description.isEnabled = true;
                xAxis.isEnabled = false;
                axisLeft.isEnabled = true;
                axisRight.isEnabled =false;

            }
            setData(plotchart);
        }
        Thread {
            while (true){
                try{
                    Thread.sleep(50)
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
                    for(i in  0 until 9) {
                        setData(arrList[i])

                    }
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
        fun newInstance(sectionNumber: Int): AllFragment {
            return AllFragment().apply {
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