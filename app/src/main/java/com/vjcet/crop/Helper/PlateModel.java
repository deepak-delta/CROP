package com.vjcet.crop.Helper;

public class PlateModel {

    private String Url;
    private String Time;

    public PlateModel() {

    }


    public PlateModel(String url, String time) {
        Url = url;
        Time = time;
    }

    public String getUrl() {
        return Url;
    }

    public void setUrl(String url) {
        Url = url;
    }

    public String getTime() {
        return Time;
    }

    public void setTime(String time) {
        Time = time;
    }
}
