package com.xad.sdk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;

import com.xad.sdk.mraid.VideoPlayerActivity;
import com.xad.sdk.utils.Constants;
import com.xad.sdk.utils.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MRAIDNativeFeatureProvider {

    private static final String TAG = "MRAIDNativeFeatureProvider";

    public static void callTel(Context context, String url) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            callTelWithoutPermission(context, url);
        } else {
            callTelWithPermission(context, url);
        }
    }

    private static void callTelWithoutPermission(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
        context.startActivity(intent);
    }


    @SuppressWarnings("MissingPermission")
    private static void callTelWithPermission(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(url));
        context.startActivity(intent);
    }

    @SuppressLint("SimpleDateFormat")
    public static void createCalendarEvent(Context context, String eventJSON) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Logger.logError(TAG, "No permission to create calender event");
        }
        try {
            // Need to fix some of the encoded string from JS
            eventJSON = eventJSON.replace("\\", "").replace("\"{", "{").replace("}\"", "}");
            JSONObject jsonObject = new JSONObject(eventJSON);

            String description = jsonObject.optString("description", "Untitled");
            String location = jsonObject.optString("location", "unknown");
            String summary = jsonObject.optString("summary");

            /*
             * NOTE: The Java SimpleDateFormat class will not work as is with the W3C spec for
             * calendar entries. The problem is that the W3C spec has time zones (UTC offsets)
             * containing a colon like this:
             *   "2014-12-21T12:34-05:00"
             * The SimpleDateFormat parser will choke on the colon. It wants something like this:
             *   "2014-12-21T12:34-0500"
             *
             * Also, the W3C spec indicates that seconds are optional, so we have to use two patterns
             * to be able to parse both this:
             *   "2014-12-21T12:34-0500"
             * and this:
             *   "2014-12-21T12:34:56-0500"
             */

            String[] patterns = {
                    "yyyy-MM-dd'T'HH:mmZ",
                    "yyyy-MM-dd'T'HH:mm:ssZ",
            };

            String[] dateStrings = new String[2];
            dateStrings[0] = jsonObject.getString("start");
            dateStrings[1] = jsonObject.optString("end");

            long startTime = 0;
            long endTime = 0;

            for (int i = 0; i < dateStrings.length; i++) {
                if (TextUtils.isEmpty(dateStrings[i])) {
                    continue;
                }
                // remove the colon in the timezone
                dateStrings[i] = dateStrings[i].replaceAll("([+-]\\d\\d):(\\d\\d)$", "$1$2");
                for (String pattern : patterns) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                        if (i == 0) {
                            startTime = sdf.parse(dateStrings[i]).getTime();
                        } else {
                            endTime = sdf.parse(dateStrings[i]).getTime();
                        }
                        break;
                    } catch (ParseException e) {
                        continue;
                    }
                }
            }

            /*
            boolean wholeDay = false;
            if (jObject.getJSONObject("recurrence") != null) {
            JSONObject recurrence = jObject.getJSONObject("recurrence");
            if (recurrence.getString("frequency") != null) {
            wholeDay = recurrence.getString("frequency").toLowerCase().equals("daily");
            }
            }
            */

            Intent intent = new Intent(Intent.ACTION_INSERT).setType("vnd.android.cursor.item/event");
            intent.putExtra(Events.TITLE, description);
            intent.putExtra(Events.DESCRIPTION, summary);
            intent.putExtra(Events.EVENT_LOCATION, location);

            if (startTime > 0) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime);
            }

            if (endTime > 0) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime);
            }

            /*
            if (wholeDay) {
            intent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, wholeDay);
            }
            */

            context.startActivity(intent);
        } catch (JSONException e) {
            Logger.logError(TAG, "Error parsing JSON: " + e.getLocalizedMessage());
        }
    }

    public static void playVideo(Context context, String url) {
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        intent.putExtra(Constants.KEY_VIDEO_DATA, url);
        context.startActivity(intent);
    }

    public static void openBrowser(Context context, String url) {
        if(context instanceof Activity && Build.VERSION.SDK_INT >= 23) {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(context, Uri.parse(url));
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
        }
    }

    public static void storePicture(final Context context,final String url) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Logger.logError(TAG, "No permission to store image");
            return;
        }
        // Spawn a new thread to download and save the image
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    storePictureInGallery(context, url);
                } catch (Exception e) {
                    Logger.logError(TAG, e.getLocalizedMessage());
                }
            }
            }).start();
    }

    public static void sendSms(Context context, String url) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            sendSmsWithoutPermission(context, url);
        } else {
            sendSmsWithPermission(context, url);
        }
    }

    private static void sendSmsWithoutPermission(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
        context.startActivity(intent);
    }

    private static void sendSmsWithPermission(Context context, String url) {
        //TODO to complete
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
        context.startActivity(intent);
    }

    @SuppressLint("SimpleDateFormat")
    private static void storePictureInGallery(Context context, String url) {
        // Setting up file to write the image to.
        SimpleDateFormat gmtDateFormat = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
        String s =  getAlbumDir() + "/img" + gmtDateFormat.format(new Date()) + ".png";
        Logger.logDebug(TAG, "Saving image into: " + s);
        File f = new File(s);
        // Open InputStream to download the image.
        InputStream is;
        try {
            is = new URL(url).openStream();
            // Set up OutputStream to write data into image file.
            OutputStream os = new FileOutputStream(f);
            copyStream(is, os);
            MediaScannerConnection.scanFile(context,
                    new String[] { f.getAbsolutePath() }, null,
                    new OnScanCompletedListener() {

                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            Logger.logDebug(TAG, "File saves successfully to " + path);
                        }
                    });
            Logger.logInfo(TAG, "Saved image successfully");
        } catch (MalformedURLException e) {
            Logger.logError(TAG, "Not able to save image due to invalid URL: " + e.getLocalizedMessage());
        } catch (IOException e) {
            Logger.logError(TAG, "Unable to save image: " + e.getLocalizedMessage());
        }
    }

    private static void copyStream(InputStream is, OutputStream os) {
        final int buffer_size = 1024;
        try {
            byte[] bytes = new byte[buffer_size];
            for (;;) {
                int count = is.read(bytes, 0, buffer_size);
                if (count == -1) {
                    break;
                }
                os.write(bytes, 0, count);
            }
        } catch (Exception ex) {
            Logger.logError(TAG, "Error saving picture: " + ex.getLocalizedMessage());
        }
    }

    @Nullable
    private static File getAlbumDir() {
        File storageDir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            storageDir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "groundtruth");
            if (!storageDir.mkdirs()) {
                if (!storageDir.exists()) {
                    Logger.logError(TAG, "Failed to create album directory");
                    return null;
                }
            }
        } else {
            Logger.logWarning(TAG, "External storage is not mounted READ/WRITE.");
        }
        return storageDir;
    }
}
