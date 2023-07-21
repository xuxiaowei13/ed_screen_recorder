package com.ed_screen_recorder.ed_screen_recorder;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderCodecInfo;
import com.hbisoft.hbrecorder.HBRecorderListener;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * EdScreenRecorderPlugin
 */
public class EdScreenRecorderPlugin implements FlutterPlugin, ActivityAware, MethodCallHandler,
        HBRecorderListener,PluginRegistry.ActivityResultListener, PluginRegistry.RequestPermissionsResultListener {

    private FlutterPluginBinding flutterPluginBinding;
    private ActivityPluginBinding activityPluginBinding;
    Result recentResult;
    Result startRecordingResult;
    Result stopRecordingResult;
    Result pauseRecordingResult;
    Result resumeRecordingResult;
    Activity activity;
    private static final int SCREEN_RECORD_REQUEST_CODE = 777;
    private HBRecorder hbRecorder;
    boolean isAudioEnabled;
    String fileName;

    String completeFileName;
    String dirPathToSave;
    boolean addTimeCode;
    String filePath;
    int videoFrame;
    int videoBitrate;
    String fileOutputFormat;
    String fileExtension;
    boolean success;
    String videoHash;
    long startDate;
    long endDate;
    int width;
    int height;

    boolean micPermission = false;
    boolean mediaPermission = false;

    private MethodChannel channel;

    private void initializeResults() {
        startRecordingResult = null;
        stopRecordingResult = null;
        pauseRecordingResult = null;
        resumeRecordingResult = null;
        recentResult = null;
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        this.flutterPluginBinding = binding;
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "ed_screen_recorder");
        channel.setMethodCallHandler(this);
        hbRecorder = new HBRecorder(flutterPluginBinding.getApplicationContext(), this);
        HBRecorderCodecInfo hbRecorderCodecInfo = new HBRecorderCodecInfo();
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        this.flutterPluginBinding = null;
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.activityPluginBinding = binding;
        setupActivity(binding.getActivity());
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        initializeResults();
        recentResult = result;
        switch (call.method) {
            case "startRecordScreen":
                try {
                    startRecordingResult = result;
                    isAudioEnabled = Boolean.TRUE.equals(call.argument("audioenable"));
                    fileName = call.argument("filename");
                    dirPathToSave = call.argument("dirpathtosave");
                    addTimeCode = Boolean.TRUE.equals(call.argument("addtimecode"));
                    videoFrame = call.argument("videoframe");
                    videoBitrate = call.argument("videobitrate");
                    fileOutputFormat = call.argument("fileoutputformat");
                    fileExtension = call.argument("fileextension");
                    videoHash = call.argument("videohash");
                    width = call.argument("width");
                    height = call.argument("height");
                    customSettings(videoFrame, videoBitrate, fileOutputFormat, addTimeCode, fileName, width, height);

                    if (isAudioEnabled) {
                        if (ContextCompat.checkSelfPermission(flutterPluginBinding.getApplicationContext(), Manifest.permission.RECORD_AUDIO)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(activity,
                                    new String[]{Manifest.permission.RECORD_AUDIO},
                                    333);
                        } else {

                            micPermission = true;
                        }
                    } else {
                        micPermission = true;
                    }

                    // if (ContextCompat.checkSelfPermission(flutterPluginBinding.getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    //         != PackageManager.PERMISSION_GRANTED) {

                    //     ActivityCompat.requestPermissions(activity,
                    //             new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    //             444);
                    // } else {
                        mediaPermission = true;
                    // }
                    if (micPermission && mediaPermission) {
                        success = startRecordingScreen();
                    }

                } catch (Exception e) {
                    Map<Object, Object> dataMap = new HashMap<Object, Object>();
                    dataMap.put("success", false);
                    dataMap.put("isProgress", false);
                    dataMap.put("file", "");
                    dataMap.put("eventname", "startRecordScreen Error");
                    dataMap.put("message", e.getMessage());
                    dataMap.put("videohash", videoHash);
                    dataMap.put("startdate", startDate);
                    dataMap.put("enddate", endDate);
                    JSONObject jsonObj = new JSONObject(dataMap);
                    startRecordingResult.success(jsonObj.toString());
                    startRecordingResult = null;
                    recentResult = null;
                }
                break;
            case "pauseRecordScreen":
                pauseRecordingResult = result;
                hbRecorder.pauseScreenRecording();
                break;
            case "resumeRecordScreen":
                resumeRecordingResult = result;
                hbRecorder.resumeScreenRecording();
                break;
            case "stopRecordScreen":
                stopRecordingResult = result;
                hbRecorder.stopScreenRecording();
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 333) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                micPermission = true;
            } else {
                micPermission = false;
            }
        } else if (requestCode == 444) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mediaPermission = true;
            } else {
                mediaPermission = false;
            }
        }
        return true;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    try {
                        if (dirPathToSave != null) {
                            setOutputPath(addTimeCode, fileName, dirPathToSave);
                        }
                    } catch (Exception e) {
                        Map<Object, Object> dataMap = new HashMap<Object, Object>();
                        dataMap.put("success", false);
                        dataMap.put("isProgress", false);
                        dataMap.put("file", "");
                        dataMap.put("eventname", "startRecordScreen Error");
                        dataMap.put("message", e.getMessage());
                        dataMap.put("videohash", videoHash);
                        dataMap.put("startdate", startDate);
                        dataMap.put("enddate", endDate);
                        JSONObject jsonObj = new JSONObject(dataMap);
                        startRecordingResult.success(jsonObj.toString());
                        startRecordingResult = null;
                        recentResult = null;
                    }
                    
                    hbRecorder.startScreenRecording(data, resultCode);
                }
            }
        }
        return true;
    }

    private void setupActivity(Activity activity) {
        if (activityPluginBinding != null) {
            activityPluginBinding.addActivityResultListener(this);
        }
        this.activity = activity;
    }

    @Override
    public void HBRecorderOnStart() {

        Log.e("Video Start:", "Start called");
        Map<Object, Object> dataMap = new HashMap<Object, Object>();
        dataMap.put("success", success);
        dataMap.put("isProgress", true);
        if (dirPathToSave != null) {
            dataMap.put("file", filePath + "." + fileExtension);
        } else {
            dataMap.put("file", getCompleteFileName(fileName, addTimeCode) + "." + fileExtension);
        }
        dataMap.put("eventname", "startRecordScreen");
        dataMap.put("message", "Started Video");
        dataMap.put("videohash", videoHash);
        startDate = System.currentTimeMillis();
        dataMap.put("startdate", startDate);
        dataMap.put("enddate", endDate);
        JSONObject jsonObj = new JSONObject(dataMap);
        if (startRecordingResult != null) {
            startRecordingResult.success(jsonObj.toString());
            startRecordingResult = null;
            recentResult = null;
        }
    }

    @Override
    public void HBRecorderOnComplete() {
        Log.e("Video Complete:", "Complete called");
        Map<Object, Object> dataMap = new HashMap<Object, Object>();
        dataMap.put("success", success);
        dataMap.put("isProgress", false);
        if (dirPathToSave != null) {
            dataMap.put("file", filePath + "." + fileExtension);
        } else {
            dataMap.put("file", getCompleteFileName(fileName, addTimeCode) + "." + fileExtension);
        }
        dataMap.put("eventname", "stopRecordScreen");
        dataMap.put("message", "Paused Video");
        dataMap.put("videohash", videoHash);
        dataMap.put("startdate", startDate);
        endDate = System.currentTimeMillis();
        dataMap.put("enddate", endDate);
        JSONObject jsonObj = new JSONObject(dataMap);
        try {
            if (stopRecordingResult != null) {
                stopRecordingResult.success(jsonObj.toString());
                stopRecordingResult = null;
                recentResult = null;
            }
        } catch (Exception e) {
            if (recentResult != null) {
                recentResult.error("Error", e.getMessage(), null);
                recentResult = null;
            }
        }
    }

    @Override
    public void HBRecorderOnError(int errorCode, String reason) {
        Log.e("Video Error:", reason);
        if (recentResult != null) {
            recentResult.error("Error", reason, null);
            recentResult = null;
        }
    }

    @Override
    public void HBRecorderOnPause() {
        if (pauseRecordingResult != null) {
            pauseRecordingResult.success(true);
            pauseRecordingResult = null;
            recentResult = null;
        }
    }

    @Override
    public void HBRecorderOnResume() {
        if (resumeRecordingResult != null) {
            resumeRecordingResult.success(true);
            resumeRecordingResult = null;
            recentResult = null;
        }
    }

    private Boolean startRecordingScreen() {

        try {
            hbRecorder.enableCustomSettings();
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) flutterPluginBinding
                    .getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent permissionIntent = mediaProjectionManager != null
                    ? mediaProjectionManager.createScreenCaptureIntent()
                    : null;
            if (activity == null) {
                Log.e("startRecordingScreen:", "activity is null");
            }
            activity.startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void customSettings(int videoFrame, int videoBitrate, String fileOutputFormat, boolean addTimeCode,
                                String fileName, int width, int height) {
        hbRecorder.isAudioEnabled(isAudioEnabled);
        hbRecorder.setAudioSource("DEFAULT");
        hbRecorder.setVideoEncoder("DEFAULT");
        hbRecorder.setVideoFrameRate(videoFrame);
        hbRecorder.setVideoBitrate(videoBitrate);
        hbRecorder.setOutputFormat(fileOutputFormat);
        if (width != 0 && height != 0) {
            hbRecorder.setScreenDimensions(height, width);
        }
        if (dirPathToSave == null) {
            hbRecorder.setFileName(getCompleteFileName(fileName, addTimeCode));
        }
    }

    private void setOutputPath(boolean addTimeCode, String fileName, String dirPathToSave) throws IOException {
        hbRecorder.setFileName(getCompleteFileName(fileName, addTimeCode));
        if (dirPathToSave != null && !dirPathToSave.equals("")) {
            File dirFile = new File(dirPathToSave);
            hbRecorder.setOutputPath(dirFile.getAbsolutePath());
            filePath = dirFile.getAbsolutePath() + "/" + getCompleteFileName(fileName, addTimeCode);
        } else {
            hbRecorder.setOutputPath(
                    flutterPluginBinding.getApplicationContext().getExternalCacheDir().getAbsolutePath());
            filePath = flutterPluginBinding.getApplicationContext().getExternalCacheDir().getAbsolutePath() + "/"
                    + getCompleteFileName(fileName, addTimeCode);
        }

    }

    private String getCompleteFileName(String fileName, boolean addTimeCode) {
        if (completeFileName != null) return completeFileName;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault());
        Date curDate = new Date(System.currentTimeMillis());
        if (addTimeCode) {
            completeFileName = fileName + "-" + formatter.format(curDate).replace(" ", "");
        } else {
            completeFileName = fileName;
        }
        return completeFileName;
    }
}
