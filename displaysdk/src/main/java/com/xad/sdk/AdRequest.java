package com.xad.sdk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AdRequest {
    /*********************************************************
    *    Gender Enum
    **********************************************************/
    public enum TestType {
        SANDBOX("sandbox"), // For internal testing
        CHANNEL("channel"),
        PRODUCTION("production"); // For production testing

        TestType(String value) {
            this.value = value;
        }

        public final String value;
    }

    public enum Gender {
        MALE("M"),
        FEMALE("F"),
        UNKNOWN("U");

        Gender(String value) {
            this.value = value;
        }
        public final String value;
    }

    private static final class Month{
        private final int monthInt;
        private Month(int m) {
            this.monthInt = m;
        }
        int getMonthInteger() {
            return monthInt;
        }
    }

    public static final Month JAN = new Month(0);
    public static final Month FEB = new Month(1);
    public static final Month MAR = new Month(2);
    public static final Month ARP = new Month(3);
    public static final Month MAY = new Month(4);
    public static final Month JUN = new Month(5);
    public static final Month JUL = new Month(6);
    public static final Month AUG = new Month(7);
    public static final Month SEP = new Month(8);
    public static final Month OCT = new Month(9);
    public static final Month NOV = new Month(10);
    public static final Month DEC = new Month(11);

    /*********************************************************
    *    Above is Enums,
    *    Below is class member variables
    **********************************************************/

    private final Calendar birthday;
    private final Gender gender;
    private final String zipCode;
    private final String city;
    private final String state;
    private final Map<String, String> extras;

    //only use in video
    protected int vmin;
    protected int vmax;

    //for test
    private final boolean isTestMode;
    private final TestType testType;
    private final String testChannelId;

    /*********************************************************
    *     Builder
    **********************************************************/
    public static class Builder{
        private Calendar birthday;
        private Gender gender = Gender.UNKNOWN;
        private String zipCode;
        private String city;
        private String state;
        private Map<String, String> extras;

        //only use in video
        private int vmin = -1;
        private int vmax = -1;

        //for test
        private boolean isTestMode = false;
        @NonNull private TestType testType = TestType.PRODUCTION; //Default test mode is production test mode
        private String testChannelId;

        /*********************************************************
        *    Builder Setters
        **********************************************************/

        public Builder setVideoDuration(int vmin, int vmax) {
            this.vmin = vmin;
            this.vmax = vmax;
            return this;
        }

        public Builder setTestMode(boolean useTesting) {
            this.isTestMode = useTesting;
            return this;
        }

        public Builder setTestType(@Nullable TestType testType, String testChannelId) {
            if(testType != null) {
                this.testType = testType;
                this.testChannelId = testChannelId;
            }
            return this;
        }

        @Deprecated
        public Builder setAge(int age) {
            Calendar calObj = Calendar.getInstance();
            calObj.set(Calendar.YEAR, calObj.get(Calendar.YEAR) - age);
            this.birthday = calObj;
            return this;
        }

        public Builder setGender(@NonNull Gender gender) {
            this.gender = gender;
            return this;
        }

        public Builder setBirthday(Calendar birthday) {
            this.birthday = birthday;
            return this;
        }

        public Builder setBirthday(int year, Month month, int day) {
            Calendar calObj = Calendar.getInstance();
            calObj.set(year, month.getMonthInteger(), day);
            this.birthday = calObj;
            return this;
        }

        public Builder setZipCode(String zipCode) {
            this.zipCode = zipCode;
            return this;
        }

        public Builder setCity(String city) {
            this.city = city;
            return this;
        }

        public Builder setState(String state) {
            this.state = state;
            return this;
        }

        public Builder addExtras(String key, String value) {
            if(this.extras == null) {
                this.extras = new HashMap<>();
            }
            this.extras.put(key, value);
            return this;
        }

        public AdRequest build () {
            return new AdRequest(this);
        }
    }

    /*********************************************************
    *    AdRequest Getters
    **********************************************************/

    public Gender getGender() {
        return this.gender;
    }

    public boolean isTesting() {
        return this.isTestMode;
    }

    public int getAge() {
        if(this.getBirthday() == null) {
            return -1;
        }
        int mob = this.getBirthday().get(Calendar.MONTH);
        int dob = this.getBirthday().get(Calendar.DAY_OF_MONTH);
        int yob = this.getBirthday().get(Calendar.YEAR);

        Calendar calToday = Calendar.getInstance();
        int todayYear = calToday.get(Calendar.YEAR);
        int todayMonth = calToday.get(Calendar.MONTH);
        int todayDay = calToday.get(Calendar.DAY_OF_MONTH);
        int age = todayYear - yob;
        if(mob > todayMonth || (mob == todayMonth && dob > todayDay) ) {
            --age;
        }
        return age;
    }

    @Nullable public Calendar getBirthday() {
        return this.birthday;
    }

    @Nullable public String getZipCode() {
        return zipCode;
    }

    @Nullable public String getCity() {
        return city;
    }

    @Nullable public String getState() {
        return state;
    }

    @Nullable public Map<String, String> getExtras() {
        return this.extras == null? null : Collections.unmodifiableMap(this.extras);
    }

    public int getVmin() {
        return this.vmin;
    }

    public int getVmax() {
        return this.vmax;
    }

    @NonNull TestType getTestType() {
        return this.testType;
    }

    @Nullable String getTestChannelId() {
        return this.testChannelId;
    }

    /*********************************************************
     *    Build AdRequest
     **********************************************************/
    private AdRequest(Builder builder) {
        this.birthday = builder.birthday;
        this.gender = builder.gender;
        this.isTestMode = builder.isTestMode;
        this.zipCode = builder.zipCode;
        this.city = builder.city;
        this.state = builder.state;
        this.extras = builder.extras;

        this.vmin = builder.vmin;
        this.vmax = builder.vmax;

        this.testChannelId = builder.testChannelId;
        this.testType = builder.testType;
    }
}