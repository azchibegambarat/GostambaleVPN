package com.example.gostambalevpn.utils;


public class ZarinpalPost {
    private String merchant_id;
    private int amount;
    private String callback_url;
    private String description;

    public ZarinpalPost() {
    }

    public ZarinpalPost(String merchant_id, int amount, String callback_url, String description) {
        this.merchant_id = merchant_id;
        this.amount = amount;
        this.callback_url = callback_url;
        this.description = description;
    }

    public String getMerchant_id() {
        return merchant_id;
    }

    public void setMerchant_id(String merchant_id) {
        this.merchant_id = merchant_id;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getCallback_url() {
        return callback_url;
    }

    public void setCallback_url(String callback_url) {
        this.callback_url = callback_url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}