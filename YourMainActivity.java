package com.your.package;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.widget.TextView;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class YourMainActivity extends Activity {
  private SharedPreferences pref;
  
  @Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    pref = getSharedPreferences("sp", Activity.MODE_PRIVATE);

    String cpuCode = getCPUInfo();
    getChipsetData(cpuCode);
}

private String getCPUInfo() {
    try {
        BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
        String line;
        StringBuilder cpuInfo = new StringBuilder();
        while ((line = br.readLine()) != null) {
            if (line.toLowerCase().contains("hardware") ||
                line.toLowerCase().contains("model name")) {
                cpuInfo.append(line.split(":")[1].trim());
                break;
            }
        }
        br.close();
        String raw = cpuInfo.length() > 0 ? cpuInfo.toString() : Build.HARDWARE;
        return raw.toUpperCase().replaceAll("[^A-Z0-9]", "");
    } catch (Exception e) {
        return Build.HARDWARE.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}

private void getChipsetData(String cpuCode) {
    String cachedCpu = pref.getString(cpuCode+"_cpu", null);
    String cachedGpu = pref.getString(cpuCode+"_gpu", null);

    if(cachedCpu != null && cachedGpu != null){
        tvCPU.setText(cachedCpu);
        tvGpu.setText(cachedGpu);
        return;
    }

    String url = "";
    if(cpuCode.startsWith("MT")){
        url = "https://tolepcoy.pages.dev/tolepcoy_mediatek?code=" + cpuCode;
    } else if(cpuCode.startsWith("MSM") || cpuCode.startsWith("SM")){
        url = "https://tolepcoy.pages.dev/tolepcoy_qualcomm?code=" + cpuCode;
    } else {
        tvCPU.setText("Chipset not found!");
        tvGpu.setText("Unknown");
        return;
    }

    String finalUrl = url;
    new Thread(() -> {
        try {
            URL urlObj = new URL(finalUrl);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while((line = br.readLine()) != null){
                result.append(line);
            }
            br.close();

            JSONObject json = new JSONObject(result.toString());

            String cpu = json.optString("chipset", "Nothing");
            String gpu = json.optString("gpu", "Nothing");

            // Save to SharedPreferences
            pref.edit()
                .putString(cpuCode+"_cpu", cpu)
                .putString(cpuCode+"_gpu", gpu)
                .apply();

            runOnUiThread(() -> {
                tvCPU.setText(cpu);
                tvGpu.setText(gpu);
            });

        } catch(Exception e){
            runOnUiThread(() -> {
                tvCPU.setText("Chipset not found!");
                tvGpu.setText("Unknown");
            });
        }
    }).start();
}
}