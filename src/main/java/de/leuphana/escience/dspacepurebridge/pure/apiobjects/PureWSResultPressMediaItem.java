package de.leuphana.escience.dspacepurebridge.pure.apiobjects;

import java.util.List;
import java.util.Map;

public class PureWSResultPressMediaItem extends PureWSResultItem {

    private Map<String, String> title;
    private List<PressMediaCoverage> mediaCoverages;

    public Map<String, String> getTitle() {
        return title;
    }

    public void setTitle(Map<String, String> title) {
        this.title = title;
    }

    public List<PressMediaCoverage> getMediaCoverages() {
        return mediaCoverages;
    }

    public void setMediaCoverages(List<PressMediaCoverage> mediaCoverages) {
        this.mediaCoverages = mediaCoverages;
    }

    @Override
    public String toString() {
        return "PureWSResultPressMediaItem{" + "title=" + title + ", mediaCoverages=" + mediaCoverages + '}';
    }
}
