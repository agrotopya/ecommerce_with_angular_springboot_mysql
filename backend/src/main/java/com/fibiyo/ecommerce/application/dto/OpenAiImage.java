package com.fibiyo.ecommerce.application.dto;


public class OpenAiImage {
    private String b64Json;
    private String url;

    public String getB64Json() {
        return b64Json;
    }

    public void setB64Json(String b64Json) {
        this.b64Json = b64Json;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
