package de.leuphana.escience.dspacepurebridge.pure.apiobjects;

public class Link {
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "Link{" +
            "url='" + url + '\'' +
            '}';
    }
}
