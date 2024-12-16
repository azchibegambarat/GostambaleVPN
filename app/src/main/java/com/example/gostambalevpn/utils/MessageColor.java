package com.example.gostambalevpn.utils;

import androidx.annotation.ColorInt;

public class MessageColor {
    @ColorInt
    private int color;
    private String message;

    public MessageColor() {
    }

    public MessageColor(String message, @ColorInt int color) {
        System.err.printf("color:%4x%n\n msg:%s", color, message);
        this.color = color;
        this.message = message;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
