/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package de.leuphana.escience.pure;

import java.sql.SQLException;
import java.util.Arrays;

import de.leuphana.escience.pure.export.DSpaceToPure;
import de.leuphana.escience.pure.imports.DSpacePureEntity;
import de.leuphana.escience.pure.imports.PureToDSpace;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
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


public class PureSyncCLI {
    private static final Logger log = LoggerFactory.getLogger(PureSyncCLI.class);
    private static final String leuphanaPureWsEndpointBaseProperty = "leuphana.pure.ws.endpoint.base";
    private static final String leuphanaPureWsApiKeyProperty = "leuphana.pure.ws.apikey";

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


    private String leuphanaPureWsEndpointBase;
    private String leuphanaPureWsApiKey;
    private boolean help;
    private boolean importData;
    private boolean exportData;
    private int exportLimit;
    private boolean checkOnly;
    private String exportHandle;

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        PureSyncCLI pureSyncCLI = new PureSyncCLI();
        try {
            CommandLine commandLine = (new DefaultParser()).parse(PureSyncCLIConfiguration.getOptions(), args);
            pureSyncCLI.setup(commandLine);
            pureSyncCLI.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setup(CommandLine commandLine) throws ParseException {
        this.importData = commandLine.hasOption('i');
        this.exportData = commandLine.hasOption('e');
        this.help = commandLine.hasOption('h') || (! this.importData && ! this.exportData);
        this.exportLimit = commandLine.hasOption('l') ? Integer.parseInt(commandLine.getOptionValue('l')) : 0;
        this.checkOnly = commandLine.hasOption('c');
        this.exportHandle = commandLine.getOptionValue('x');

        leuphanaPureWsEndpointBase = configurationService.getProperty(leuphanaPureWsEndpointBaseProperty);
        if (StringUtils.isEmpty(leuphanaPureWsEndpointBase)) {
            throw new IllegalStateException(leuphanaPureWsEndpointBaseProperty + "Property must be set!");
        }
        if (!leuphanaPureWsEndpointBase.endsWith("/")) {
            leuphanaPureWsEndpointBase += "/";
        }
        leuphanaPureWsApiKey = configurationService.getProperty(leuphanaPureWsApiKeyProperty);
        if (StringUtils.isEmpty(leuphanaPureWsApiKey)) {
            throw new IllegalStateException(leuphanaPureWsApiKeyProperty + " Property must be set!");
        }
    }

    private void run() throws Exception {
        if (help) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("/dspace/bin/dspace dsrun de.leuphana.escience.pure.PureSyncCLI", PureSyncCLIConfiguration.getOptions());
            return;
        }
        if (importData) {
            log.info("Importing Data ({}) from Pure", Arrays.toString(DSpacePureEntity.values()));
            syncPureToDSpace();
        }
        if (exportData) {
            log.info("Exporting Data to Pure");
            syncDSpaceToPure(exportHandle, exportLimit, checkOnly);
        }
    }

    private void syncPureToDSpace() throws SQLException {
        PureToDSpace pureToDSpace = new PureToDSpace(leuphanaPureWsEndpointBase, leuphanaPureWsApiKey, handleService,
            itemService, collectionService, workspaceItemService, installItemService, relationshipTypeService,
            relationshipService,
            configurationService);
        pureToDSpace.syncObjects();
    }

    private void syncDSpaceToPure(String exportDataHandle, int exportLimit, boolean checkOnly) {
        String dspaceBaseUrl = configurationService.getProperty("dspace.ui.url");

        DSpaceServicesContainer.Builder builder = new DSpaceServicesContainer.Builder();
        builder.itemService(itemService)
            .authorizeService(authorizeService)
            .bitstreamService(bitstreamService)
            .configurationService(configurationService)
            .resourcePolicyService(resourcePolicyService)
            .handleService(handleService);

        DSpaceServicesContainer dSpaceServicesContainer = new DSpaceServicesContainer(builder);

        DSpaceToPure dSpaceToPure = new DSpaceToPure(leuphanaPureWsEndpointBase, leuphanaPureWsApiKey, dspaceBaseUrl,
            exportDataHandle, exportLimit, checkOnly, dSpaceServicesContainer);
        dSpaceToPure.syncItems();
    }
}