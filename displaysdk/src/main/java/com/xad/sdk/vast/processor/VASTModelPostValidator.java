//
//  VASTModelPostValidator.java
//
//  Copyright (c) 2014 Nexage. All rights reserved.
//

package com.xad.sdk.vast.processor;

import java.util.List;

import android.text.TextUtils;

import com.xad.sdk.utils.Logger;
import com.xad.sdk.vast.model.VASTMediaFile;
import com.xad.sdk.vast.model.VASTModel;

public class VASTModelPostValidator {
    private static final String TAG = "VASTModelPostValidator";
    // This method tries to make sure that there is at least 1 Media file to
    // be used for VASTActivity. Also, if the boolean validateModel is true, it will
    // do additional validations which includes "at least 1 impression tracking url's is required'
    // If any of the above fails, it returns false. The false indicates that you can stop proceeding
    // further to display this on the MediaPlayer.
    
    public static boolean validate(VASTModel model, VASTMediaPicker mediaPicker) {
        Logger.logDebug(TAG, "validate");

        if (!validateModel(model)) {
            Logger.logDebug(TAG, "Validator returns: not valid (invalid model)");
            return false;
        }

        boolean isValid = false;
        
        // Must have a MediaPicker to choose one of the MediaFile element from XML
        if (mediaPicker != null) {
            List<VASTMediaFile> mediaFiles = model.getMediaFiles();
            VASTMediaFile mediaFile = mediaPicker.pickVideo(mediaFiles);
            
            if (mediaFile != null) {
                String url = mediaFile.getValue();
                if (!TextUtils.isEmpty(url)) {
                    isValid = true;
                    // Let's set this value inside VASTModel so that it can be
                    // accessed from VASTPlayer
                    model.setPickedMediaFileURL(url);
                    Logger.logDebug(TAG, "mediaPicker selected mediaFile with URL " + url);
                }
            }

        }
        else {
            Logger.logWarning(TAG, "MediaPicker: We don't have a compatible media file to play.");
        }
        
        Logger.logDebug(TAG, "Validator returns: " + (isValid? "valid":"not valid (no media file)"));
        
        return isValid;
    }

    private static boolean validateModel(VASTModel model) {
        Logger.logDebug(TAG, "validateModel");
        boolean isValid = true;
        
        // There should be at least one impression.
        List<String> impressions = model.getImpressions();
        if (impressions == null || impressions.size() == 0) {
            isValid = false;
        }
        
        // There must be at least one VASTMediaFile object       
        List<VASTMediaFile> mediaFiles = model.getMediaFiles();
        if (mediaFiles == null || mediaFiles.size() == 0) {
            Logger.logDebug(TAG, "Validator error: mediaFile list invalid");
            isValid = false;
        }
              
        return isValid;
    }
}
