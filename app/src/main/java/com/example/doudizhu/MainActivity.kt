package com.example.doudizhu

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.doudizhu.databinding.ActivityMainBinding
import java.util.Stack

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val textViews = mutableListOf<TextView>()
    private var lastClickTime = 0L
    private var lastClickedView: TextView? = null
    private val doubleClickTimeout = 300L // 双击判定时间间隔（毫秒）

    // 添加每种牌的总数映射
    private val cardTotalMap = mapOf(
        "2" to 12,
        "A" to 12,
        "K" to 12,
        "Q" to 12,
        "J" to 12,
        "10" to 12,
        "9" to 12,
        "8" to 12,
        "7" to 12,
        "6" to 12,
        "5" to 12,
        "4" to 12,
        "3" to 12
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        initViews()
        setupClickListeners()
        setResetClickListener()
    }

    private fun setResetClickListener() {
        var lastResetClickTime = 0L
        var isResetDoubleClick = false

        binding.reset.setOnClickListener {
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastResetClickTime < doubleClickTimeout) {
                // 双击处理：退回上一步
                isResetDoubleClick = true
                undoLastStep()
            } else {
                // 延迟处理单击，等待可能的双击
                isResetDoubleClick = false
                binding.reset.postDelayed({
                    if (!isResetDoubleClick) {
                        // 单击处理：重置
                        resetAll()
                    }
                }, doubleClickTimeout)
            }

            lastResetClickTime = currentTime
        }
    }

    private fun resetAll() {
        // 重置每个TextView的内容和背景色
        textViews.forEachIndexed { index, textView ->
            if (index % 5 == 0) { // 第0列
                val cardType = textView.text.toString()
                val totalCards = cardTotalMap[cardType] ?: 0
                // 重置右上角的剩余牌数
                val countTextView = binding.root.findViewById<TextView>(
                    resources.getIdentifier("count${index/5}0", "id", packageName)
                )
                countTextView?.text = totalCards.toString()
            } else {
                // 重置1-4列的牌数为0
                textView.text = ""
            }
            // 重置背景色为默认颜色
            textView.setBackgroundColor(defaultColor)
        }
        Toast.makeText(this, "页面已重置", Toast.LENGTH_SHORT).show()
    }

    // 添加颜色常量
    private val greenColor = android.graphics.Color.rgb(200, 255, 200)
    private val defaultColor = android.graphics.Color.TRANSPARENT

    // 用于保存每次点击的状态，包括行、列和增加的数量
    private val stateStack = Stack<Triple<Int, Int, Int>>()

    private fun handleSingleClick(row: Int, col: Int, textView: TextView) {
        if (col == 0) return  // 第0列不响应点击

        val currentValue = textView.text.toString().toIntOrNull() ?: 0
        val cardTextView = textViews[row * 5] // 第0列的牌面TextView
        val cardType = cardTextView.text.toString()
        val countTextView = binding.root.findViewById<TextView>(
            resources.getIdentifier("count${row}0", "id", packageName)
        )
        val remainingCards = countTextView.text.toString().toIntOrNull() ?: 0

        if (remainingCards > 0) {
            // 保存当前状态，增加1
            stateStack.push(Triple(row, col, 1))

            val newValue = currentValue + 1
            textView.text = newValue.toString()
            
            val newRemainingCards = remainingCards - 1
            countTextView.text = newRemainingCards.toString()
            
            if (newRemainingCards == 0) {
                setRowBackground(row, greenColor)
                Toast.makeText(this, "${cardType}已出完", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleDoubleClick(row: Int, col: Int, textView: TextView) {
        if (col == 0) return  // 第0列不响应点击

        val currentValue = textView.text.toString().toIntOrNull() ?: 0
        val cardTextView = textViews[row * 5] // 第0列的牌面TextView
        val cardType = cardTextView.text.toString()
        val countTextView = binding.root.findViewById<TextView>(
            resources.getIdentifier("count${row}0", "id", packageName)
        )
        val remainingCards = countTextView.text.toString().toIntOrNull() ?: 0

        // 如果剩余牌数大于0，处理双击
        if (remainingCards > 0) {
            val increment = if (remainingCards >= 2) 2 else 1
            // 保存当前状态，增加1或2
            stateStack.push(Triple(row, col, increment))

            val newValue = currentValue + increment
            textView.text = newValue.toString()
            
            val newRemainingCards = remainingCards - increment
            countTextView.text = newRemainingCards.toString()
            
            if (newRemainingCards == 0) {
                setRowBackground(row, greenColor)
                Toast.makeText(this, "${cardType}已出完", Toast.LENGTH_SHORT).show()
            } else {
                Log.i("${MainActivity::class.simpleName}","在第${row}行，第${col}列添加了${increment}张牌")
            }
        }
    }

    private fun undoLastStep() {
        if (stateStack.isNotEmpty()) {
            val (row, col, increment) = stateStack.pop()
            val textView = textViews[row * 5 + col]
            val currentValue = textView.text.toString().toIntOrNull() ?: 0

            val countTextView = binding.root.findViewById<TextView>(
                resources.getIdentifier("count${row}0", "id", packageName)
            )
            val remainingCards = countTextView.text.toString().toIntOrNull() ?: 0

            // 退回一步，减去保存的增量
            if (currentValue >= increment) {
                val newValue = currentValue - increment
                textView.text = if (newValue == 0) "" else newValue.toString()
                countTextView.text = (remainingCards + increment).toString()

                // 如果剩余牌数不为0，将整行背景色恢复为默认颜色
                if (remainingCards + increment > 0) {
                    setRowBackground(row, defaultColor)
                }
            }
        } else {
            Toast.makeText(this, "没有可退回的步骤", Toast.LENGTH_SHORT).show()
        }
    }

    // 添加设置整行背景色的辅助函数
    private fun setRowBackground(row: Int, color: Int) {
        for (col in 0..4) {
            val position = row * 5 + col
            if (position < textViews.size) {
                textViews[position].setBackgroundColor(color)
            }
        }
    }

    private fun initViews() {
        // 初始化所有带ID的TextView
        with(binding) {
            textViews.addAll(
                listOf(
                    id00, id01, id02, id03, id04,
                    id10, id11, id12, id13, id14,
                    id20, id21, id22, id23, id24,
                    id30, id31, id32, id33, id34,
                    id40, id41, id42, id43, id44,
                    id50, id51, id52, id53, id54,
                    id60, id61, id62, id63, id64,
                    id70, id71, id72, id73, id74,
                    id80, id81, id82, id83, id84,
                    id90, id91, id92, id93, id94,
                    id100, id101, id102, id103, id104,
                    id110, id111, id112, id113, id114,
                    id120, id121, id122, id123, id124
                )
            )
        }

        // 初始化每行第0列右上角的剩余牌数，并检查是否需要设置绿色背景
        textViews.forEachIndexed { index, textView ->
            if (index % 5 == 0) { // 第0列
                val cardType = textView.text.toString()
                val totalCards = cardTotalMap[cardType] ?: 0
                // 设置右上角的剩余牌数
                val countTextView = binding.root.findViewById<TextView>(
                    resources.getIdentifier("count${index/5}0", "id", packageName)
                )
                countTextView?.text = totalCards.toString()
                
                // 如果初始剩余牌数为0，设置该行为绿色
                if (totalCards == 0) {
                    setRowBackground(index / 5, greenColor)
                }
            }
        }
    }

    private fun setupClickListeners() {
        textViews.forEach { textView ->
            var isDoubleClick = false

            textView.setOnClickListener { view ->
                val clickedTextView = view as TextView
                val currentTime = System.currentTimeMillis()
                val position = textViews.indexOf(clickedTextView)
                val row = position / 5
                val col = position % 5

                if (lastClickedView == clickedTextView &&
                    currentTime - lastClickTime < doubleClickTimeout
                ) {
                    // 双击处理
                    isDoubleClick = true
                    handleDoubleClick(row, col, clickedTextView)
                } else {
                    // 延迟处理单击，等待可能的双击
                    isDoubleClick = false
                    textView.postDelayed({
                        if (!isDoubleClick) {
                            handleSingleClick(row, col, clickedTextView)
                        }
                    }, doubleClickTimeout)
                }

                lastClickTime = currentTime
                lastClickedView = clickedTextView
            }
        }
    }


}
