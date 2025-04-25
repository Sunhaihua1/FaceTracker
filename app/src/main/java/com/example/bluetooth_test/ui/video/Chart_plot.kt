package com.example.bluetooth_test.ui.video

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
import com.example.bluetooth_test.databinding.FragmentChartPlotBinding
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
class Chart_plot : Fragment() {
    val arr_region = arrayOf(arrayOf(1, 2, 3, 4, 5, 6, 7, 8))
    val region_name = arrayOf("右嘴唇", "左眉毛", "右下颚","左下颚","右眉毛","左嘴唇","右脸颊","左脸颊")
    private var _binding: FragmentChartPlotBinding? = null
    private var Fragment_id: Int? = null;
    val arrList : MutableList<LineChart> = mutableListOf<LineChart>()
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    var messageHandler:MessageHandler? = MessageHandler();
    private var _Test: EditText? = null;
    private var connected_thread = ConnectedThread.getInstance();

    var timer = Timer();
    var timerTask: TimerTask?=null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        messageHandler = MessageHandler();
        Fragment_id = arguments?.getInt(ARG_SECTION_NUMBER);

    }


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
            timer.schedule(timerTask,0, 150)

        }
        else timerTask?.cancel();
        Log.e("TAG", "onVisible: " + Fragment_id)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentChartPlotBinding.inflate(inflater, container, false)
        _binding?.sectionLabel?.text = "未开始采集"

        val root = binding.root;
        arrList.clear();
        for (i in arr_region[Fragment_id!!]) {
            val mchart = LineChart(context);
            mchart.id = View.generateViewId();
            val layout = binding.linearPlot
            layout.addView(mchart)
            val layoutParams = mchart.layoutParams
            layoutParams.height = 400
            layoutParams.width = LayoutParams.MATCH_PARENT;
            arrList.add(mchart)
            val description = Description()
            description.text = region_name[i-1] + i.toString()
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
        timer.schedule(timerTask,0, 150)

        return root
    }

    @SuppressLint("HandlerLeak")
    inner class MessageHandler: Handler(){
        override fun handleMessage(msg: Message) {
            try{
                super.handleMessage(msg)
                activity?.runOnUiThread {
                    setData()
                }
            } catch (t: Throwable) {
                return
            }

        }
    }
    fun updateText(newText: String) {
            binding.sectionLabel.text = newText  // 直接更新 UI
    }

    fun setData() {
        if(_binding == null) return;
        for (i in 0 until arrList.size) {
            val values = ArrayList<ArrayList<Entry>>(3).apply {
                for (t in 1..3) {
                    add(ArrayList())
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
                    val setName = when (j) {
                        0 -> "AccX"
                        1 -> "AccY"
                        2 -> "AccZ"
                        else -> "Unknown"  // 默认情况（可选）
                    }

                    set1 = LineDataSet(values[j], setName)
                    set1.apply {
                        // 线条样式
                        color = when (j) {
                            0 -> Color.rgb(255, 99, 132)
                            1 -> Color.rgb(54, 162, 235)
                            else -> Color.rgb(75, 192, 192)
                        }
                        lineWidth = 2.5f
                        mode = LineDataSet.Mode.CUBIC_BEZIER

                        // 数据点样式
                        circleRadius = 4f
                        setCircleColor(color)
                        setDrawCircleHole(true)
                        circleHoleRadius = 2f
                        circleHoleColor = Color.WHITE

                        // 填充和交互
                        setDrawFilled(true)
                        fillDrawable = GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(Color.argb(100, 255, 99, 132), Color.argb(20, 255, 99, 132))
                        )
                        highLightColor = Color.rgb(244, 117, 117)
                        enableDashedHighlightLine(10f, 5f, 0f)
                    }
                    dataSets.add(set1)

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
        fun newInstance(sectionNumber: Int): Chart_plot {
            return Chart_plot().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }


}