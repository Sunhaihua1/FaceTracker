package com.example.bluetooth_test.ui.plot

import android.graphics.Color
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
import androidx.fragment.app.Fragment
import com.example.bluetooth_test.ConnectedThread
import com.example.bluetooth_test.Sensor_data
import com.example.bluetooth_test.databinding.FragmentPlotactivityBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.util.LinkedList
import java.util.Queue
import java.util.Timer
import java.util.TimerTask
import kotlin.math.max


/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {
    val arr_region = arrayOf(arrayOf(6, 5, 4, 3),arrayOf(4,3, 2, 1), arrayOf(8,7),arrayOf(2,1))
    val state: Array<String> = arrayOf("正常","轻度","中度","重度")
    val myArray = arrayOf("嘴唇右", "嘴唇左", "脸颊右","脸颊左","眉毛右","眉毛左","下颚右","下颚左")
    private var _binding: FragmentPlotactivityBinding? = null
    private var Fragment_id: Int? = null;
    private var connected_thread = ConnectedThread.getInstance();

    val arrList : MutableList<LineChart> = mutableListOf<LineChart>()
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    var messageHandler:MessageHandler? = MessageHandler();
    private var _Test: EditText? = null;
    var timer = Timer();
    var timerTask: TimerTask?=null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        messageHandler = MessageHandler();
        Fragment_id = arguments?.getInt(ARG_SECTION_NUMBER);

    }

//


    @Deprecated("Deprecated in Java")
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (Fragment_id == null) return;
        if (isVisibleToUser) {
            timerTask?.cancel();
            timerTask = object : TimerTask() {
                //创建定时触发后要执行的逻辑任务
                override fun run() {
                    val message = Message()
                    var status = 0;
                    for (idx in arr_region[Fragment_id!!]) {
                        status = max(status,connected_thread.cur_state[idx - 1])
                    }
                    message.obj = status;
                    messageHandler?.sendMessage(message);
//                    System.gc()
                }
            }
            timer!!.schedule(timerTask,0, 150)

        }
        else timerTask?.cancel();
        Log.e("TAG", "onVisible: " + Fragment_id)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentPlotactivityBinding.inflate(inflater, container, false)
        val root = binding.root;
        arrList.clear();
        var textView: TextView = binding.sectionLabel;
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
                description.text = myArray[i-1] + i.toString()
                mchart.description = description
                mchart.isLogEnabled = false
            }
            for ((i, plotchart) in arrList.withIndex()) {
                plotchart.apply {

                    setTouchEnabled(false);
                    description.isEnabled = true;
                    xAxis.isEnabled = false;
                    xAxis.isEnabled = false;
                    axisLeft.isEnabled = true;
                    axisRight.isEnabled =false;
                }
            }
        timerTask?.cancel();
        timerTask = object : TimerTask() {
            //创建定时触发后要执行的逻辑任务
            override fun run() {

                val message = Message()
                var status = 0;
                for (idx in arr_region[Fragment_id!!]) {
                    status = max(status,connected_thread.cur_state[idx - 1])
                }
                message.obj = status;
                messageHandler?.sendMessage(message);
//                System.gc()
            }
        }
        timer!!.schedule(timerTask,0, 150)

        return root
    }

    inner class MessageHandler: Handler(){
        override fun handleMessage(msg: Message) {
            try{
                super.handleMessage(msg)
                activity?.runOnUiThread {
                    setData()
                    if(binding.switch1.isChecked) {
                        binding.sectionLabel.text = state[msg.obj as Int]
                    }
                    else {
                        binding.sectionLabel.text = "请开启评分"
                    }
                }
            } catch (t: Throwable) {
                return
            }

        }
    }
    public fun setData() {
        if(_binding == null) return;
        for (i in 0 until arrList.size) {
            val values = ArrayList<ArrayList<Entry>>(3).apply {
                for (t in 1..3) {
                    add(ArrayList<Entry>())
                }
            }
            val chart = arrList[i]
            val Data: Queue<Sensor_data> = LinkedList(ConnectedThread.data_sensor[arr_region[Fragment_id!!][i] - 1])
            var idx = 0;
            chart.clear()
            while (Data.size > 0){
                val tmp = Data.poll();
                values[0].add(Entry(idx.toFloat(), tmp.x))
                values[1].add(Entry(idx.toFloat(), tmp.y))
                values[2].add(Entry(idx.toFloat(), tmp.z))
                idx ++
            }
            val dataSets = ArrayList<ILineDataSet>()

            for (j in 0..2) {
                var set1: LineDataSet?=null;
                if (chart.data != null && chart.data.dataSetCount > j) {
                    set1 = chart.data.getDataSetByIndex(j) as LineDataSet
                    set1.values = values[j]
                    set1.notifyDataSetChanged()
                    chart.data.notifyDataChanged()
                    chart.notifyDataSetChanged()
                } else {

                    // create a dataset and give it a type
                    set1 = LineDataSet(values[j], (('x'.toInt() + j).toChar()).toString())
                    set1.setDrawIcons(false)
                    // draw dashed line
//            set1.enableDashedLine(10f, 5f, 0f)
                    if (j == 0) {
                        set1!!.color = Color.RED
                    }
                    else if (j == 1) {
                        set1.color = Color.BLUE
                    }
                    else if (j == 2) {
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
                chart.data.notifyDataChanged()
                // set data
                chart.notifyDataSetChanged()

                chart.invalidate()

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


}