import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:http/http.dart' as http;

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: YourMainActivity(),
    );
  }
}

class YourMainActivity extends StatefulWidget {
  @override
  _YourMainActivityState createState() => _YourMainActivityState();
}

class _YourMainActivityState extends State<YourMainActivity> {
  Map<String, dynamic> cache = {};
  String cpuCode = "";
  String chipset = "";
  String gpu = "";

  @override
  void initState() {
    super.initState();
    loadCache().then((_) {
      cpuCode = getCPUInfo();
      getChipsetData(cpuCode);
    });
  }

  Future<File> _getCacheFile() async {
    final dir = await getApplicationDocumentsDirectory();
    return File('${dir.path}/chipset_cache.json');
  }

  Future<void> loadCache() async {
    try {
      final file = await _getCacheFile();
      if (await file.exists()) {
        cache = jsonDecode(await file.readAsString());
      }
    } catch (_) {
      cache = {};
    }
  }

  Future<void> saveCache() async {
    final file = await _getCacheFile();
    await file.writeAsString(jsonEncode(cache));
  }

  String getCPUInfo() {
    String raw = "UNKNOWN";
    try {
      final file = File("/proc/cpuinfo");
      if (file.existsSync()) {
        final lines = file.readAsLinesSync();
        for (var line in lines) {
          final lower = line.toLowerCase();
          if (lower.contains("hardware") || lower.contains("model name")) {
            raw = line.split(":")[1].trim();
            break;
          }
        }
      }
    } catch (_) {}

    return raw.toUpperCase().replaceAll(RegExp(r'[^A-Z0-9]'), '');
  }

  Future<void> getChipsetData(String cpuCode) async {
    if (cache.containsKey(cpuCode)) {
      setState(() {
        chipset = cache[cpuCode]["chipset"];
        gpu = cache[cpuCode]["gpu"];
      });
      return;
    }

    String url = "";
    if (cpuCode.startsWith("MT")) {
      url = "https://tolepcoy.pages.dev/tolepcoy_mediatek?code=$cpuCode";
    } else if (cpuCode.startsWith("MSM") || cpuCode.startsWith("SM")) {
      url = "https://tolepcoy.pages.dev/tolepcoy_qualcomm?code=$cpuCode";
    } else {
      setState(() {
        chipset = "Chipset not found!";
        gpu = "Unknown";
      });
      return;
    }

    try {
      final response = await http.get(Uri.parse(url));
      final data = jsonDecode(response.body);

      String fetchedChipset = data["chipset"] ?? "Nothing";
      String fetchedGpu = data["gpu"] ?? "Nothing";

      cache[cpuCode] = {"chipset": fetchedChipset, "gpu": fetchedGpu};
      await saveCache();

      setState(() {
        chipset = fetchedChipset;
        gpu = fetchedGpu;
      });
    } catch (_) {
      setState(() {
        chipset = "Chipset not found!";
        gpu = "Unknown";
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text("Chipset Info")),
      body: Padding(
        padding: EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text("CPU Code: $cpuCode"),
            SizedBox(height: 8),
            Text("Chipset: $chipset"),
            SizedBox(height: 8),
            Text("GPU: $gpu"),
          ],
        ),
      ),
    );
  }
}