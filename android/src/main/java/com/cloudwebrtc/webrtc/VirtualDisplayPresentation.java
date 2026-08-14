package com.cloudwebrtc.webrtc;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import io.flutter.embedding.android.FlutterView;
import io.flutter.embedding.engine.FlutterEngine;

/**
 * Renders a Flutter widget tree onto a private VirtualDisplay via a
 * Presentation, without that content ever appearing on the device's
 * real, physical screen.
 */
public class VirtualDisplayPresentation extends Presentation {
    private final FlutterEngine flutterEngine;

    public VirtualDisplayPresentation(Context outerContext, Display display, FlutterEngine flutterEngine) {
        super(outerContext, display);
        this.flutterEngine = flutterEngine;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout container = new FrameLayout(getContext());
        setContentView(container, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        FlutterView flutterView = new FlutterView(getContext());
        container.addView(flutterView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        flutterView.attachToFlutterEngine(flutterEngine);
    }
}