import 'dart:convert';

import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:geolocator/geolocator.dart';
import 'package:http/http.dart' as http;
import 'package:permission_handler/permission_handler.dart';

const String _apiUrl = 'http://192.168.1.110:8080/api/tickets';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
  final cameras = await availableCameras();
  runApp(KentGozuApp(cameras: cameras));
}

class KentGozuApp extends StatelessWidget {
  const KentGozuApp({super.key, required this.cameras});

  final List<CameraDescription> cameras;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'KentGözü Saha',
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark().copyWith(
        scaffoldBackgroundColor: Colors.black,
      ),
      home: cameras.isEmpty
          ? const _NoCameraScreen()
          : ReportScreen(camera: cameras.first),
    );
  }
}

class _NoCameraScreen extends StatelessWidget {
  const _NoCameraScreen();

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Center(
        child: Text(
          'Kamera bulunamadı.',
          style: TextStyle(color: Colors.white70, fontSize: 18),
        ),
      ),
    );
  }
}

class ReportScreen extends StatefulWidget {
  const ReportScreen({super.key, required this.camera});

  final CameraDescription camera;

  @override
  State<ReportScreen> createState() => _ReportScreenState();
}

class _ReportScreenState extends State<ReportScreen> {
  CameraController? _controller;
  bool _cameraReady = false;
  bool _loading = false;
  String? _initError;

  @override
  void initState() {
    super.initState();
    _initCamera();
  }

  Future<void> _initCamera() async {
    final cameraStatus = await Permission.camera.request();
    if (!cameraStatus.isGranted) {
      if (mounted) {
        setState(() => _initError = 'Kamera izni reddedildi.');
      }
      return;
    }

    final ctrl = CameraController(
      widget.camera,
      ResolutionPreset.high,
      enableAudio: false,
      imageFormatGroup: ImageFormatGroup.jpeg,
    );

    try {
      await ctrl.initialize();
      if (!mounted) return;
      setState(() {
        _controller = ctrl;
        _cameraReady = true;
      });
    } catch (e) {
      if (mounted) {
        setState(() => _initError = 'Kamera başlatılamadı: $e');
      }
    }
  }

  Future<Position> _getLocation() async {
    bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      throw Exception('Konum servisi kapalı. Lütfen açın.');
    }

    LocationPermission permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }
    if (permission == LocationPermission.denied ||
        permission == LocationPermission.deniedForever) {
      throw Exception('Konum izni reddedildi.');
    }

    return Geolocator.getCurrentPosition(
      locationSettings: const LocationSettings(
        accuracy: LocationAccuracy.high,
        timeLimit: Duration(seconds: 10),
      ),
    );
  }

  Future<void> _submitReport() async {
    if (_loading || _controller == null || !_cameraReady) return;

    setState(() => _loading = true);

    try {
      await _controller!.takePicture();

      final position = await _getLocation();

      final payload = {
        'reporterEmail': 'saha@kentgozu.com',
        'title': 'Mobil Saha İhbarı',
        'description': 'Sahadan anlık konumla gönderildi.',
        'status': 'SUBMITTED',
        'latitude': position.latitude,
        'longitude': position.longitude,
      };

      final response = await http
          .post(
            Uri.parse(_apiUrl),
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode(payload),
          )
          .timeout(const Duration(seconds: 15));

      if (!mounted) return;

      if (response.statusCode >= 200 && response.statusCode < 300) {
        _showSnackbar('Merkeze İletildi ✓', Colors.green.shade700);
      } else {
        _showSnackbar(
          'Sunucu hatası: ${response.statusCode}',
          Colors.red.shade700,
        );
      }
    } catch (e) {
      if (mounted) {
        _showSnackbar('Hata: $e', Colors.red.shade700);
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _showSnackbar(String message, Color background) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          message,
          style: const TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.w600,
          ),
        ),
        backgroundColor: background,
        behavior: SnackBarBehavior.floating,
        margin: const EdgeInsets.fromLTRB(16, 0, 16, 100),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        duration: const Duration(seconds: 3),
      ),
    );
  }

  @override
  void dispose() {
    _controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        fit: StackFit.expand,
        children: [
          _buildCameraLayer(),
          _buildHud(),
          if (_loading) _buildLoadingOverlay(),
        ],
      ),
    );
  }

  Widget _buildCameraLayer() {
    if (_initError != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Text(
            _initError!,
            textAlign: TextAlign.center,
            style: const TextStyle(color: Colors.white70, fontSize: 16),
          ),
        ),
      );
    }

    if (!_cameraReady || _controller == null) {
      return const Center(
        child: CircularProgressIndicator(color: Colors.white),
      );
    }

    return SizedBox.expand(
      child: FittedBox(
        fit: BoxFit.cover,
        child: SizedBox(
          width: _controller!.value.previewSize?.height ?? 1,
          height: _controller!.value.previewSize?.width ?? 1,
          child: CameraPreview(_controller!),
        ),
      ),
    );
  }

  Widget _buildHud() {
    return SafeArea(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 16),
            child: Text(
              'KentGözü',
              style: TextStyle(
                color: Colors.white,
                fontSize: 20,
                fontWeight: FontWeight.bold,
                letterSpacing: 2,
                shadows: [
                  Shadow(color: Colors.black54, blurRadius: 8),
                ],
              ),
            ),
          ),
          const Spacer(),
          Padding(
            padding: const EdgeInsets.only(bottom: 48),
            child: GestureDetector(
              onTap: _loading ? null : _submitReport,
              child: Container(
                width: 88,
                height: 88,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: _loading
                      ? Colors.grey.shade700
                      : const Color(0xFFFF3B30),
                  border: Border.all(color: Colors.white, width: 4),
                  boxShadow: const [
                    BoxShadow(
                      color: Colors.black45,
                      blurRadius: 16,
                      offset: Offset(0, 6),
                    ),
                  ],
                ),
                child: const Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.campaign_rounded, color: Colors.white, size: 32),
                    SizedBox(height: 2),
                    Text(
                      'İHBAR',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 11,
                        fontWeight: FontWeight.w800,
                        letterSpacing: 1.5,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildLoadingOverlay() {
    return Container(
      color: Colors.black54,
      child: const Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            CircularProgressIndicator(color: Colors.white, strokeWidth: 3),
            SizedBox(height: 16),
            Text(
              'Merkeze iletiliyor...',
              style: TextStyle(color: Colors.white70, fontSize: 14),
            ),
          ],
        ),
      ),
    );
  }
}
