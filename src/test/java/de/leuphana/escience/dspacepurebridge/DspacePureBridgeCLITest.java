package de.leuphana.escience.dspacepurebridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.leuphana.escience.MockedEnvironmentTestMethod;
import org.apache.commons.cli.CommandLine;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.factory.AuthorizeServiceFactoryImpl;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.factory.ContentServiceFactoryImpl;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.factory.HandleServiceFactoryImpl;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.services.factory.DSpaceServicesFactoryImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DspacePureBridgeCLITest {

    @Mock
    CommandLine commandLine;

    @Mock
    HandleService handleService;
    @Mock
    ItemService itemService;
    @Mock
    BitstreamService bitstreamService;
    @Mock
    InstallItemService installItemService;
    @Mock
    RelationshipTypeService relationshipTypeService;
    @Mock
    RelationshipService relationshipService;
    @Mock
    CollectionService collectionService;
    @Mock
    WorkspaceItemService workspaceItemService;
    @Mock
    ConfigurationService configurationService;
    @Mock
    AuthorizeService authorizeService;
    @Mock
    ResourcePolicyService resourcePolicyService;

    @Spy
    @InjectMocks
    HandleServiceFactoryImpl handleServiceFactory;
    @Spy
    @InjectMocks
    DSpaceServicesFactoryImpl dSpaceServicesFactory;
    @Spy
    @InjectMocks
    AuthorizeServiceFactoryImpl authorizeServiceFactory;
    @Spy
    @InjectMocks
    ContentServiceFactoryImpl contentServiceFactory;

    @Test
    void setupEndpointBaseMissing() throws Exception {
        executeTestInMockedEnvironment(() -> {
            IllegalStateException illegalStateException = Assertions.assertThrows(IllegalStateException.class, () -> {
                DspacePureBridgeCLI dspacePureBridgeCLI = new DspacePureBridgeCLI();
                dspacePureBridgeCLI.setup(commandLine);
            });

            assertEquals(DspacePureBridgeCLI.PURE_BRIDGE_PURE_WS_ENDPOINT_BASE + " Property must be set!",
                illegalStateException.getMessage());
        });
    }

    @Test
    void setupAPIKeyMissing() throws Exception {
        executeTestInMockedEnvironment(() -> {
            when(configurationService.getProperty(DspacePureBridgeCLI.PURE_BRIDGE_PURE_WS_ENDPOINT_BASE)).thenReturn(
                "http://localhost:8080/pure/");
            IllegalStateException illegalStateException = Assertions.assertThrows(IllegalStateException.class, () -> {
                DspacePureBridgeCLI dspacePureBridgeCLI = new DspacePureBridgeCLI();
                dspacePureBridgeCLI.setup(commandLine);
            });
            assertEquals(DspacePureBridgeCLI.PURE_BRIDGE_PURE_WS_APIKEY + " Property must be set!",
                illegalStateException.getMessage());
        });
    }


    @Test
    void setup() throws Exception {
        executeTestInMockedEnvironment(() -> {
            String endpointBase = "http://localhost:8080/pure";
            String apiKey = "xy1234";
            when(configurationService.getProperty(DspacePureBridgeCLI.PURE_BRIDGE_PURE_WS_ENDPOINT_BASE)).thenReturn(
                endpointBase);
            when(configurationService.getProperty(DspacePureBridgeCLI.PURE_BRIDGE_PURE_WS_APIKEY)).thenReturn(apiKey);

            DspacePureBridgeCLI dspacePureBridgeCLI = new DspacePureBridgeCLI();
            dspacePureBridgeCLI.setup(commandLine);

            assertEquals(endpointBase + "/", dspacePureBridgeCLI.getPureWsEndpointBase());
            assertEquals(apiKey, dspacePureBridgeCLI.getPureWsApiKey());
        });
    }

    @Test
    void runImport() throws Exception {
        executeTestInMockedEnvironment(() -> {
            String endpointBase = "http://localhost:8080/pure";
            String apiKey = "xy1234";
            when(configurationService.getProperty(DspacePureBridgeCLI.PURE_BRIDGE_PURE_WS_ENDPOINT_BASE)).thenReturn(
                endpointBase);
            when(configurationService.getProperty(DspacePureBridgeCLI.PURE_BRIDGE_PURE_WS_APIKEY)).thenReturn(apiKey);
            when(commandLine.hasOption('i')).thenReturn(true);
            DspacePureBridgeCLI dspacePureBridgeCLI = spy(DspacePureBridgeCLI.class);
            doNothing().when(dspacePureBridgeCLI).syncPureToDSpace();

            dspacePureBridgeCLI.setup(commandLine);
            dspacePureBridgeCLI.run();

            verify(dspacePureBridgeCLI, times(1)).syncPureToDSpace();
            assertEquals(apiKey, dspacePureBridgeCLI.getPureWsApiKey());
        });
    }

    @Test
    void runExportDefaultOptions() throws Exception {
        executeTestInMockedEnvironment(() -> {
            String endpointBase = "http://localhost:8080/pure";
            String apiKey = "xy1234";
            when(configurationService.getProperty(DspacePureBridgeCLI.PURE_BRIDGE_PURE_WS_ENDPOINT_BASE)).thenReturn(
                endpointBase);
            when(configurationService.getProperty(DspacePureBridgeCLI.PURE_BRIDGE_PURE_WS_APIKEY)).thenReturn(apiKey);
            when(commandLine.hasOption('i')).thenReturn(false);
            when(commandLine.hasOption('e')).thenReturn(true);
            DspacePureBridgeCLI dspacePureBridgeCLI = spy(DspacePureBridgeCLI.class);
            doNothing().when(dspacePureBridgeCLI).syncDSpaceToPure(null, 0, false);

            dspacePureBridgeCLI.setup(commandLine);
            dspacePureBridgeCLI.run();

            assertEquals(apiKey, dspacePureBridgeCLI.getPureWsApiKey());
        });
    }


    @Test
    void runExportWithOptions() throws Exception {
        executeTestInMockedEnvironment(() -> {
            String endpointBase = "http://localhost:8080/pure";
            String apiKey = "xy1234";
            String limitToHandle = "123456789/123456789";
            int exportLimit = 100;
            boolean checkOnly = true;

            when(configurationService.getProperty(DspacePureBridgeCLI.PURE_BRIDGE_PURE_WS_ENDPOINT_BASE)).thenReturn(
                endpointBase);
            when(configurationService.getProperty(DspacePureBridgeCLI.PURE_BRIDGE_PURE_WS_APIKEY)).thenReturn(apiKey);
            when(commandLine.hasOption('i')).thenReturn(false);
            when(commandLine.hasOption('e')).thenReturn(true);
            when(commandLine.getOptionValue('x')).thenReturn(limitToHandle);
            when(commandLine.hasOption('l')).thenReturn(true);
            when(commandLine.getOptionValue('l')).thenReturn(String.valueOf(exportLimit));
            when(commandLine.hasOption('c')).thenReturn(checkOnly);
            when(commandLine.hasOption('h')).thenReturn(false);
            DspacePureBridgeCLI dspacePureBridgeCLI = spy(DspacePureBridgeCLI.class);
            doNothing().when(dspacePureBridgeCLI).syncDSpaceToPure(limitToHandle, exportLimit, checkOnly);

            dspacePureBridgeCLI.setup(commandLine);
            dspacePureBridgeCLI.run();

            assertEquals(apiKey, dspacePureBridgeCLI.getPureWsApiKey());
        });
    }

    void executeTestInMockedEnvironment(MockedEnvironmentTestMethod testMethod) throws Exception {
        try (MockedStatic<HandleServiceFactory> handleServiceFactoryMockedStatic = Mockito.mockStatic(
            HandleServiceFactory.class);
             MockedStatic<DSpaceServicesFactory> dSpaceServicesFactoryMockedStatic = Mockito.mockStatic(
                 DSpaceServicesFactory.class);
             MockedStatic<AuthorizeServiceFactory> authorizeServiceFactoryMockedStatic = Mockito.mockStatic(
                 AuthorizeServiceFactory.class);
             MockedStatic<ContentServiceFactory> contentServiceFactoryMockedStatic = Mockito.mockStatic(
                 ContentServiceFactory.class)) {

            handleServiceFactoryMockedStatic.when(HandleServiceFactory::getInstance).thenReturn(handleServiceFactory);
            dSpaceServicesFactoryMockedStatic.when(DSpaceServicesFactory::getInstance)
                .thenReturn(dSpaceServicesFactory);
            authorizeServiceFactoryMockedStatic.when(AuthorizeServiceFactory::getInstance)
                .thenReturn(authorizeServiceFactory);
            contentServiceFactoryMockedStatic.when(ContentServiceFactory::getInstance)
                .thenReturn(contentServiceFactory);

            testMethod.run();
        }
    }
}