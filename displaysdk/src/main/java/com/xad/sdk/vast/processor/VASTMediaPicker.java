//
//  MediaPicker.java
//
//  Created by Harsha Herur on 12/4/13.
//  Copyright (c) 2014 Nexage. All rights reserved.
//

package com.xad.sdk.vast.processor;

import com.xad.sdk.vast.model.VASTMediaFile;

import java.util.List;


public interface VASTMediaPicker {
    VASTMediaFile pickVideo(List<VASTMediaFile> list);
}
