package de.leuphana.escience.dspacepurebridge.pure.apiobjects;

public class PressMediaCoverage {
    private String url;
    private String date;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "PressMediaCoverage{" +
            "url='" + url + '\'' +
            ", date='" + date + '\'' +
            '}';
    }
}
