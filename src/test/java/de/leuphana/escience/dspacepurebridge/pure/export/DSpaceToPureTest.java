package de.leuphana.escience.dspacepurebridge.pure.export;

import static de.leuphana.escience.dspacepurebridge.pure.export.DSpaceToPure.SYNC_MAIL_RECIPIENTS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.leuphana.escience.dspacepurebridge.CLIScriptContextUtils;
import de.leuphana.escience.MockedEnvironmentTestMethod;
import de.leuphana.escience.dspacepurebridge.pure.Constants;
import de.leuphana.escience.dspacepurebridge.pure.DSpaceServicesContainer;
import de.leuphana.escience.dspacepurebridge.pure.generated.ApiException;
import de.leuphana.escience.dspacepurebridge.pure.imports.DSpacePureEntity;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class DSpaceToPureTest {

    @Mock
    Context context;

    @Mock
    ConfigurationService configurationService;

    @Mock
    ItemService itemService;

    @Mock
    HandleService handleService;

    @Mock
    DSpaceServicesContainer dSpaceServicesContainer;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    ExportStatus exportStatus;

    @Mock
    Map<ExportType, AbstractExport> exporterRegistry;

    @InjectMocks
    @Spy
    DSpaceToPure classUnderTest;

    @BeforeEach
    void setup() {
        lenient().when(dSpaceServicesContainer.getConfigurationService()).thenReturn(configurationService);
        lenient().when(dSpaceServicesContainer.getItemService()).thenReturn(itemService);
        lenient().when(dSpaceServicesContainer.getHandleService()).thenReturn(handleService);
    }

    @Test
    void setupFilters() {
        String[] filterConfigurations = new String[] {"type:dissertation", "collection:collection_handle"};
        when(configurationService.getArrayProperty("dspace-pure-bridge.export.filter")).thenReturn(filterConfigurations);

        classUnderTest.setupFilters();

        Assertions.assertEquals(2, classUnderTest.getUnmodifiableFilterList().size());
    }

    @Test
    void setupTypeToExportTypeMap() {

        String[] exportTypeForTypeConfigurations =
            new String[] {"Dissertation:STUDENT_THESIS", "default:RESEARCH_OUTPUT"};
        when(configurationService.getArrayProperty("dspace-pure-bridge.export.exportTypeForType")).thenReturn(
            exportTypeForTypeConfigurations);

        classUnderTest.setupTypeToExportTypeMap();
        Map<String, ExportType> exportTypeHashMap = classUnderTest.getUnmodifiableExportTypeHashMap();
        Assertions.assertEquals(2, exportTypeHashMap.size());
        Assertions.assertEquals(ExportType.STUDENT_THESIS, exportTypeHashMap.get("Dissertation"));
        Assertions.assertEquals(ExportType.RESEARCH_OUTPUT, exportTypeHashMap.get("default"));
    }

    @Test
    void syncItemThreadExportLimitReached() throws Exception {
        UUID uuid = UUID.randomUUID();
        Item item = Mockito.mock(Item.class);
        classUnderTest.setExportLimit(10);
        when(itemService.find(context, uuid)).thenReturn(item);
        when(exportStatus.getItemSyncInfos().size()).thenReturn(10);

        executeTestInMockedEnvironment(() -> classUnderTest.syncItemThread(uuid));

        verify(classUnderTest, never()).syncDSpaceItemToPure(any(), any(), anyString(), any());
    }


    @Test
    void syncItemThreadHandleSpecifiedAndItemDoesNotMatch() throws Exception {
        UUID uuid = UUID.randomUUID();
        Item item = Mockito.mock(Item.class);
        String requestedHandle = "123456789/123456789";
        String itemHandle = "123456789/123456788";
        classUnderTest.setExportDataHandle(requestedHandle);
        when(item.getHandle()).thenReturn(itemHandle);
        when(itemService.find(context, uuid)).thenReturn(item);

        executeTestInMockedEnvironment(() -> classUnderTest.syncItemThread(uuid));

        verify(classUnderTest, never()).syncDSpaceItemToPure(any(), any(), anyString(), any());
    }

    @Test
    void syncItemThreadItemAlreadySynced() throws Exception {
        UUID uuid = UUID.randomUUID();
        String pureItemUUID = UUID.randomUUID().toString();

        Item item = Mockito.mock(Item.class);
        classUnderTest.setExportLimit(10);
        when(itemService.find(context, uuid)).thenReturn(item);
        when(itemService.getMetadataFirstValue(item, Constants.SCHEME, Constants.ELEMENT, Constants.UUID_QUALIFIER,
            Item.ANY)).thenReturn(pureItemUUID);

        executeTestInMockedEnvironment(() -> classUnderTest.syncItemThread(uuid));

        verify(classUnderTest, never()).syncDSpaceItemToPure(any(), any(), anyString(), any());
    }

    @Test
    void syncItemThreadItemTypeNotValidForSync() throws Exception {
        UUID uuid = UUID.randomUUID();

        Collection collection = Mockito.mock(Collection.class);
        Item item = Mockito.mock(Item.class);
        MetadataValue itemType = Mockito.mock(MetadataValue.class);
        when(itemType.getValue()).thenReturn("Dissertation");
        when(item.getOwningCollection()).thenReturn(collection);
        when(collection.getHandle()).thenReturn("collection_handle");
        when(itemService.find(context, uuid)).thenReturn(item);
        when(itemService.getMetadataByMetadataString(item, "dc.type")).thenReturn(List.of(itemType));

        String[] filterConfigurations = new String[] {"type:Article;collection:collection_handle"};
        String[] exportTypeForTypeConfigurations =
            new String[] {"Dissertation:STUDENT_THESIS", "default:RESEARCH_OUTPUT"};
        when(configurationService.getArrayProperty("dspace-pure-bridge.export.exportTypeForType")).thenReturn(
            exportTypeForTypeConfigurations);
        when(configurationService.getArrayProperty("dspace-pure-bridge.export.filter")).thenReturn(filterConfigurations);

        executeTestInMockedEnvironment(() -> {
            classUnderTest.setupFilters();
            classUnderTest.setupTypeToExportTypeMap();
            classUnderTest.syncItemThread(uuid);
        });

        verify(classUnderTest, times(0)).syncDSpaceItemToPure(any(), any(), anyString(), any());
    }


    @Test
    void syncItemThreadItemCollectionNotValidForSync() throws Exception {
        UUID uuid = UUID.randomUUID();

        Collection collection = Mockito.mock(Collection.class);
        Item item = Mockito.mock(Item.class);
        MetadataValue itemType = Mockito.mock(MetadataValue.class);
        when(item.getOwningCollection()).thenReturn(collection);
        when(collection.getHandle()).thenReturn("collection_handle");
        when(itemService.find(context, uuid)).thenReturn(item);

        String[] filterConfigurations = new String[] {"type:Dissertation;collection:collection_handle2"};
        String[] exportTypeForTypeConfigurations =
            new String[] {"Dissertation:STUDENT_THESIS", "default:RESEARCH_OUTPUT"};
        when(configurationService.getArrayProperty("dspace-pure-bridge.export.exportTypeForType")).thenReturn(
            exportTypeForTypeConfigurations);
        when(configurationService.getArrayProperty("dspace-pure-bridge.export.filter")).thenReturn(filterConfigurations);

        executeTestInMockedEnvironment(() -> {
            classUnderTest.setupFilters();
            classUnderTest.setupTypeToExportTypeMap();
            classUnderTest.syncItemThread(uuid);
        });

        verify(classUnderTest, times(0)).syncDSpaceItemToPure(any(), any(), anyString(), any());
    }

    @Test
    void syncItemThreadItemExportTypeNotValidForSync() throws Exception {
        UUID uuid = UUID.randomUUID();

        Collection collection = Mockito.mock(Collection.class);
        Item item = Mockito.mock(Item.class);
        MetadataValue itemType = Mockito.mock(MetadataValue.class);
        when(itemType.getValue()).thenReturn("Dissertation");
        when(item.getOwningCollection()).thenReturn(collection);
        when(collection.getHandle()).thenReturn("collection_handle");
        when(itemService.find(context, uuid)).thenReturn(item);
        when(itemService.getMetadataByMetadataString(item, "dc.type")).thenReturn(List.of(itemType));

        String[] filterConfigurations = new String[] {"type:Dissertation;collection:collection_handle"};
        String[] exportTypeForTypeConfigurations =
            new String[] {"Article:STUDENT_THESIS", "default:RESEARCH_OUTPUT"};
        when(configurationService.getArrayProperty("dspace-pure-bridge.export.exportTypeForType")).thenReturn(
            exportTypeForTypeConfigurations);
        when(configurationService.getArrayProperty("dspace-pure-bridge.export.filter")).thenReturn(filterConfigurations);
        doNothing().when(classUnderTest)
            .syncDSpaceItemToPure(context, item, "Dissertation", ExportType.RESEARCH_OUTPUT);

        executeTestInMockedEnvironment(() -> {
            classUnderTest.setupFilters();
            classUnderTest.setupTypeToExportTypeMap();
            classUnderTest.syncItemThread(uuid);
        });

        verify(classUnderTest, times(1)).syncDSpaceItemToPure(any(), any(), anyString(), any());
    }


    @Test
    void syncItemThreadItem() throws Exception {
        UUID uuid = UUID.randomUUID();

        Collection collection = Mockito.mock(Collection.class);
        Item item = Mockito.mock(Item.class);
        MetadataValue itemType = Mockito.mock(MetadataValue.class);
        when(itemType.getValue()).thenReturn("Dissertation");
        when(item.getOwningCollection()).thenReturn(collection);
        when(collection.getHandle()).thenReturn("collection_handle");
        when(itemService.find(context, uuid)).thenReturn(item);
        when(itemService.getMetadataByMetadataString(item, "dc.type")).thenReturn(List.of(itemType));

        String[] filterConfigurations = new String[] {"type:Dissertation;collection:collection_handle"};
        String[] exportTypeForTypeConfigurations =
            new String[] {"Dissertation:STUDENT_THESIS", "default:RESEARCH_OUTPUT"};
        when(configurationService.getArrayProperty("dspace-pure-bridge.export.exportTypeForType")).thenReturn(
            exportTypeForTypeConfigurations);
        when(configurationService.getArrayProperty("dspace-pure-bridge.export.filter")).thenReturn(filterConfigurations);
        doNothing().when(classUnderTest).syncDSpaceItemToPure(context, item, "Dissertation", ExportType.STUDENT_THESIS);

        executeTestInMockedEnvironment(() -> {
            classUnderTest.setupFilters();
            classUnderTest.setupTypeToExportTypeMap();
            classUnderTest.syncItemThread(uuid);
        });

        verify(classUnderTest, times(1)).syncDSpaceItemToPure(any(), any(), anyString(), any());
    }

    @Test
    void testMarkAsSynced() throws Exception {
        Item item = Mockito.mock(Item.class);
        ExportResult exportResult = Mockito.mock(ExportResult.class);

        UUID exportUuid = UUID.randomUUID();
        when(exportResult.getUuid()).thenReturn(exportUuid);

        classUnderTest.markItemAsSynced(context, item, exportResult, false);

        verify(itemService, times(1))
            .addMetadata(context, item, Constants.SCHEME,
                Constants.ELEMENT,
                Constants.UUID_QUALIFIER, null,
                String.valueOf(exportUuid));
    }

    @Test
    void prepareOrcidToPureMap() throws Exception {
        Collection personCollection = Mockito.mock(Collection.class);
        Item purePerson1 = Mockito.mock(Item.class);
        Item purePerson2 = Mockito.mock(Item.class);
        Item purePerson3 = Mockito.mock(Item.class);

        String orcid = "0000-0001-2345-678X";

        MetadataValue pureUUID1MetadataValue = Mockito.mock(MetadataValue.class);
        MetadataValue pureUUID2MetadataValue = Mockito.mock(MetadataValue.class);
        MetadataValue pureUUID3MetadataValue = Mockito.mock(MetadataValue.class);
        MetadataValue orcidMetadataValue = Mockito.mock(MetadataValue.class);
        when(orcidMetadataValue.getValue()).thenReturn(orcid);

        when(handleService.resolveToObject(context, DSpacePureEntity.PERSON.getDspacePureEntityCollectionHandle(configurationService)))
            .thenReturn(personCollection);
        when(itemService.findByCollection(context, personCollection))
            .thenReturn(List.of(purePerson1, purePerson2, purePerson3).iterator());

        when(itemService
            .getMetadata(purePerson1, "person", "identifier", "orcid", Item.ANY, false))
            .thenReturn(Collections.emptyList());
        when(itemService
            .getMetadata(purePerson2, "person", "identifier", "orcid", Item.ANY, false))
            .thenReturn(Collections.singletonList(orcidMetadataValue));
        when(itemService
            .getMetadata(purePerson3, "person", "identifier", "orcid", Item.ANY, false))
            .thenReturn(Collections.emptyList());
        when(itemService
            .getMetadata(purePerson1, Constants.SCHEME, Constants.ELEMENT, Constants.UUID_QUALIFIER, Item.ANY, false))
            .thenReturn(Collections.singletonList(pureUUID1MetadataValue));
        when(itemService
            .getMetadata(purePerson2, Constants.SCHEME, Constants.ELEMENT, Constants.UUID_QUALIFIER, Item.ANY, false))
            .thenReturn(Collections.singletonList(pureUUID2MetadataValue));
        when(itemService
            .getMetadata(purePerson3, Constants.SCHEME, Constants.ELEMENT, Constants.UUID_QUALIFIER, Item.ANY, false))
            .thenReturn(Collections.singletonList(pureUUID3MetadataValue));


        classUnderTest.prepareOrcidToPureMap(context);

        Assertions.assertEquals(1, classUnderTest.getdSpaceObjectMappings().getOrcidToPureMap().size());
        Assertions.assertEquals(purePerson2, classUnderTest.getdSpaceObjectMappings().getOrcidToPureMap().get(orcid));
    }

    @Test
    void prepareOrganizationNameToPureMap() throws Exception {
        Collection orgUnitCollection = Mockito.mock(Collection.class);
        Item pureOrgUnit1 = Mockito.mock(Item.class);
        Item pureOrgUnit2 = Mockito.mock(Item.class);
        Item pureOrgUnit3 = Mockito.mock(Item.class);

        UUID uuid = UUID.randomUUID();
        String legalName = "0000-0001-2345-678X";

        MetadataValue pureUUID1MetadataValue = Mockito.mock(MetadataValue.class);
        MetadataValue pureUUID2MetadataValue = Mockito.mock(MetadataValue.class);
        MetadataValue pureUUID3MetadataValue = Mockito.mock(MetadataValue.class);
        MetadataValue legalNameMetadataValue = Mockito.mock(MetadataValue.class);
        when(pureUUID2MetadataValue.getValue()).thenReturn(uuid.toString());
        when(legalNameMetadataValue.getValue()).thenReturn(legalName);

        when(
            handleService.resolveToObject(context, DSpacePureEntity.ORGANIZATION.getDspacePureEntityCollectionHandle(configurationService)))
            .thenReturn(orgUnitCollection);
        when(itemService.findByCollection(context, orgUnitCollection))
            .thenReturn(List.of(pureOrgUnit1, pureOrgUnit2, pureOrgUnit3).iterator());

        when(itemService
            .getMetadata(pureOrgUnit1, "organization", "legalName", null, Item.ANY, false))
            .thenReturn(Collections.emptyList());
        when(itemService
            .getMetadata(pureOrgUnit2, "organization", "legalName", null, Item.ANY, false))
            .thenReturn(Collections.singletonList(legalNameMetadataValue));
        when(itemService
            .getMetadata(pureOrgUnit3, "organization", "legalName", null, Item.ANY, false))
            .thenReturn(Collections.emptyList());
        when(itemService
            .getMetadata(pureOrgUnit1, Constants.SCHEME, Constants.ELEMENT, Constants.UUID_QUALIFIER, Item.ANY, false))
            .thenReturn(Collections.singletonList(pureUUID1MetadataValue));
        when(itemService
            .getMetadata(pureOrgUnit2, Constants.SCHEME, Constants.ELEMENT, Constants.UUID_QUALIFIER, Item.ANY, false))
            .thenReturn(Collections.singletonList(pureUUID2MetadataValue));
        when(itemService
            .getMetadata(pureOrgUnit3, Constants.SCHEME, Constants.ELEMENT, Constants.UUID_QUALIFIER, Item.ANY, false))
            .thenReturn(Collections.singletonList(pureUUID3MetadataValue));


        classUnderTest.prepareOrganizationNameToPureMap(context);

        Assertions.assertEquals(1, classUnderTest.getdSpaceObjectMappings().getOrganizationNameToPureMap().size());
        Assertions.assertEquals(uuid,
            classUnderTest.getdSpaceObjectMappings().getOrganizationNameToPureMap().get(legalName));
    }

    @Test
    void syncItemsTest() throws Exception {
        doNothing().when(classUnderTest).prepareOrcidToPureMap(context);
        doNothing().when(classUnderTest).prepareOrganizationNameToPureMap(context);
        doNothing().when(classUnderTest).sendErrorEmail(context);
        doNothing().when(classUnderTest).syncItemThread(any());

        String[] filterConfigurations = new String[] {"type:Dissertation;collection:collection_handle"};
        when(configurationService.getArrayProperty("dspace-pure-bridge.export.filter")).thenReturn(filterConfigurations);

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        Item item1 = Mockito.mock(Item.class);
        Item item2 = Mockito.mock(Item.class);
        Item item3 = Mockito.mock(Item.class);
        when(item1.getID()).thenReturn(uuid1);
        when(item2.getID()).thenReturn(uuid2);
        when(item3.getID()).thenReturn(uuid3);
        when(itemService.findArchivedByMetadataField(context, "dspace.entity.type", "Publication")).thenReturn(
            List.of(item1, item2, item3).iterator());

        executeTestInMockedEnvironment(() -> {
            classUnderTest.setupFilters();
            classUnderTest.syncItems();
        });

        verify(classUnderTest, times(1)).syncItemThread(uuid1);
        verify(classUnderTest, times(1)).syncItemThread(uuid2);
        verify(classUnderTest, times(1)).syncItemThread(uuid3);
    }

    @Test
    void syncDSpaceItemToPureDuplicateCheck() throws Exception {
        ExportType exportType = ExportType.STUDENT_THESIS;
        Item item = Mockito.mock(Item.class);
        UUID duplicateUuid = UUID.randomUUID();

        ExportResult exportResult = Mockito.mock(ExportResult.class);
        when(exportResult.getUuid()).thenReturn(duplicateUuid);
        AbstractExport exporter = Mockito.mock(AbstractExport.class);

        when(exporterRegistry.get(exportType)).thenReturn(exporter);
        when(exporter.checkForDuplicate(item, exportType)).thenReturn(exportResult);

        classUnderTest.syncDSpaceItemToPure(context, item, "Dissertation", exportType);

        verify(classUnderTest, times(1)).markItemAsSynced(context, item, exportResult, true);
        verify(exporter, never()).createExport(any(), any(), anyString());
    }

    @Test
    void syncDSpaceItemToPureExceptionDuringCreation() throws Exception {
        String syncTypeValue = "Dissertation";
        ExportType exportType = ExportType.STUDENT_THESIS;
        Item item = Mockito.mock(Item.class);
        AbstractExport exporter = Mockito.mock(AbstractExport.class);
        when(exporterRegistry.get(exportType)).thenReturn(exporter);
        when(exporter.checkForDuplicate(item, exportType)).thenReturn(null);
        doThrow(new ApiException()).when(exporter).createExport(context, item, syncTypeValue);

        classUnderTest.syncDSpaceItemToPure(context, item, syncTypeValue, exportType);

        verify(exporter, never()).export(any());
    }

    @Test
    void syncDSpaceItemToPureExceptionDuringExport() throws Exception {
        String syncTypeValue = "Dissertation";
        ExportType exportType = ExportType.STUDENT_THESIS;
        Item item = Mockito.mock(Item.class);
        ExportItem exportItem = Mockito.mock(ExportItem.class);
        AbstractExport exporter = Mockito.mock(AbstractExport.class);
        when(exporterRegistry.get(exportType)).thenReturn(exporter);
        when(exporter.checkForDuplicate(item, exportType)).thenReturn(null);
        when(exporter.createExport(context, item, syncTypeValue)).thenReturn(exportItem);
        doThrow(new ApiException()).when(exporter).export(exportItem);

        classUnderTest.syncDSpaceItemToPure(context, item, syncTypeValue, exportType);

        verify(classUnderTest, never()).markItemAsSynced(any(), any(), any(), anyBoolean());
    }


    @Test
    void syncDSpaceItemToPureOk() throws Exception {
        String syncTypeValue = "Dissertation";
        ExportType exportType = ExportType.STUDENT_THESIS;
        Item item = Mockito.mock(Item.class);
        ExportItem exportItem = Mockito.mock(ExportItem.class);
        ExportResult exportResult = Mockito.mock(ExportResult.class);
        AbstractExport exporter = Mockito.mock(AbstractExport.class);
        when(exporterRegistry.get(exportType)).thenReturn(exporter);
        when(exportResult.getUuid()).thenReturn(UUID.randomUUID());
        when(exporter.checkForDuplicate(item, exportType)).thenReturn(null);
        when(exporter.createExport(context, item, syncTypeValue)).thenReturn(exportItem);
        when(exporter.export(exportItem)).thenReturn(exportResult);

        classUnderTest.syncDSpaceItemToPure(context, item, syncTypeValue, exportType);

        verify(classUnderTest, times(1)).markItemAsSynced(context, item, exportResult, false);
    }

    @Test
    void sendErrorEmail() throws Exception {
        when(configurationService.getArrayProperty(SYNC_MAIL_RECIPIENTS)).thenReturn(
            new String[] {"pureexport@example.org"});
        Email email = Mockito.mock(Email.class);

        executeTestInMockedEnvironment(() -> classUnderTest.sendErrorEmail(context), email);

        verify(email, times(1)).addRecipient("pureexport@example.org");
        verify(email, times(2)).addArgument(anyString());
        verify(email, times(1)).send();
    }

    void executeTestInMockedEnvironment(MockedEnvironmentTestMethod testMethod) throws Exception {
        executeTestInMockedEnvironment(testMethod, null);
    }

    void executeTestInMockedEnvironment(MockedEnvironmentTestMethod testMethod, Email email) throws Exception {
        try (
            MockedStatic<CLIScriptContextUtils> cliScriptContextUtilsMockedStatic = Mockito.mockStatic(
                CLIScriptContextUtils.class);
            MockedStatic<Email> emailMockedStatic = Mockito.mockStatic(Email.class);
            MockedStatic<I18nUtil> i18nUtilMockedStatic = Mockito.mockStatic(I18nUtil.class)) {
            cliScriptContextUtilsMockedStatic.when(CLIScriptContextUtils::createReducedContext).thenReturn(context);

            if (email != null) {
                i18nUtilMockedStatic.when(() -> I18nUtil.getEmailFilename(any(), anyString()))
                    .thenAnswer((Answer<Void>) invocation -> null);

                emailMockedStatic.when(() -> Email.getEmail(any())).thenReturn(email);
            }

            testMethod.run();
        }
    }
}