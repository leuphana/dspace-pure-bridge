/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package de.leuphana.escience.pure.export;

import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.leuphana.escience.pure.Constants;
import de.leuphana.escience.pure.DSpaceServicesContainer;
import de.leuphana.escience.pure.generated.ApiException;
import de.leuphana.escience.CLIScriptContextUtils;
import de.leuphana.escience.pure.imports.DSpaceObjectMappings;
import de.leuphana.escience.pure.imports.DSpacePureEntity;
import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class DSpaceToPure {

    static final String mailRecipientsProperty = "leuphana.pure.sync.mail.recipients";


    public static final String DEFAULT_METADATA_VALUE = "default";
    public static final String PURE_AUTHOR_ROLE = "Author";
    public static final String PURE_SUPERVISOR_ROLE = "Supervisor";
    public static final String PURE_REVIEWER_ROLE = "Reviewer";
    private static final Logger log = LoggerFactory.getLogger(DSpaceToPure.class);
    private ExportStatus exportStatus;
    private DSpaceServicesContainer dSpaceServicesContainer;
    private final DSpaceObjectMappings dSpaceObjectMappings = new DSpaceObjectMappings();

    private final List<PublicationExportFilter> filterList = new ArrayList<>();
    private String exportDataHandle;
    private int exportLimit;
    private boolean checkOnly;

    private final Map<String, ExportType> exportTypeHashMap = new HashMap<>();
    private final Map<ExportType, AbstractExport> exporterRegistry = new HashMap<>();


    public DSpaceToPure(String leuphanaPureWsEndpointBase, String leuphanaPureWsApiKey, String dspaceBaseUrl,
                        String exportDataHandle, int exportLimit, boolean checkOnly,
                        DSpaceServicesContainer dSpaceServicesContainer) {
        this.dSpaceServicesContainer = dSpaceServicesContainer;
        this.exportDataHandle = exportDataHandle;
        this.exportLimit = exportLimit;
        this.checkOnly = checkOnly;

        setupFilters();
        setupTypeToExportTypeMap();

        RestTemplate duplicateCheckRestTemplate = new RestTemplateBuilder()
            .requestFactory(HttpComponentsClientHttpRequestFactory.class)
            .connectTimeout(Duration.ofMillis(60000))
            .readTimeout(Duration.ofMillis(60000))
            .defaultHeader("api-key", leuphanaPureWsApiKey).build();

        exportStatus = new ExportStatus(dspaceBaseUrl);
        ResearchOutputExport researchOutputExport =
            new ResearchOutputExport(leuphanaPureWsEndpointBase, leuphanaPureWsApiKey, dSpaceServicesContainer,
                dSpaceObjectMappings, exportStatus, duplicateCheckRestTemplate);
        StudentThesisExport studentThesisExport =
            new StudentThesisExport(leuphanaPureWsEndpointBase, leuphanaPureWsApiKey, dSpaceServicesContainer,
                dSpaceObjectMappings, exportStatus, duplicateCheckRestTemplate);
        try {
            researchOutputExport.init();
            studentThesisExport.init();
            exporterRegistry.put(ExportType.STUDENT_THESIS, studentThesisExport);
            exporterRegistry.put(ExportType.RESEARCH_OUTPUT, researchOutputExport);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    DSpaceToPure() {

    }

    void setupFilters() {
        String[] filterConfigurations =
            dSpaceServicesContainer.getConfigurationService().getArrayProperty("leuphana.pure.sync.filter");
        if (filterConfigurations != null) {
            for (String filterConfiguration : filterConfigurations) {
                filterList.add(
                    PublicationExportFilter.buildPublicationSyncFilterFromConfiguration(filterConfiguration));
            }
        }
    }

    void setupTypeToExportTypeMap() {
        String[] exportTypeConfigurations =
            dSpaceServicesContainer.getConfigurationService().getArrayProperty("leuphana.pure.sync.exportTypeForType");
        if (exportTypeConfigurations != null) {
            for (String filterConfiguration : exportTypeConfigurations) {
                String parts[] = filterConfiguration.split(":");
                if (parts.length == 2) {
                    log.info("Registering ExportType {} for Type {}", parts[1], parts[0]);
                    this.exportTypeHashMap.put(parts[0], ExportType.valueOf(parts[1]));
                }
            }
        }
    }

    void syncItemThread(UUID itemId) {
        Context context = null;

        try {
            context = CLIScriptContextUtils.createReducedContext();
            Item item = dSpaceServicesContainer.getItemService().find(context, itemId);

            if (exportLimit > 0 && exportStatus.getItemSyncInfos().size() >= exportLimit) {
                log.info("Export limit {} reached, will not sync item with handle: {}", exportLimit, item.getHandle());
                CLIScriptContextUtils.closeContext(context);
                return;
            }

            if (StringUtils.isNotEmpty(exportDataHandle) && !exportDataHandle.equals(item.getHandle())) {
                log.info("Item handle {} is not the requested handle: {}", item.getHandle(), exportDataHandle);
                CLIScriptContextUtils.closeContext(context);
                return;
            }

            String itemPureUUID = dSpaceServicesContainer
                .getItemService()
                .getMetadataFirstValue(item,
                    Constants.SCHEME,
                    Constants.ELEMENT,
                    Constants.UUID_QUALIFIER,
                    Item.ANY);
            if (StringUtils.isNotBlank(itemPureUUID)) {
                log.info("Item {} already present in pure (pure uuid = {})", item.getHandle(), itemPureUUID);
                CLIScriptContextUtils.closeContext(context);
                return;
            }

            String typeToSync = null;
            for (PublicationExportFilter publicationExportFilter : filterList) {
                typeToSync =
                    publicationExportFilter.itemIsSyncableForType(item, dSpaceServicesContainer.getItemService());
                if (StringUtils.isNotEmpty(typeToSync)) {
                    break;
                }
            }
            if (StringUtils.isEmpty(typeToSync)) {
                log.info("Item {} not syncable - no filter matched!", item.getHandle());
                CLIScriptContextUtils.closeContext(context);
                return;
            }

            ExportType exportType;
            if (exportTypeHashMap.containsKey(typeToSync)) {
                exportType = exportTypeHashMap.get(typeToSync);
            } else {
                exportType = exportTypeHashMap.get("default");
            }
            if (exportType == null) {
                log.error("Item {} not syncable - no export type defined for type {}!", item.getHandle(), typeToSync);
                CLIScriptContextUtils.closeContext(context);
                return;
            }

            log.info("Syncing item {} to pure", item.getHandle());
            syncDSpaceItemToPure(context, item, typeToSync, exportType);
        } catch (SQLException e) {
            if (context != null) {
                context.abort();
            }
            throw new RuntimeException(e);
        } finally {
            CLIScriptContextUtils.closeContext(context);
        }
    }

    public void syncItems() {
        if (filterList.isEmpty()) {
            log.error("No filters defined! Use property: 'leuphana.pure.sync.filter'");
            return;
        }

        if (StringUtils.isNotEmpty(exportDataHandle)) {
            log.info("Export limited to handle: {}", exportDataHandle);
        }
        if (exportLimit > 0) {
            log.info("Export limited to {} number of successful exports", exportLimit);
        }
        if (checkOnly) {
            log.info("CheckOnly Option given - will not export any item");
        }

        Context context = null;

        try {
            context = CLIScriptContextUtils.createReducedContext();

            log.info("Prepare Maps for Pure object mapping (Persons, Organizations)");
            prepareOrcidToPureMap(context);
            prepareOrganizationNameToPureMap(context);


            log.info("Fetching relevant items");
            Iterator<Item> allItems =
                dSpaceServicesContainer.getItemService().findArchivedByMetadataField(context, "dspace.entity.type",
                    "Publication");
            ExecutorService pureSyncerThreadPool =
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

            while (allItems.hasNext()) {
                Item item = allItems.next();

                pureSyncerThreadPool.execute(() -> {
                    syncItemThread(item.getID());
                });
            }
            pureSyncerThreadPool.shutdown();
            pureSyncerThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            sendErrorEmail(context);
        } catch (SQLException | AuthorizeException | InterruptedException e) {
            if (context != null) {
                context.abort();
            }
            throw new RuntimeException(e);
        } finally {
            CLIScriptContextUtils.closeContext(context);
        }
    }

    private void sendErrorEmail(Context context) {
        if (!exportStatus.getItemSyncInfos().isEmpty() || !exportStatus.getItemSyncErrors().isEmpty()) {
            String[] mailRecipients =
                dSpaceServicesContainer.getConfigurationService().getArrayProperty(mailRecipientsProperty);
            if (mailRecipients != null && mailRecipients.length > 0) {
                log.info("Sending sync status email to: {}", String.join(", ", mailRecipients));
                try {
                    Locale supportedLocale = I18nUtil.getEPersonLocale(context.getCurrentUser());
                    Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "pure_sync_status"));

                    for (String recipient : mailRecipients) {
                        email.addRecipient(recipient);
                    }
                    email.addArgument(StringUtils.join(exportStatus.getItemSyncErrors(), "\r\n"));
                    email.addArgument(StringUtils.join(exportStatus.getItemSyncInfos(), "\r\n"));
                    email.send();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                log.warn("Not sending sync status email, no recipients defined using '{}' property",
                    mailRecipientsProperty);
            }
        }
    }

    void syncDSpaceItemToPure(Context context, Item item,
                              String syncTypeValue, ExportType exportType)
        throws SQLException {

        AbstractExport exporter = exporterRegistry.get(exportType);
        ExportItem exportItem = null;
        try {
            ExportResult exportResult = exporter.checkForDuplicate(item, exportType);
            if (exportResult != null && exportResult.getUuid() != null) {
                markItemAsSynced(context, item, exportResult, true);
                return;
            }
            if (item.getID().equals(UUID.randomUUID())) {
                exportItem = exporter.createExport(context, item, syncTypeValue);
            }
        } catch (ApiException e) {
            log.error(exportStatus.error(item, "HTTP Status: " + e.getCode() + " - " + e.getResponseBody()));
            return;
        }
        if (exportItem != null) {
            System.err.println(exportItem);
            if (checkOnly) {
                log.info("Item {} is ready for export", item.getHandle());
                return;
            }
            ExportResult exportResult;
            try {
                exportResult = exporter.export(exportItem);
            } catch (ApiException e) {
                log.error(exportStatus.error(item, "HTTP Status: " + e.getCode() + " - " + e.getResponseBody()));
                return;
            }
            if (exportResult != null && exportResult.getUuid() != null) {
                markItemAsSynced(context, item, exportResult, false);
            }
        }
    }

    void markItemAsSynced(Context context, Item item, ExportResult exportResult, boolean isDoublet)
        throws SQLException {
        if (exportResult != null && exportResult.getUuid() != null) {
            dSpaceServicesContainer.getItemService()
                .addMetadata(context, item, Constants.SCHEME,
                    Constants.ELEMENT,
                    Constants.UUID_QUALIFIER, null,
                    String.valueOf(exportResult.getUuid()));
            log.info(exportStatus.success(item, exportResult, isDoublet));
        }
    }

    void prepareOrcidToPureMap(Context context) throws SQLException {
        Collection personCollection =
            (Collection) dSpaceServicesContainer.getHandleService().resolveToObject(
                context, DSpacePureEntity.PERSON.getDspacePureEntityCollectionHandle());
        Iterator<Item> entities = dSpaceServicesContainer.getItemService().findByCollection(context, personCollection);
        while (entities.hasNext()) {
            Item item = entities.next();
            List<MetadataValue> orcidMetadata =
                dSpaceServicesContainer.getItemService()
                    .getMetadata(item, "person", "identifier", "orcid", Item.ANY, false);
            List<MetadataValue> pureUUIDMetadata =
                dSpaceServicesContainer.getItemService()
                    .getMetadata(item, Constants.SCHEME,
                        Constants.ELEMENT,
                        Constants.UUID_QUALIFIER,
                        Item.ANY, false);
            if (!pureUUIDMetadata.isEmpty() && !orcidMetadata.isEmpty()) {
                dSpaceObjectMappings.getOrcidToPureMap().put(orcidMetadata.get(0).getValue(), item);
            }
        }
    }

    void prepareOrganizationNameToPureMap(Context context) throws SQLException {
        Collection orgUnitCollection =
            (Collection) dSpaceServicesContainer
                .getHandleService()
                .resolveToObject(context,
                    DSpacePureEntity.ORGANIZATION.getDspacePureEntityCollectionHandle());
        Iterator<Item> entities = dSpaceServicesContainer.getItemService().findByCollection(context, orgUnitCollection);
        while (entities.hasNext()) {
            Item item = entities.next();
            List<MetadataValue> orgUnitNameMetadata =
                dSpaceServicesContainer.getItemService()
                    .getMetadata(item, "organization", "legalName", null, Item.ANY, false);
            List<MetadataValue> pureUUIDMetadata =
                dSpaceServicesContainer.getItemService()
                    .getMetadata(item, Constants.SCHEME,
                        Constants.ELEMENT,
                        Constants.UUID_QUALIFIER,
                        Item.ANY, false);
            if (!pureUUIDMetadata.isEmpty() && !orgUnitNameMetadata.isEmpty()) {
                dSpaceObjectMappings.getOrganizationNameToPureMap().put(orgUnitNameMetadata.get(0).getValue(),
                    UUID.fromString(
                        pureUUIDMetadata.get(0).getValue()));
            }
        }
    }
}
