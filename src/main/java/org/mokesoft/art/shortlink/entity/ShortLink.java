package org.mokesoft.art.shortlink.entity;


public class ShortLink {

    private Long id;
    private String originUrl;
    private String hash;
    private Long expired;

    private Boolean isNew = true;


    public ShortLink() {
    }

    public ShortLink(String originUrl, String hash, Long expired) {
        this.originUrl = originUrl;
        this.hash = hash;
        this.expired = expired;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginUrl() {
        return originUrl;
    }

    public void setOriginUrl(String originUrl) {
        this.originUrl = originUrl;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Long getExpired() {
        return expired;
    }

    public void setExpired(Long expired) {
        this.expired = expired;
    }

    public Boolean getNew() {
        return isNew;
    }

    public void setNew(Boolean aNew) {
        isNew = aNew;
    }
}
