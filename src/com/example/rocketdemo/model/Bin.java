package com.example.rocketdemo.model;

import org.json.JSONObject;

public class Bin {

    private boolean isWorking;
    private String hint;
    private String logoUrl;
    private String imageUrl;

    public Bin(final JSONObject json) {
        isWorking = json.optBoolean("works", true);
        hint = json.optString("hint");
        logoUrl = json.optString("logo");
        imageUrl = json.optString("image");
    }

    public boolean isWorking() {
        return isWorking;
    }

    public String getHint() {
        return hint;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

}
