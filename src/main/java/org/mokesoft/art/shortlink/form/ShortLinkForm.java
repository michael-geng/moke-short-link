package org.mokesoft.art.shortlink.form;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

@DataObject(generateConverter = true)
public class ShortLinkForm {

    private String url;
    //0代表永久，然后数字代表月份
    private String expired = "0";

    public ShortLinkForm(JsonObject jsonObject) {
        url = jsonObject.getString("url");
        expired = jsonObject.getString("expired");
    }

    public ShortLinkForm(String url, String expired) {
        this.url = url;
        if (!StringUtils.isBlank(expired)) {
            this.expired = expired;
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getExpired() {
        return expired;
    }

    public void setExpired(String expiredType) {
        this.expired = expiredType;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        ShortLinkFormConverter.toJson(this, json);
        return json;
    }

    @Override
    public String toString() {
        return "ShortLinkForm{" +
                "url='" + url + '\'' +
                ", expired='" + expired + '\'' +
                '}';
    }
}
