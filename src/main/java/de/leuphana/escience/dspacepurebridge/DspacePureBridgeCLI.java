package de.leuphana.escience.dspacepurebridge;

import de.leuphana.escience.dspacepurebridge.pure.export.DSpaceToPure;
import de.leuphana.escience.dspacepurebridge.pure.imports.DSpacePureEntity;
import de.leuphana.escience.dspacepurebridge.pure.imports.PureToDSpace;
import de.leuphana.escience.dspacepurebridge.search.ItemFinder;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.core.Context;
import org.dspace.discovery.*;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;


public class DspacePureBridgeCLI {
    private static final Logger log = LoggerFactory.getLogger(DspacePureBridgeCLI.class);
    static final String PURE_BRIDGE_PURE_WS_ENDPOINT_BASE = "dspace-pure-bridge.pure.ws.endpoint.base";
    static final String PURE_BRIDGE_PURE_WS_APIKEY = "dspace-pure-bridge.pure.ws.apikey";

    private final HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    private final InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    private final RelationshipTypeService relationshipTypeService =
            ContentServiceFactory.getInstance().getRelationshipTypeService();
    private final RelationshipService relationshipService =
            ContentServiceFactory.getInstance().getRelationshipService();
    private final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private final WorkspaceItemService workspaceItemService =
            ContentServiceFactory.getInstance().getWorkspaceItemService();
    private final ConfigurationService configurationService =
            DSpaceServicesFactory.getInstance().getConfigurationService();
    private final AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    private final ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance()
            .getResourcePolicyService();
    private final SearchService searchService = SearchUtils.getSearchService();
    private final ItemFinder itemFinder = new ItemFinder();

    private String pureWsEndpointBase;
    private String pureWsApiKey;
    private boolean help;
    private boolean importData;
    private boolean exportData;
    private int exportLimit;
    private boolean checkOnly;
    private String exportHandle;

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        DspacePureBridgeCLI dspacePureBridgeCLI = new DspacePureBridgeCLI();
        try {
            CommandLine commandLine = (new DefaultParser()).parse(
                    de.leuphana.escience.dspacepurebridge.PureSyncCLIConfiguration.getOptions(), args);
            dspacePureBridgeCLI.setup(commandLine);
            dspacePureBridgeCLI.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void setup(CommandLine commandLine) {
        this.importData = commandLine.hasOption('i');
        this.exportData = commandLine.hasOption('e');
        this.help = commandLine.hasOption('h') || (!this.importData && !this.exportData);
        this.exportLimit = commandLine.hasOption('l') ? Integer.parseInt(commandLine.getOptionValue('l')) : 0;
        this.checkOnly = commandLine.hasOption('c');
        this.exportHandle = commandLine.getOptionValue('x');

        pureWsEndpointBase = configurationService.getProperty(PURE_BRIDGE_PURE_WS_ENDPOINT_BASE);
        if (StringUtils.isEmpty(pureWsEndpointBase)) {
            throw new IllegalStateException(PURE_BRIDGE_PURE_WS_ENDPOINT_BASE + " Property must be set!");
        }
        if (!pureWsEndpointBase.endsWith("/")) {
            pureWsEndpointBase += "/";
        }
        pureWsApiKey = configurationService.getProperty(PURE_BRIDGE_PURE_WS_APIKEY);
        if (StringUtils.isEmpty(pureWsApiKey)) {
            throw new IllegalStateException(PURE_BRIDGE_PURE_WS_APIKEY + " Property must be set!");
        }

        if (!checkDSpaceConfig()) {
            throw new IllegalStateException("Dspace config check failed!");
        }
    }

    void run() throws Exception {
        if (help) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("/dspace/bin/dspace dsrun de.leuphana.escience.dspacepurebridge.PureSyncCLI", PureSyncCLIConfiguration.getOptions());
            return;
        }
        if (importData) {
            if (log.isInfoEnabled()) {
                log.info("Importing Data ({}) from Pure", Arrays.toString(DSpacePureEntity.values()));
            }
            syncPureToDSpace();
        }
        if (exportData) {
            log.info("Exporting Data to Pure");
            syncDSpaceToPure(exportHandle, exportLimit, checkOnly);
        }
    }

    private boolean checkDSpaceConfig() {
        boolean validConfig = true;

        for (String requiredProperty : List.of(
                "event.dispatcher." + CLIScriptContextUtils.DISPATCHER_SET_NAME + ".class",
                "event.dispatcher." + CLIScriptContextUtils.DISPATCHER_SET_NAME + ".consumers")) {
            if (!configurationService.hasProperty(requiredProperty)) {
                log.error("DSpace configuration is missing required property: {}", requiredProperty);
                validConfig = false;
            }
        }
        return validConfig;
    }

    void syncPureToDSpace() throws SQLException, SearchServiceException {

        DSpaceServicesContainer.Builder builder = new DSpaceServicesContainer.Builder();
        builder.itemService(itemService)
                .configurationService(configurationService)
                .collectionService(collectionService)
                .workspaceItemService(workspaceItemService)
                .installItemService(installItemService)
                .relationshipTypeService(relationshipTypeService)
                .relationshipService(relationshipService)
                .handleService(handleService)
                .searchService(searchService);

        DSpaceServicesContainer
                dSpaceServicesContainer = new DSpaceServicesContainer(builder);

        PureToDSpace pureToDSpace = new PureToDSpace(pureWsEndpointBase, pureWsApiKey, dSpaceServicesContainer, itemFinder);
        pureToDSpace.syncObjects();
    }

    void syncDSpaceToPure(String exportDataHandle, int exportLimit, boolean checkOnly) {
        String dspaceBaseUrl = configurationService.getProperty("dspace.ui.url");

        DSpaceServicesContainer.Builder builder = new DSpaceServicesContainer.Builder();
        builder.itemService(itemService)
                .authorizeService(authorizeService)
                .bitstreamService(bitstreamService)
                .configurationService(configurationService)
                .resourcePolicyService(resourcePolicyService)
                .handleService(handleService)
                .searchService(searchService);

        DSpaceServicesContainer
                dSpaceServicesContainer = new DSpaceServicesContainer(builder);

        DSpaceToPure dSpaceToPure = new DSpaceToPure(pureWsEndpointBase, pureWsApiKey, dspaceBaseUrl,
                exportDataHandle, exportLimit, checkOnly, dSpaceServicesContainer, itemFinder);
        dSpaceToPure.syncItems();
    }


    String getPureWsApiKey() {
        return pureWsApiKey;
    }

    String getPureWsEndpointBase() {
        return pureWsEndpointBase;
    }

}