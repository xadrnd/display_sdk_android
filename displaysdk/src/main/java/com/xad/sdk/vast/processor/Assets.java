package com.xad.sdk.vast.processor;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Ray.Wu on 4/13/16.
 */
public class Assets {
    public static String readFromAssets(Context context, String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(filename), "UTF-8"));

        // do reading, usually loop until end of file reading
        StringBuilder sb = new StringBuilder();
        String mLine = reader.readLine();
        while (mLine != null) {
            sb.append(mLine); // process line
            sb.append("\n");
            mLine = reader.readLine();
        }
        reader.close();
        return sb.toString();
    }

    public static InputStream inputStreamFromAssets(Context context, String filename) throws IOException {
        return context.getAssets().open(filename);
    }
}
