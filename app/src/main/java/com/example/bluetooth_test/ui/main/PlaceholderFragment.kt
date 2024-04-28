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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.bluetooth_test.ConnectedThread
import com.example.bluetooth_test.databinding.FragmentPlotactivityBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet


/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel
    private var _binding: FragmentPlotactivityBinding? = null
    private var Fragment_id: Int? = null;
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

    private fun setData(chart: LineChart,count: Int, range: Float) {

        val values = ArrayList<Entry>()

        for (i in 0 until count) {

            val value = (Math.random() * range).toFloat() - 30
            values.add(Entry(i.toFloat(), value/*, resources.getDrawable(R.drawable.star)*/))
        }

        val set1: LineDataSet

        if (chart.data != null && chart.data.dataSetCount > 0) {
            set1 = chart.data.getDataSetByIndex(0) as LineDataSet
            set1.values = values
            set1.notifyDataSetChanged()
            chart.data.notifyDataChanged()
            chart.notifyDataSetChanged()
        } else {
            // create a dataset and give it a type
            set1 = LineDataSet(values, "DataSet 1")

            set1.setDrawIcons(false)

            // draw dashed line
            set1.enableDashedLine(10f, 5f, 0f)

            // black lines and points
            set1.color = Color.BLACK
            set1.setCircleColor(Color.BLACK)

            // line thickness and point size
            set1.lineWidth = 1f
            set1.circleRadius = 3f

            // draw points as solid circles
            set1.setDrawCircleHole(false)

            // customize legend entry
            set1.formLineWidth = 1f
            set1.formLineDashEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
            set1.formSize = 15f

            // text size of values
            set1.valueTextSize = 9f

            // draw selection line as dashed
            set1.enableDashedHighlightLine(10f, 5f, 0f)

            // set the filled area
            set1.setDrawFilled(true)
            set1.fillFormatter = IFillFormatter { dataSet, dataProvider -> chart.axisLeft.axisMinimum }

            // set color of filled area
            /*if (Utils.getSDKInt() >= 18) {
                // drawables only supported on api level 18 and above
                val drawable = ContextCompat.getDrawable(this, R.drawable.fade_red)
                set1.fillDrawable = drawable
            } else {
                set1.fillColor = Color.BLACK
            }*/

            val dataSets = ArrayList<ILineDataSet>()
            dataSets.add(set1) // add the data sets

            // create a data object with the data sets
            val data = LineData(dataSets)

            // set data
            chart.data = data
        }
    }




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentPlotactivityBinding.inflate(inflater, container, false)
        val root = binding.root;
        var textView: TextView = binding.sectionLabel;
        textView.setText("Hello from " + ConnectedThread.data);
//        pageViewModel.text.observe(viewLifecycleOwner, Observer {
//            textView.text = it
//        })
        var mchart = LineChart(context);
        val layoutParams = mchart.layoutParams
        layoutParams.height = 100
        layoutParams.width = LayoutParams.MATCH_PARENT;


        var layout = binding.layoutLinear
        layout.addView(mchart)
        textView = binding.editTextText;
        val mpchart0 = binding.chart0
        val mpchart1 = binding.chart1
        val mpchart2 = binding.chart2
        val chart = arrayOf(mpchart0,mpchart1,mpchart2);
        for ((i, plotchart) in chart.withIndex()) {
            plotchart.apply {
                setTouchEnabled(false);
                description.isEnabled = (false);
                xAxis.isEnabled = (false);
                xAxis.isEnabled = (false);
                axisLeft.isEnabled = (false);
                axisRight.isEnabled = (false);
            }
            setData(plotchart,45, 180f);
        }
        Thread {
            while (true){
                try{
                    Thread.sleep(1000)
                    Log.e("TAG", "fffffffff: " )

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
                    binding.sectionLabel.setText(ConnectedThread.data.toString())

                }
            } catch (t: Throwable) {
                return
            }

            Log.e("TAG", "xxxxxxxxxx: " )
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