package com.example.gostambalevpn.utils;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ZarinpalAuthority {
    private ZarinpalAuthorityData data;

    public ZarinpalAuthorityData getData() {
        return data;
    }

    public void setData(ZarinpalAuthorityData data) {
        this.data = data;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ZarinpalAuthorityData{
        private String authority;
        private String fee;
        private String fee_type;
        private int code;
        private String message;

        public String getAuthority() {
            return authority;
        }

        public void setAuthority(String authority) {
            this.authority = authority;
        }

        public String getFee() {
            return fee;
        }

        public void setFee(String fee) {
            this.fee = fee;
        }

        public String getFee_type() {
            return fee_type;
        }

        public void setFee_type(String fee_type) {
            this.fee_type = fee_type;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}