//
//  VAST_DOC_ELEMENTS.java
//
//  Copyright (c) 2014 Nexage. All rights reserved.
//


package com.xad.sdk.vast.model;

public enum VAST_DOC_ELEMENTS {

    vastVersion ("2.0"),
    vasts ("VASTS"),
    vastAdTagURI ("VASTAdTagURI"),
    vastVersionAttribute ("version");

    private final String value;

    VAST_DOC_ELEMENTS(String value) {
        this.value = value;

    }

    public String getValue() {
        return value;
    }

}
