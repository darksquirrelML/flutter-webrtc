import 'dart:core';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_webrtc/flutter_webrtc.dart';
import 'package:flutter_webrtc/src/native/mediadevices_impl.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter/foundation.dart';

/*
 * getVirtualDisplayMedia sample — proof of concept test.
 * Captures a hidden FlutterEngine's rendered output (never shown on the
 * real screen) and turns it into a valid WebRTC video track.
 */
class GetVirtualDisplayMediaSample extends StatefulWidget {
  static String tag = 'get_virtual_display_media_sample';

  @override
  _GetVirtualDisplayMediaSampleState createState() =>
      _GetVirtualDisplayMediaSampleState();
}

class _GetVirtualDisplayMediaSampleState
    extends State<GetVirtualDisplayMediaSample> {
  MediaStream? _localStream;
  final RTCVideoRenderer _localRenderer = RTCVideoRenderer();
  bool _inCalling = false;
  String _status = 'Not started';

  static const String _engineId = 'is_virtual_display_test_engine';

  @override
  void initState() {
    super.initState();
    initRenderers();
  }

  @override
  void deactivate() {
    super.deactivate();
    if (_inCalling) {
      _stop();
    }
    _localRenderer.dispose();
  }

  Future<void> initRenderers() async {
    await _localRenderer.initialize();
  }

  Future<void> _makeCall() async {
    setState(() {
      _status = 'Calling getVirtualDisplayMedia...';
    });
    try {
      final mediaDevices = navigator.mediaDevices as MediaDeviceNative;
      var stream = await mediaDevices.getVirtualDisplayMedia(<String, dynamic>{
        'width': 720,
        'height': 1280,
        'fps': 15,
      });

      _localStream = stream;
      _localRenderer.srcObject = _localStream;

      setState(() {
        _status = 'Capturing — you should see live content below';
      });
    } catch (e) {
      setState(() {
        _status = 'ERROR: $e';
      });
      print(e.toString());
    }
    if (!mounted) return;

    setState(() {
      _inCalling = true;
    });
  }

  Future<void> _stop() async {
    try {
      await _localStream?.dispose();
      _localStream = null;
      _localRenderer.srcObject = null;
    } catch (e) {
      print(e.toString());
    }
  }

  Future<void> _hangUp() async {
    await _stop();
    setState(() {
      _inCalling = false;
      _status = 'Stopped';
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('GetVirtualDisplayMedia test'),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12.0),
            child: Text(_status, style: TextStyle(fontSize: 14)),
          ),
          Expanded(
            child: Container(
              width: MediaQuery.of(context).size.width,
              color: Colors.black12,
              child: _inCalling
                  ? RTCVideoView(_localRenderer)
                  : Center(child: Text('Not capturing yet')),
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          _inCalling ? _hangUp() : _makeCall();
        },
        tooltip: _inCalling ? 'Stop' : 'Start Test',
        child: Icon(_inCalling ? Icons.stop : Icons.play_arrow),
      ),
    );
  }
}