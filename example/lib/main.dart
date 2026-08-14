import 'dart:core';

import 'package:flutter/material.dart';
import 'package:flutter_background/flutter_background.dart';
import 'package:flutter_webrtc_example/src/capture_frame_sample.dart';

import 'src/adm_sample.dart';
import 'src/data_packet_cryptor_sample.dart';
import 'src/device_enumeration_sample.dart';
import 'src/get_display_media_sample.dart';
import 'src/get_virtual_display_media_sample.dart';
import 'src/get_user_media_sample.dart'
    if (dart.library.js_interop) 'src/get_user_media_sample_web.dart';
import 'src/loopback_data_channel_sample.dart';
import 'src/loopback_sample_unified_tracks.dart';
import 'src/route_item.dart';
import 'src/screen_capture_api_sample.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(MyApp());
}

// This is the specific entry point our hidden capture engine will run —
// completely separate from the app's normal home screen. In the real IS
// app, this would show the clean, full-screen active camera instead.
@pragma('vm:entry-point')
void virtualDisplayEntrypoint() {
  runApp(MaterialApp(
    debugShowCheckedModeBanner: false,
    home: Scaffold(
      backgroundColor: Colors.black,
      body: Center(
        child: TweenAnimationBuilder<double>(
          tween: Tween(begin: 0, end: 1),
          duration: const Duration(seconds: 2),
          builder: (context, value, child) {
            return Container(
              width: 150,
              height: 150,
              decoration: BoxDecoration(
                color: Color.lerp(Colors.red, Colors.blue, value),
                borderRadius: BorderRadius.circular(20),
              ),
              child: Center(
                child: Text(
                  'IS CLEAN VIDEO',
                  style: TextStyle(color: Colors.white, fontSize: 14),
                  textAlign: TextAlign.center,
                ),
              ),
            );
          },
          onEnd: () {},
        ),
      ),
    ),
  ));
}

Future<bool> startForegroundService() async {
  final androidConfig = FlutterBackgroundAndroidConfig(
    notificationTitle: 'Title of the notification',
    notificationText: 'Text of the notification',
    notificationImportance: AndroidNotificationImportance.normal,
    notificationIcon: AndroidResource(
        name: 'background_icon',
        defType: 'drawable'), // Default is ic_launcher from folder mipmap
  );
  await FlutterBackground.initialize(androidConfig: androidConfig);
  return FlutterBackground.enableBackgroundExecution();
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  late List<RouteItem> items;

  @override
  void initState() {
    super.initState();
    _initItems();
  }

  ListBody _buildRow(context, item) {
    return ListBody(children: <Widget>[
      ListTile(
        title: Text(item.title),
        onTap: () => item.push(context),
        trailing: Icon(Icons.arrow_right),
      ),
      Divider()
    ]);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
          appBar: AppBar(
            title: Text('Flutter-WebRTC example'),
          ),
          body: ListView.builder(
              shrinkWrap: true,
              padding: const EdgeInsets.all(0.0),
              itemCount: items.length,
              itemBuilder: (context, i) {
                return _buildRow(context, items[i]);
              })),
    );
  }

  void _initItems() {
    items = <RouteItem>[
      RouteItem(
          title: 'GetUserMedia',
          push: (BuildContext context) {
            Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (BuildContext context) => GetUserMediaSample()));
          }),
      
      RouteItem(
          title: 'GetVirtualDisplayMedia (IS Test)',
          push: (BuildContext context) {
            Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (BuildContext context) =>
                        GetVirtualDisplayMediaSample()));
          }),


      RouteItem(
          title: 'Device Enumeration',
          push: (BuildContext context) {
            Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (BuildContext context) =>
                        DeviceEnumerationSample()));
          }),
      RouteItem(
          title: 'GetDisplayMedia',
          push: (BuildContext context) {
            Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (BuildContext context) =>
                        GetDisplayMediaSample()));
          }),
      RouteItem(
          title: 'Screen Capture APIs',
          push: (BuildContext context) {
            Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (BuildContext context) =>
                        ScreenCaptureApiSample()));
          }),
      RouteItem(
          title: 'LoopBack Sample (Unified Tracks)',
          push: (BuildContext context) {
            Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (BuildContext context) =>
                        LoopBackSampleUnifiedTracks()));
          }),
      RouteItem(
          title: 'DataChannelLoopBackSample',
          push: (BuildContext context) {
            Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (BuildContext context) =>
                        DataChannelLoopBackSample()));
          }),
      RouteItem(
          title: 'Capture Frame',
          push: (BuildContext context) {
            Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (BuildContext context) => CaptureFrameSample()));
          }),
      RouteItem(
          title: 'ADM Sample',
          push: (BuildContext context) {
            Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (BuildContext context) => AdmSample()));
          }),
      RouteItem(
          title: 'Data Packet Cryptor Sample',
          push: (BuildContext context) {
            Navigator.push(
                context,
                MaterialPageRoute(
                    builder: (BuildContext context) => DataPacketCryptorSample()));
          }),
    ];
  }
}
