package com.backgroundcheck;

import android.os.Handler;
import android.widget.Toast;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.util.Map;
import java.util.HashMap;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.json.JSONArray;
import org.json.JSONObject;

import com.backgroundcheck.ForegroundService.ForegroundBinder;
import com.facebook.react.bridge.ReadableMap;

import static android.content.Context.BIND_AUTO_CREATE;
import static com.backgroundcheck.BackgroundModeExt.clearKeyguardFlags;


public class BackgroundModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

  private static int count = 0;

  @Override
  public void onHostResume() {
    inBackground = false;
    stopService();
  }

  @Override
  public void onHostPause() {
    System.out.println("onHostPause");
    try {
      inBackground = true;
      startService();
      System.out.println("onHostPause success");
    } finally {
      clearKeyguardFlags(reactContext.getCurrentActivity());
    }
  }

  @Override
  public void onHostDestroy() {
    stopService();
    android.os.Process.killProcess(android.os.Process.myPid());
  }

  // Event types for callbacks
  private enum Event { ACTIVATE, DEACTIVATE, FAILURE }

  // Plugin namespace
  private static final String JS_NAMESPACE = "cordova.plugins.backgroundMode";

  // Flag indicates if the app is in background or foreground
  private boolean inBackground = false;

  // Flag indicates if the plugin is enabled or disabled
  private boolean isDisabled = true;

  // Flag indicates if the service is bind
  private boolean isBind = false;

  // Default settings for the notification

  // Service that keeps the app awake
  private ForegroundService service;

  // Used to (un)bind the service to with the activity
  private final ServiceConnection connection = new ServiceConnection()
  {
    @Override
    public void onServiceConnected (ComponentName name, IBinder service)
    {
      ForegroundBinder binder = (ForegroundBinder) service;
      BackgroundModule.this.service = binder.getService();
    }

    @Override
    public void onServiceDisconnected (ComponentName name)
    {
      fireEvent(Event.FAILURE, "'service disconnected'");
    }
  };

  /**
   * Executes the request.
   *
   * @param action   The action to execute.
   *
   * @return Returning false results in a "MethodNotFound" error.
   */
  @ReactMethod
  public boolean execute (String action, Boolean arg0, ReadableMap arg1)
  {
    System.out.println("execute" + action);
    boolean validAction = true;

    switch (action)
    {
      case "configure":
        configure(arg1, arg0);
        break;
      case "enable":
        enableMode();
        break;
      case "disable":
        disableMode();
        break;
      default:
        validAction = false;
    }

    return validAction;
  }

  /**
   * Enable the background mode.
   */
  private void enableMode()
  {
    isDisabled = false;

    if (inBackground) {
      startService();
    }
  }

  /**
   * Disable the background mode.
   */
  private void disableMode()
  {
    stopService();
    isDisabled = true;
  }

  /**
   * Update the default settings and configure the notification.
   *
   * @param settings The settings
   * @param update A truthy value means to update the running service.
   */
  private void configure(ReadableMap settings, boolean update)
  {
//    if (update) {
//      updateNotification();
//    }
  }

  @ReactMethod
  public void startCountdown() {
    final Handler h = new Handler();
    final Runnable r = new Runnable() {
      @Override
      public void run() {
        System.out.println("counting down" + count);
        count += 1;
        h.postDelayed(this, 1000);
      }
    };
    h.postDelayed(r, 1000);
  }

  /**
   * Bind the activity to a background service and put them into foreground
   * state.
   */
  private void startService()
  {
    Activity context = reactContext.getCurrentActivity();

    if (isDisabled || isBind)
      return;

    Intent intent = new Intent(context, ForegroundService.class);

    try {
      context.bindService(intent, connection, BIND_AUTO_CREATE);
      fireEvent(Event.ACTIVATE, null);
      context.startService(intent);
      System.out.println("start service success");
    } catch (Exception e) {
      System.out.println("start service failure");
      fireEvent(Event.FAILURE, String.format("'%s'", e.getMessage()));
    }

    isBind = true;
  }

  /**
   * Bind the activity to a background service and put them into foreground
   * state.
   */
  private void stopService()
  {
    Activity context = reactContext.getCurrentActivity();
    Intent intent    = new Intent(context, ForegroundService.class);

    if (!isBind) return;

    fireEvent(Event.DEACTIVATE, null);
    context.unbindService(connection);
    context.stopService(intent);

    isBind = false;
  }

  /**
   * Fire vent with some parameters inside the web view.
   *
   * @param event The name of the event
   * @param params Optional arguments for the event
   */
  private void fireEvent (Event event, String params)
  {
//    String eventName = event.name().toLowerCase();
//    Boolean active   = event == Event.ACTIVATE;
//
//    String str = String.format("%s._setActive(%b)",
//            JS_NAMESPACE, active);
//
//    str = String.format("%s;%s.on('%s', %s)",
//            str, JS_NAMESPACE, eventName, params);
//
//    str = String.format("%s;%s.fireEvent('%s',%s);",
//            str, JS_NAMESPACE, eventName, params);
//
//    final String js = str;
//
//    cordova.getActivity().runOnUiThread(() -> webView.loadUrl("javascript:" + js));
  }

  private static ReactApplicationContext reactContext;

  private static final String DURATION_SHORT_KEY = "SHORT";
  private static final String DURATION_LONG_KEY = "LONG";

  BackgroundModule(ReactApplicationContext context) {
    super(context);
    reactContext = context;
    reactContext.addLifecycleEventListener(this);
  }

  @Override
  public String getName() {
    return "BackgroundModule";
  }

  @ReactMethod
  public void show(String message, int duration) {
    Toast.makeText(getReactApplicationContext(), message, duration).show();
  }
}