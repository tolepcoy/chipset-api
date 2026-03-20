package com.your.package

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.app.Activity
import android.widget.TextView
import org.json.JSONObject
import java.io.BufferedReader
import java.io.FileReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class YourMainActivity : Activity() {

    private lateinit var pref: SharedPreferences
    private lateinit var tvCPU: TextView
    private lateinit var tvGpu: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pref = getSharedPreferences("sp", Activity.MODE_PRIVATE)
        tvCPU = findViewById(R.id.tvCPU)
        tvGpu = findViewById(R.id.tvGpu)

        val cpuCode = getCPUInfo()
        getChipsetData(cpuCode)
    }

    private fun getCPUInfo(): String {
        return try {
            val br = BufferedReader(FileReader("/proc/cpuinfo"))
            val cpuInfo = StringBuilder()
            var line: String?
            while (br.readLine().also { line = it } != null) {
                line?.let {
                    val lower = it.lowercase()
                    if (lower.contains("hardware") || lower.contains("model name")) {
                        cpuInfo.append(it.split(":")[1].trim())
                        break
                    }
                }
            }
            br.close()
            val raw = if (cpuInfo.isNotEmpty()) cpuInfo.toString() else Build.HARDWARE
            raw.uppercase().replace(Regex("[^A-Z0-9]"), "")
        } catch (e: Exception) {
            Build.HARDWARE.uppercase().replace(Regex("[^A-Z0-9]"), "")
        }
    }

    private fun getChipsetData(cpuCode: String) {
        val cachedCpu = pref.getString("${cpuCode}_cpu", null)
        val cachedGpu = pref.getString("${cpuCode}_gpu", null)

        if (cachedCpu != null && cachedGpu != null) {
            tvCPU.text = cachedCpu
            tvGpu.text = cachedGpu
            return
        }

        val url = when {
            cpuCode.startsWith("MT") -> "https://tolepcoy.pages.dev/tolepcoy_mediatek?code=$cpuCode"
            cpuCode.startsWith("MSM") || cpuCode.startsWith("SM") -> "https://tolepcoy.pages.dev/tolepcoy_qualcomm?code=$cpuCode"
            else -> {
                tvCPU.text = "Chipset not found!"
                tvGpu.text = "Unknown"
                return
            }
        }

        Thread {
            try {
                val urlObj = URL(url)
                val conn = urlObj.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                val br = BufferedReader(InputStreamReader(conn.inputStream))
                val result = StringBuilder()
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    result.append(line)
                }
                br.close()

                val json = JSONObject(result.toString())
                val cpu = json.optString("chipset", "Nothing")
                val gpu = json.optString("gpu", "Nothing")

                pref.edit()
                    .putString("${cpuCode}_cpu", cpu)
                    .putString("${cpuCode}_gpu", gpu)
                    .apply()

                runOnUiThread {
                    tvCPU.text = cpu
                    tvGpu.text = gpu
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvCPU.text = "Chipset not found!"
                    tvGpu.text = "Unknown"
                }
            }
        }.start()
    }
}