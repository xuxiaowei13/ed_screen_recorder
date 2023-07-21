import 'dart:async';
import 'dart:io';

import 'package:ed_screen_recorder/ed_screen_recorder.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';
import 'package:video_player/video_player.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: ThemeData.dark(),
      debugShowCheckedModeBanner: false,
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({Key? key}) : super(key: key);

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  EdScreenRecorder? screenRecorder;
  Map<String, dynamic>? _response;
  bool inProgress = false;
  VideoPlayerController? _controller;
  final TextEditingController _textEditingController = TextEditingController();

  @override
  void initState() {
    super.initState();
    screenRecorder = EdScreenRecorder();
  }

  Future<void> startRecord({required String fileName}) async {
    Directory? tempDir = await getExternalStorageDirectory();
    if (tempDir == null) return;
    String tempPath = tempDir.path;
    try {
      var startResponse = await screenRecorder?.startRecordScreen(
        fileName: "Eren",
        //Optional. It will save the video there when you give the file path with whatever you want.
        //If you leave it blank, the Android operating system will save it to the gallery.
        dirPathToSave: tempPath,
        audioEnable: false,
        videoBitrate: 1000000,
        videoFrame: 15,
        width: 480,
        height: 960,
      );
      setState(() {
        _response = startResponse;
      });
    } on PlatformException {
      kDebugMode
          ? debugPrint("Error: An error occurred while starting the recording!")
          : null;
    }
  }

  Future<void> stopRecord() async {
    try {
      var stopResponse = await screenRecorder?.stopRecord();
      setState(() {
        _response = stopResponse;
        _controller = VideoPlayerController.file(_response?['file']);
      });
      await _controller!.initialize();
      setState(() {});
    } on PlatformException {
      kDebugMode
          ? debugPrint("Error: An error occurred while stopping recording.")
          : null;
    }
  }

  Future<void> pauseRecord() async {
    try {
      await screenRecorder?.pauseRecord();
    } on PlatformException {
      kDebugMode
          ? debugPrint("Error: An error occurred while pause recording.")
          : null;
    }
  }

  Future<void> resumeRecord() async {
    try {
      await screenRecorder?.resumeRecord();
    } on PlatformException {
      kDebugMode
          ? debugPrint("Error: An error occurred while resume recording.")
          : null;
    }
  }

  Future<void> seekToTimestamp() async {
    // calculate the cursor relative to the start timestamp
    if (_controller != null && _controller!.value.isInitialized) {
      int targetTimestamp = int.parse(_textEditingController.value.text);
      if (_response != null) {
        int startTimestamp = _response?['startdate'];
        int cursor = targetTimestamp - startTimestamp;
        await _controller?.seekTo(Duration(milliseconds: cursor));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Screen Recording Debug"),
      ),
      body: Center(
        child: SingleChildScrollView(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text("File: ${(_response?['file'] as File?)?.path}"),
              Text("Status: ${(_response?['success']).toString()}"),
              Text("Event: ${_response?['eventname']}"),
              Text("Progress: ${(_response?['progressing']).toString()}"),
              Text("Message: ${_response?['message']}"),
              Text("Video Hash: ${_response?['videohash']}"),
              Text("Start Date: ${(_response?['startdate']).toString()}"),
              Text("End Date: ${(_response?['enddate']).toString()}"),
              ElevatedButton(
                  onPressed: () => startRecord(fileName: "eren"),
                  child: const Text('START RECORD')),
              ElevatedButton(
                  onPressed: () => resumeRecord(),
                  child: const Text('RESUME RECORD')),
              ElevatedButton(
                  onPressed: () => pauseRecord(),
                  child: const Text('PAUSE RECORD')),
              ElevatedButton(
                  onPressed: () => stopRecord(),
                  child: const Text('STOP RECORD')),
              // add a text field for the target timestamp
              TextField(
                controller: _textEditingController,
                decoration: const InputDecoration(
                  hintText: 'Enter a timestamp',
                ),
              ),
              ElevatedButton(
                  onPressed: () => seekToTimestamp(),
                  child: const Text('SEEK TO TIMESTAMP')),
              // add a video viewer
              if (_controller != null && _controller!.value.isInitialized)
                AspectRatio(
                  aspectRatio: _controller!.value.aspectRatio,
                  child: VideoPlayer(_controller!),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
