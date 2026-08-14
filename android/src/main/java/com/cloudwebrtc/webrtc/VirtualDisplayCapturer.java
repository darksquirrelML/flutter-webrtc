package com.cloudwebrtc.webrtc;

import org.webrtc.SurfaceTextureHelper;
import org.webrtc.CapturerObserver;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.view.Display;
import android.view.Surface;

import android.os.Handler;
import android.os.Looper;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.dart.DartExecutor;

/**
 * Captures video frames from a hidden Flutter widget, rendered onto a
 * private (non-mirrored, invisible) VirtualDisplay via a Presentation,
 * instead of mirroring the device's real screen (which is what
 * OrientationAwareScreenCapturer / MediaProjection do).
 */
@TargetApi(21)
public class VirtualDisplayCapturer implements VideoCapturer {
    private static final int VIRTUAL_DISPLAY_DPI = 400;

    private final Context applicationContext;

    private int width;
    private int height;
    private VirtualDisplay virtualDisplay;
    private Surface virtualDisplaySurface;
    private SurfaceTextureHelper surfaceTextureHelper;
    private CapturerObserver capturerObserver;
    private VirtualDisplayPresentation presentation;
    private volatile boolean isDisposed = false;

    public VirtualDisplayCapturer(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    private void checkNotDisposed() {
        if (isDisposed) {
            throw new RuntimeException("capturer is disposed.");
        }
    }

    @Override
    public synchronized void initialize(final SurfaceTextureHelper surfaceTextureHelper,
                                         final Context applicationContext, final CapturerObserver capturerObserver) {
        checkNotDisposed();
        if (capturerObserver == null) {
            throw new RuntimeException("capturerObserver not set.");
        }
        this.capturerObserver = capturerObserver;
        if (surfaceTextureHelper == null) {
            throw new RuntimeException("surfaceTextureHelper not set.");
        }
        this.surfaceTextureHelper = surfaceTextureHelper;
    }

    @Override
    public synchronized void startCapture(final int width, final int height, final int framerate) {
        this.width = width;
        this.height = height;

        createVirtualDisplay();
        capturerObserver.onCapturerStarted(true);
        surfaceTextureHelper.startListening(new VideoSink() {
            @Override
            public void onFrame(VideoFrame frame) {
                if (isDisposed) return;
                capturerObserver.onFrameCaptured(frame);
            }
        });
    }

    @Override
    public void stopCapture() {
        if (isDisposed) return;
        ThreadUtils.invokeAtFrontUninterruptibly(surfaceTextureHelper.getHandler(), new Runnable() {
            @Override
            public void run() {
                surfaceTextureHelper.stopListening();
                capturerObserver.onCapturerStopped();
                if (presentation != null) {
                    presentation.dismiss();
                    presentation = null;
                }
                if (virtualDisplay != null) {
                    virtualDisplay.release();
                    virtualDisplay = null;
                }
                if (virtualDisplaySurface != null) {
                    virtualDisplaySurface.release();
                    virtualDisplaySurface = null;
                }
            }
        });
    }

    @Override
    public synchronized void dispose() {
        isDisposed = true;
    }

    @Override
    public synchronized void changeCaptureFormat(final int width, final int height, final int framerate) {
        checkNotDisposed();
        this.width = width;
        this.height = height;
    }

    private void createVirtualDisplay() {
        surfaceTextureHelper.setTextureSize(width, height);
        surfaceTextureHelper.getSurfaceTexture().setDefaultBufferSize(width, height);
        virtualDisplaySurface = new Surface(surfaceTextureHelper.getSurfaceTexture());

        DisplayManager displayManager =
                (DisplayManager) applicationContext.getSystemService(Context.DISPLAY_SERVICE);

        // No PUBLIC flag: this display is never mirrored, never shown on the
        // real screen, and never selectable as an external display by the OS.
        int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;

        virtualDisplay = displayManager.createVirtualDisplay(
                "IS_CleanVideoCapture", width, height, VIRTUAL_DISPLAY_DPI,
                virtualDisplaySurface, flags);


        final Display display = virtualDisplay.getDisplay();

        // FlutterEngine + Presentation must be created on the main thread.
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                FlutterEngine engine = new FlutterEngine(applicationContext);
                engine.getDartExecutor().executeDartEntrypoint(
                        DartExecutor.DartEntrypoint.createDefault());

                presentation = new VirtualDisplayPresentation(applicationContext, display, engine);
                presentation.show();
            }
        });
    }


    @Override
    public boolean isScreencast() {
        return true;
    }
}