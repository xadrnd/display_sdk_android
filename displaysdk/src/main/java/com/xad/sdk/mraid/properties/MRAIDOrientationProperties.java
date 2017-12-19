package com.xad.sdk.mraid.properties;

import java.util.Arrays;
import java.util.List;

public final class MRAIDOrientationProperties {

    public static final int FORCE_ORIENTATION_PORTRAIT = 0;
    public static final int FORCE_ORIENTATION_LANDSCAPE = 1;
    public static final int FORCE_ORIENTATION_NONE = 2;

    public boolean allowOrientationChange;
    public int forceOrientation;

    public MRAIDOrientationProperties() {
        this(true, FORCE_ORIENTATION_NONE);
    }
    
    public MRAIDOrientationProperties(boolean allowOrientationChange, int forceOrientation) {
        this.allowOrientationChange = allowOrientationChange;
        this.forceOrientation = forceOrientation;
    }
    
    static public int forceOrientationFromString(String name) {
        final List<String> names = Arrays.asList("portrait", "landscape", "none");
        int idx = names.indexOf(name);
        if (idx != -1) {
            return idx;
        }
        // Use none for the default value.
        return FORCE_ORIENTATION_NONE;
    }
    
    public String forceOrientationString() {
        switch (forceOrientation) {
        case FORCE_ORIENTATION_PORTRAIT : return "portrait";
        case FORCE_ORIENTATION_LANDSCAPE: return "landscape";
        case FORCE_ORIENTATION_NONE: return "none";
        default: return "error";
        }
    }

    //TODO only for temporary use. Use MraidController.OrientationProperties instead
    public MRAIDOrientationProperties(OrientationProperties op) {
        this.allowOrientationChange = op.allowOrientationChange;
        this.forceOrientation = forceOrientationFromString(op.forceOrientation.value);
    }
 
}
