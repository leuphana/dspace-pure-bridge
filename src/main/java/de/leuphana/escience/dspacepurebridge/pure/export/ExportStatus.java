package de.leuphana.escience.dspacepurebridge.pure.export;

import org.dspace.content.Item;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ExportStatus {

    private final Set<String> itemSyncInfos = ConcurrentHashMap.newKeySet();
    private final Set<String> itemSyncErrors = ConcurrentHashMap.newKeySet();
    private final String dspaceBaseUrl;

    public ExportStatus(String dspaceBaseUrl) {
        this.dspaceBaseUrl = dspaceBaseUrl;
    }

    public String error(Item item, String errorMessage) {
        String error = String.format("Item %s/handle/%s - %s", dspaceBaseUrl, item.getHandle(), errorMessage);
        itemSyncErrors.add(error);
        return error;
    }

    public String success(Item item, ExportResult uploadResult, boolean isDoublet) {
        String info;

        if (isDoublet) {
            info = String.format(
                "Item %s/handle/%s already existed in Pure, therefore not synced (pure uuid=%s, portal url=%s)",
                dspaceBaseUrl,
                item.getHandle(), uploadResult.getUuid(), uploadResult.getPortalUrl());
        } else {
            info = String.format("Item %s/handle/%s successfully synced (pure uuid=%s, portal url=%s)", dspaceBaseUrl,
                item.getHandle(), uploadResult.getUuid(), uploadResult.getPortalUrl());
        }
        itemSyncInfos.add(info);
        return info;
    }

    public Set<String> getItemSyncInfos() {
        return itemSyncInfos;
    }

    public Set<String> getItemSyncErrors() {
        return itemSyncErrors;
    }
}
