/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package de.leuphana.escience.dspacepurebridge;

import java.sql.SQLException;
import java.util.Arrays;

import de.leuphana.escience.dspacepurebridge.pure.export.DSpaceToPure;
import de.leuphana.escience.dspacepurebridge.pure.imports.DSpacePureEntity;
import de.leuphana.escience.dspacepurebridge.pure.imports.PureToDSpace;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
        this.help = commandLine.hasOption('h') || (! this.importData && ! this.exportData);
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

    void syncPureToDSpace() throws SQLException {
        PureToDSpace pureToDSpace = new PureToDSpace(pureWsEndpointBase, pureWsApiKey, handleService,
            itemService, collectionService, workspaceItemService, installItemService, relationshipTypeService,
            relationshipService,
            configurationService);
        pureToDSpace.syncObjects();
    }

    void syncDSpaceToPure(String exportDataHandle, int exportLimit, boolean checkOnly) {
        String dspaceBaseUrl = configurationService.getProperty("dspace.ui.url");

        de.leuphana.escience.dspacepurebridge.pure.DSpaceServicesContainer.Builder builder = new de.leuphana.escience.dspacepurebridge.pure.DSpaceServicesContainer.Builder();
        builder.itemService(itemService)
            .authorizeService(authorizeService)
            .bitstreamService(bitstreamService)
            .configurationService(configurationService)
            .resourcePolicyService(resourcePolicyService)
            .handleService(handleService);

        de.leuphana.escience.dspacepurebridge.pure.DSpaceServicesContainer
            dSpaceServicesContainer = new de.leuphana.escience.dspacepurebridge.pure.DSpaceServicesContainer(builder);

        DSpaceToPure dSpaceToPure = new DSpaceToPure(pureWsEndpointBase, pureWsApiKey, dspaceBaseUrl,
            exportDataHandle, exportLimit, checkOnly, dSpaceServicesContainer);
        dSpaceToPure.syncItems();
    }


    String getPureWsApiKey() {
        return pureWsApiKey;
    }

    String getPureWsEndpointBase() {
        return pureWsEndpointBase;
    }

}