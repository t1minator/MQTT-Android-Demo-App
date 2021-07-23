package com.internetofhomethings.mqttdemo;


public class CaptureMessage {

    public CaptureMessage(){

    }
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Fields getFields() {
        return fields;
    }

    public void setFields(Fields fields) {
        this.fields = fields;
    }

    Fields fields;
    int version;
    String operation;
    String model;






}
