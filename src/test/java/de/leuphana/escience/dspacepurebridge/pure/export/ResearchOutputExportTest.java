package de.leuphana.escience.dspacepurebridge.pure.export;

import static de.leuphana.escience.dspacepurebridge.identifiers.Identifiers.DOI_RESOLVER_HTTPS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.leuphana.escience.dspacepurebridge.Constants;
import de.leuphana.escience.dspacepurebridge.DSpaceServicesContainer;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.AccessType;
import de.leuphana.escience.dspacepurebridge.pure.generated.ApiException;
import de.leuphana.escience.dspacepurebridge.pure.generated.api.ResearchOutputApi;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.ClassificationRef;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.ClassificationRefList;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.DoiElectronicVersion;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.InternalContributorAssociation;
import de.leuphana.escience.dspacepurebridge.pure.generated.model.ResearchOutput;
import de.leuphana.escience.dspacepurebridge.pure.imports.DSpaceObjectMappings;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResearchOutputExportTest {
    @Mock
    private Context context;

    @Mock
    private Item publicationItem;

    @Mock
    private ItemService itemService;

    @Mock
    private DSpaceServicesContainer dSpaceServicesContainer;

    @Mock
    private BitstreamService bitstreamService;

    @Mock
    private AuthorizeService authorizeService;

    @Mock
    private ResourcePolicyService resourcePolicyService;


    @Spy
    private DSpaceObjectMappings dSpaceObjectMappings;

    @Mock
    private ResearchOutputApi researchOutputApi;

    @Mock
    private ConfigurationService configurationService;

    @Spy
    private ExportStatus exportStatus = new ExportStatus("TEST");

    @Spy
    @InjectMocks
    private ResearchOutputExport classUnderTest;

    @BeforeEach
    public void setUp() throws Exception {
        lenient().when(dSpaceServicesContainer.getConfigurationService()).thenReturn(configurationService);
        lenient().when(dSpaceServicesContainer.getItemService()).thenReturn(itemService);
        lenient().when(dSpaceServicesContainer.getAuthorizeService()).thenReturn(authorizeService);
        lenient().when(dSpaceServicesContainer.getResourcePolicyService()).thenReturn(resourcePolicyService);
        lenient().when(dSpaceServicesContainer.getBitstreamService()).thenReturn(bitstreamService);

        lenient().doAnswer(invocation -> invocation.getArgument(1)).when(configurationService)
                .getProperty(anyString(), anyString());
    }

    @Test
    public void setupMappingFromConfiguration() {
        Map<String, String> mapping = new HashMap<>();
        mapping.put(ResearchOutputExport.MAPPING_PROPERTIES_PREFIX + "." +
                        ResearchOutputMappingType.ACCESS_TYPE + "." + AccessType.EMBARGO,
                "/dk/atira/pure/core/openaccesspermission/embargoed");
        mapping.put(
                ResearchOutputExport.MAPPING_PROPERTIES_PREFIX + "." +
                        ResearchOutputMappingType.LICENSE + ".Nutzung nach Urheberrecht",
                "/dk/atira/pure/core/document/licenses/unspecified");
        mapping.put(
                ResearchOutputExport.MAPPING_PROPERTIES_PREFIX + "." +
                        ResearchOutputMappingType.LICENSE + ".CC-BY-ND",
                "/dk/atira/pure/core/document/licenses/cc_by_nd");
        mapping.put(
                ResearchOutputExport.MAPPING_PROPERTIES_PREFIX + "." +
                        ResearchOutputMappingType.ROLE + ".Dissertation-Author",
                "/dk/atira/pure/researchoutput/roles/bookanthology/author");


        when(configurationService.getPropertyKeys(ResearchOutputExport.MAPPING_PROPERTIES_PREFIX)).thenReturn(
                new ArrayList<>(mapping.keySet()));
        for (String key : mapping.keySet()) {
            when(configurationService.getProperty(key)).thenReturn(mapping.get(key));
        }
        classUnderTest.setupMappingFromConfiguration();

        Assertions.assertNotEquals(0, classUnderTest.mapping.size());
        Assertions.assertEquals("/dk/atira/pure/core/document/licenses/cc_by_nd",
                classUnderTest.mapping.get(ResearchOutputMappingType.LICENSE).get("CC-BY-ND"));
        Assertions.assertEquals("/dk/atira/pure/core/document/licenses/unspecified",
                classUnderTest.mapping.get(ResearchOutputMappingType.LICENSE).get("Nutzung nach Urheberrecht"));
        Assertions.assertEquals("/dk/atira/pure/core/openaccesspermission/embargoed", classUnderTest.mapping.get(
                        ResearchOutputMappingType.ACCESS_TYPE)
                .get(AccessType.EMBARGO.toString()));
        Assertions.assertEquals("/dk/atira/pure/researchoutput/roles/bookanthology/author",
                classUnderTest.mapping.get(ResearchOutputMappingType.ROLE).get("Dissertation-Author"));
    }

    @Test
    public void setupMappingWithInvalidMappingFromConfigurationType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Map<String, String> mapping = new HashMap<>();
            mapping.put(
                    ResearchOutputExport.MAPPING_PROPERTIES_PREFIX + ".UNKNOWN.MappingValueXY",
                    "/dk/atira/pure/core/document/xy/12");

            when(configurationService.getPropertyKeys(ResearchOutputExport.MAPPING_PROPERTIES_PREFIX)).thenReturn(
                    new ArrayList<>(mapping.keySet()));
            classUnderTest.setupMappingFromConfiguration();
        });
    }

    @Test()
    public void setupMappingFromConfigurationWithInvalidKey() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Map<String, String> mapping = new HashMap<>();
            mapping.put(
                    ResearchOutputExport.MAPPING_PROPERTIES_PREFIX + ".MappingValueXY",
                    "/dk/atira/pure/core/document/xy/12");

            when(configurationService.getPropertyKeys(ResearchOutputExport.MAPPING_PROPERTIES_PREFIX)).thenReturn(
                    new ArrayList<>(mapping.keySet()));
            classUnderTest.setupMappingFromConfiguration();
        });
    }

    @Test
    public void createResearchOutputDissertation() throws ApiException, SQLException {
        MetadataValue publicationLanguageMetadataValue = mock(MetadataValue.class);
        MetadataValue publicationYearMetadataValue = mock(MetadataValue.class);
        MetadataValue creationContextMetadataValue = mock(MetadataValue.class);
        MetadataValue titleDeuMetadataValue = mock(MetadataValue.class);
        MetadataValue titleEngMetadataValue = mock(MetadataValue.class);
        MetadataValue subTitleDeuMetadataValue = mock(MetadataValue.class);
        MetadataValue subTitleEngMetadataValue = mock(MetadataValue.class);
        MetadataValue abstractDeuMetadataValue = mock(MetadataValue.class);
        MetadataValue abstractEngMetadataValue = mock(MetadataValue.class);
        MetadataValue identifierMetadataValue = mock(MetadataValue.class);
        MetadataValue affiliationMetadataValue = mock(MetadataValue.class);
        final String publicationLanguage = DSpaceLanguage.GERMAN.getIso3Letter();
        final String publicationType = "Dissertation";
        final String researchValue = "Research";
        final String researchValueUrl = "/category/research";
        final String dissertationUrl = "/type/dissertation";
        final String publishedUrl = "/published/published";
        final String germanUrl = "/language/german";
        final String englishUrl = "/language/english";
        final String openAccessUrl = "/accessType/openAccess";
        final String versionTypeUrl = "/versionType/other";
        final String testLicense = "TEST LICENSE";
        final String licenseUrl = "/license/open";
        final String authorUrl = "/role/author";
        final String publicationYear = "2022";
        final String titleGerman = "TITEL DEU";
        final String titleEnglish = "TITLE ENG";
        final String subTitleGerman = "UNTERTITEL DEU";
        final String subTitleEnglish = "SUBTITLE ENG";
        final String abstractGerman = "ABSTRAKT DEU";
        final String abstractEnglish = "ABSTRACT ENG";
        final String doiValue = DOI_RESOLVER_HTTPS + "/DOITEST/XY";
        final String orgUnit = "ORGUNIT A";
        final String firstName = "firstName";
        final String lastName = "lastName";
        final UUID defaultOrganizationUUID = UUID.randomUUID();
        final UUID defaultAuthorUUID = UUID.randomUUID();
        final String defaultAuthorFirstName = "default";
        final String defaultAuthorLastName = "author";

        when(configurationService.getProperty("dspace-pure-bridge.export.defaultOrganizationUUID"))
                .thenReturn(defaultOrganizationUUID.toString());
        when(configurationService.getProperty("dspace-pure-bridge.export.defaultAuthorUUID")).thenReturn(
                defaultAuthorUUID.toString());
        when(configurationService.getProperty("dspace-pure-bridge.export.defaultAuthorFirstName")).thenReturn(
                defaultAuthorFirstName);
        when(configurationService.getProperty("dspace-pure-bridge.export.defaultAuthorLastName")).thenReturn(
                defaultAuthorLastName);

        Map<ResearchOutputMappingType, Map<String, String>> mapping = classUnderTest.mapping;
        for (ResearchOutputMappingType mappingType : ResearchOutputMappingType.values()) {
            mapping.put(mappingType, new HashMap<>());
        }
        mapping.get(ResearchOutputMappingType.CATEGORY).put(researchValue, researchValueUrl);
        mapping.get(ResearchOutputMappingType.TYPE).put(publicationType, dissertationUrl);
        mapping.get(ResearchOutputMappingType.PUBLISHED).put(DSpaceToPure.DEFAULT_METADATA_VALUE, publishedUrl);
        mapping.get(ResearchOutputMappingType.LANGUAGE).put(publicationLanguage, germanUrl);
        mapping.get(ResearchOutputMappingType.LANGUAGE).put(DSpaceLanguage.ENGLISH.getIso3Letter(), englishUrl);
        mapping.get(ResearchOutputMappingType.ACCESS_TYPE).put(String.valueOf(AccessType.OPEN_ACCESS), openAccessUrl);
        mapping.get(ResearchOutputMappingType.ELECTRONIC_VERSION_TYPE)
                .put(DSpaceToPure.DEFAULT_METADATA_VALUE, versionTypeUrl);
        mapping.get(ResearchOutputMappingType.LICENSE).put(testLicense, licenseUrl);
        mapping.get(ResearchOutputMappingType.ROLE)
                .put(DSpaceToPure.PURE_AUTHOR_ROLE, authorUrl);

        when(creationContextMetadataValue.getValue()).thenReturn(researchValue);
        when(itemService.getMetadataByMetadataString(publicationItem, "local.CreationContext")).thenReturn(
                Collections.singletonList(creationContextMetadataValue));

        when(publicationLanguageMetadataValue.getValue()).thenReturn(publicationLanguage);
        when(itemService.getMetadataByMetadataString(publicationItem, "DataCite.Language")).thenReturn(
                List.of(publicationLanguageMetadataValue));

        when(publicationYearMetadataValue.getValue()).thenReturn(publicationYear);
        when(itemService.getMetadataByMetadataString(publicationItem, "DataCite.PublicationYear")).thenReturn(
                Collections.singletonList(publicationYearMetadataValue));
        when(titleDeuMetadataValue.getValue()).thenReturn(titleGerman);
        when(titleEngMetadataValue.getValue()).thenReturn(titleEnglish);
        when(titleEngMetadataValue.getLanguage()).thenReturn(DSpaceLanguage.ENGLISH.getIso2Letter());
        when(itemService.getMetadataByMetadataString(publicationItem, "dc.title")).thenReturn(
                List.of(titleDeuMetadataValue, titleEngMetadataValue));
        when(subTitleDeuMetadataValue.getValue()).thenReturn(subTitleGerman);
        when(subTitleDeuMetadataValue.getLanguage()).thenReturn(DSpaceLanguage.GERMAN.getIso2Letter());
        when(subTitleEngMetadataValue.getValue()).thenReturn(subTitleEnglish);
        when(subTitleEngMetadataValue.getLanguage()).thenReturn(DSpaceLanguage.ENGLISH.getIso2Letter());
        when(itemService.getMetadataByMetadataString(publicationItem, "DataCite.Title.Subtitle")).thenReturn(
                List.of(subTitleDeuMetadataValue, subTitleEngMetadataValue));
        when(abstractDeuMetadataValue.getValue()).thenReturn(abstractGerman);
        when(abstractDeuMetadataValue.getLanguage()).thenReturn(DSpaceLanguage.GERMAN.getIso2Letter());
        when(abstractEngMetadataValue.getValue()).thenReturn(abstractEnglish);
        when(abstractEngMetadataValue.getLanguage()).thenReturn(DSpaceLanguage.ENGLISH.getIso2Letter());
        when(itemService.getMetadataByMetadataString(
                publicationItem, "DataCite.Description.Abstract")).thenReturn(
                List.of(abstractDeuMetadataValue, abstractEngMetadataValue));
        when(identifierMetadataValue.getValue()).thenReturn(doiValue);
        when(itemService.getMetadataByMetadataString(publicationItem, "dc.identifier.uri")).thenReturn(
                Collections.singletonList(identifierMetadataValue));
        when(affiliationMetadataValue.getValue()).thenReturn(orgUnit);
        when(itemService.getMetadataByMetadataString(publicationItem, "local.Affiliation")).thenReturn(
                Collections.singletonList(affiliationMetadataValue));


        ClassificationRef research = new ClassificationRef();
        research.setUri(researchValueUrl);
        ClassificationRefList categoriesRefList = new ClassificationRefList();
        categoriesRefList.addClassificationsItem(research);
        when(researchOutputApi.researchoutputGetAllowedCategories()).thenReturn(categoriesRefList);

        ClassificationRef type = new ClassificationRef();
        type.setUri(dissertationUrl);
        ClassificationRefList typesRefList = new ClassificationRefList();
        typesRefList.addClassificationsItem(type);
        when(researchOutputApi.researchOutputGetAllowedTypes()).thenReturn(typesRefList);

        ClassificationRef published = new ClassificationRef();
        published.setUri(publishedUrl);
        ClassificationRefList publishedRefList = new ClassificationRefList();
        publishedRefList.addClassificationsItem(published);
        when(researchOutputApi.researchoutputGetAllowedPublicationStatuses()).thenReturn(publishedRefList);

        ClassificationRef german = new ClassificationRef();
        german.setUri(germanUrl);
        ClassificationRef english = new ClassificationRef();
        english.setUri(englishUrl);
        ClassificationRefList languagesRefList = new ClassificationRefList();
        languagesRefList.addClassificationsItem(german);
        languagesRefList.addClassificationsItem(english);
        when(researchOutputApi.researchoutputGetAllowedLanguages()).thenReturn(languagesRefList);

        ClassificationRef accessType = new ClassificationRef();
        accessType.setUri(openAccessUrl);
        ClassificationRefList accessTypeRefList = new ClassificationRefList();
        accessTypeRefList.addClassificationsItem(accessType);
        when(researchOutputApi.researchoutputGetAllowedElectronicVersionAccessTypes()).thenReturn(accessTypeRefList);

        ClassificationRef versionType = new ClassificationRef();
        versionType.setUri(versionTypeUrl);
        ClassificationRefList versionTypeRefList = new ClassificationRefList();
        versionTypeRefList.addClassificationsItem(versionType);
        when(researchOutputApi.researchoutputGetAllowedElectronicVersionVersionTypes()).thenReturn(versionTypeRefList);

        ClassificationRef licenseType = new ClassificationRef();
        licenseType.setUri(licenseUrl);
        ClassificationRefList licensesRefList = new ClassificationRefList();
        licensesRefList.addClassificationsItem(licenseType);
        when(researchOutputApi.researchoutputGetAllowedElectronicVersionLicenseTypes()).thenReturn(licensesRefList);

        ClassificationRef authorRole = new ClassificationRef();
        authorRole.setUri(authorUrl);
        ClassificationRefList authorRoleRefList = new ClassificationRefList();
        authorRoleRefList.addClassificationsItem(authorRole);
        when(researchOutputApi.researchoutputGetAllowedBookAnthologyContributorRoles()).thenReturn(authorRoleRefList);

        ElectronicVersionAccessDetail electronicVersionAccessDetail = new ElectronicVersionAccessDetail();
        electronicVersionAccessDetail.setAccessType(AccessType.OPEN_ACCESS);
        electronicVersionAccessDetail.setLicense(testLicense);
        doReturn(electronicVersionAccessDetail).when(classUnderTest).getElectronicVersionAccessDetail(context,
                publicationItem);

        UUID authorPureUUID = UUID.randomUUID();
        Item author = mock(Item.class);
        when(itemService.getMetadataFirstValue(
                author, "person", "givenName", null, Item.ANY))
                .thenReturn(firstName);
        when(itemService.getMetadataFirstValue(
                author, "person", "familyName", null, Item.ANY))
                .thenReturn(lastName);
        when(itemService.getMetadataFirstValue(
                author, Constants.SCHEME,
                Constants.ELEMENT,
                Constants.UUID_QUALIFIER,
                Item.ANY))
                .thenReturn(authorPureUUID.toString());


        doReturn(List.of(author)).when(classUnderTest).getItemAuthors(context, publicationItem, false);

        doReturn(researchOutputApi).when(classUnderTest).createResearchOutputApiClient();
        doNothing().when(classUnderTest).setupMappingFromConfiguration();
        classUnderTest.init();
        ExportItem exportItem = classUnderTest.createExport(context, publicationItem, publicationType);
        ResearchOutput researchOutput = (ResearchOutput) exportItem.export();

        Assertions.assertEquals(titleGerman, researchOutput.getTitle().getValue());
        Assertions.assertEquals(titleEnglish,
                researchOutput.getTranslatedTitle().get(DSpaceLanguage.ENGLISH.getLocale()));
        Assertions.assertEquals(doiValue,
                ((DoiElectronicVersion) researchOutput.getElectronicVersions().get(0)).getDoi());
        Assertions.assertEquals(openAccessUrl, researchOutput.getElectronicVersions().get(0).getAccessType().getUri());
        Assertions.assertEquals(licenseUrl, researchOutput.getElectronicVersions().get(0).getLicenseType().getUri());
        Assertions.assertEquals(subTitleGerman, researchOutput.getSubTitle().getValue());
        Assertions.assertEquals(subTitleEnglish,
                researchOutput.getTranslatedSubTitle().get(DSpaceLanguage.ENGLISH.getLocale()));
        Assertions.assertEquals(abstractGerman, researchOutput.getAbstract().get(DSpaceLanguage.GERMAN.getLocale()));
        Assertions.assertEquals(abstractEnglish, researchOutput.getAbstract().get(DSpaceLanguage.ENGLISH.getLocale()));
        Assertions.assertEquals(publicationYear,
                String.valueOf(researchOutput.getPublicationStatuses().get(0).getPublicationDate().getYear()));
        Assertions.assertEquals(researchValueUrl, researchOutput.getCategory().getUri());
        Assertions.assertEquals(dissertationUrl, researchOutput.getType().getUri());
        Assertions.assertEquals(authorPureUUID,
                ((InternalContributorAssociation) researchOutput.getContributors().get(0)).getPerson().getUuid());
        Assertions.assertEquals(firstName,
                ((InternalContributorAssociation) researchOutput.getContributors().get(0)).getName()
                        .getFirstName());
        Assertions.assertEquals(lastName,
                ((InternalContributorAssociation) researchOutput.getContributors().get(0)).getName()
                        .getLastName());
        Assertions.assertEquals(authorUrl,
                ((InternalContributorAssociation) researchOutput.getContributors().get(0)).getRole().getUri());
        Assertions.assertEquals(defaultOrganizationUUID, researchOutput.getOrganizations().get(0).getUuid());
    }

    @Test
    public void getElectronicVersionAccessDetail() throws SQLException {
        List<Bundle> bundles = new ArrayList<>();
        Bundle bundle = mock(Bundle.class);
        bundles.add(bundle);
        Bitstream bitstreamOpenAccess = mock(Bitstream.class);
        Bitstream bitstreamEmbargo = mock(Bitstream.class);
        MetadataValue licenseOpenMetadataValue = mock(MetadataValue.class);
        when(licenseOpenMetadataValue.getValue()).thenReturn("LICENSE OA");
        MetadataValue licenseRestrictedMetadataValue = mock(MetadataValue.class);
        when(licenseRestrictedMetadataValue.getValue()).thenReturn("LICENSE EMBARGO");

        when(itemService.getBundles(publicationItem, org.dspace.core.Constants.DEFAULT_BUNDLE_NAME)).thenReturn(
                bundles);
        when(bundle.getPrimaryBitstream()).thenReturn(null);
        when(bundle.getBitstreams()).thenReturn(List.of(bitstreamOpenAccess, bitstreamEmbargo));
        when(classUnderTest.getAccessRightsValueForPolicies(eq(context), any(List.class))).thenReturn(
                AccessType.OPEN_ACCESS, AccessType.EMBARGO);

        when(bitstreamService.getMetadataByMetadataString(bitstreamOpenAccess, "local.BitstreamLicense")).thenReturn(List.of(licenseOpenMetadataValue));
        when(bitstreamService.getMetadataByMetadataString(bitstreamEmbargo, "local.BitstreamLicense")).thenReturn(List.of(licenseRestrictedMetadataValue));

        ElectronicVersionAccessDetail electronicVersionAccessDetail =
                classUnderTest.getElectronicVersionAccessDetail(context, publicationItem);
        Assertions.assertEquals(AccessType.EMBARGO, electronicVersionAccessDetail.getAccessType());
        Assertions.assertEquals("LICENSE EMBARGO", electronicVersionAccessDetail.getLicense());
    }

    @Test
    public void getElectronicVersionAccessDetailEmbargoFirst() throws SQLException {
        List<Bundle> bundles = new ArrayList<>();
        Bundle bundle = mock(Bundle.class);
        bundles.add(bundle);
        Bitstream bitstreamOpenAccess = mock(Bitstream.class);
        Bitstream bitstreamEmbargo = mock(Bitstream.class);
        MetadataValue licenseMetadataValue = mock(MetadataValue.class);
        when(licenseMetadataValue.getValue()).thenReturn("LICENSE EMBARGO");
        when(itemService.getBundles(publicationItem, org.dspace.core.Constants.DEFAULT_BUNDLE_NAME)).thenReturn(
                bundles);
        when(bundle.getPrimaryBitstream()).thenReturn(null);
        when(bundle.getBitstreams()).thenReturn(List.of(bitstreamOpenAccess, bitstreamEmbargo));
        when(classUnderTest.getAccessRightsValueForPolicies(eq(context), any(List.class))).thenReturn(
                AccessType.EMBARGO);
        when(bitstreamService.getMetadataByMetadataString(bitstreamOpenAccess, "local.BitstreamLicense")).thenReturn(List.of(licenseMetadataValue));

        ElectronicVersionAccessDetail electronicVersionAccessDetail =
                classUnderTest.getElectronicVersionAccessDetail(context, publicationItem);
        Assertions.assertEquals(AccessType.EMBARGO, electronicVersionAccessDetail.getAccessType());
        Assertions.assertEquals("LICENSE EMBARGO", electronicVersionAccessDetail.getLicense());
    }

    @Test
    public void getElectronicVersionAccessDetailNoEmbargo() throws SQLException {
        List<Bundle> bundles = new ArrayList<>();
        Bundle bundle = mock(Bundle.class);
        bundles.add(bundle);
        Bitstream bitstreamOpenAccess = mock(Bitstream.class);
        Bitstream bitstreamEmbargo = mock(Bitstream.class);
        MetadataValue licenseOpenMetadataValue = mock(MetadataValue.class);
        when(licenseOpenMetadataValue.getValue()).thenReturn("LICENSE OA");
        MetadataValue licenseRestrictedMetadataValue = mock(MetadataValue.class);
        when(licenseRestrictedMetadataValue.getValue()).thenReturn("LICENSE RESTRICTED");
        when(itemService.getBundles(publicationItem, org.dspace.core.Constants.DEFAULT_BUNDLE_NAME)).thenReturn(
                bundles);
        when(bundle.getPrimaryBitstream()).thenReturn(null);
        when(bundle.getBitstreams()).thenReturn(List.of(bitstreamOpenAccess, bitstreamEmbargo));
        when(classUnderTest.getAccessRightsValueForPolicies(eq(context), any(List.class))).thenReturn(
                AccessType.OPEN_ACCESS, AccessType.RESTRICTED);
        when(bitstreamService.getMetadataByMetadataString(bitstreamOpenAccess, "local.BitstreamLicense")).thenReturn(List.of(licenseOpenMetadataValue));
        when(bitstreamService.getMetadataByMetadataString(bitstreamEmbargo, "local.BitstreamLicense")).thenReturn(List.of(licenseRestrictedMetadataValue));

        //First non embargo access is used
        ElectronicVersionAccessDetail electronicVersionAccessDetail =
                classUnderTest.getElectronicVersionAccessDetail(context, publicationItem);
        Assertions.assertEquals(AccessType.OPEN_ACCESS, electronicVersionAccessDetail.getAccessType());
        Assertions.assertEquals("LICENSE OA", electronicVersionAccessDetail.getLicense());
    }

    @Test
    public void getElectronicVersionAccessDetailPrimaryBitstream() throws SQLException {
        List<Bundle> bundles = new ArrayList<>();
        Bundle bundle = mock(Bundle.class);
        bundles.add(bundle);
        Bitstream bitstream = mock(Bitstream.class);
        MetadataValue licenseMetadataValue = mock(MetadataValue.class);
        when(licenseMetadataValue.getValue()).thenReturn("LICENSE");

        when(itemService.getBundles(publicationItem, org.dspace.core.Constants.DEFAULT_BUNDLE_NAME)).thenReturn(
                bundles);
        when(bundle.getPrimaryBitstream()).thenReturn(bitstream);
        when(classUnderTest.getAccessRightsValueForPolicies(eq(context), any(List.class))).thenReturn(
                AccessType.OPEN_ACCESS);
        when(bitstreamService.getMetadataByMetadataString(bitstream, "local.BitstreamLicense")).thenReturn(List.of(licenseMetadataValue));

        ElectronicVersionAccessDetail electronicVersionAccessDetail =
                classUnderTest.getElectronicVersionAccessDetail(context, publicationItem);
        Assertions.assertEquals(AccessType.OPEN_ACCESS, electronicVersionAccessDetail.getAccessType());
        Assertions.assertEquals("LICENSE", electronicVersionAccessDetail.getLicense());
    }

    @Test
    public void getAccessRightsValueForAnonymousPolicy() throws SQLException {
        ResourcePolicy resourcePolicy = mock(ResourcePolicy.class);
        Group group = mock(Group.class);

        when(group.getName()).thenReturn(Group.ANONYMOUS);
        when(resourcePolicy.getGroup()).thenReturn(group);
        when(resourcePolicyService.isDateValid(resourcePolicy)).thenReturn(true);

        AccessType accessType = classUnderTest.getAccessRightsValueForPolicies(context, List.of(resourcePolicy));
        Assertions.assertEquals(AccessType.OPEN_ACCESS, accessType);
    }


    @Test
    public void getAccessRightsValueForAnonymousPolicyAndEmbargoDate() throws SQLException {
        ResourcePolicy resourcePolicy = mock(ResourcePolicy.class);
        Group group = mock(Group.class);

        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, 15);
        LocalDate startDate = c.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        when(group.getName()).thenReturn(Group.ANONYMOUS);
        when(resourcePolicy.getGroup()).thenReturn(group);
        when(resourcePolicyService.isDateValid(resourcePolicy)).thenReturn(false);
        when(resourcePolicy.getStartDate()).thenReturn(startDate);

        AccessType accessType = classUnderTest.getAccessRightsValueForPolicies(context, List.of(resourcePolicy));
        Assertions.assertEquals(AccessType.EMBARGO, accessType);
    }

    @Test
    public void getAccessRightsValueForNonAnonymousNonAdminPolicy() throws SQLException {
        ResourcePolicy resourcePolicy = mock(ResourcePolicy.class);
        Group group = mock(Group.class);

        when(group.getName()).thenReturn("CUSTOM_GROUP");
        when(resourcePolicy.getGroup()).thenReturn(group);
        when(resourcePolicyService.isDateValid(resourcePolicy)).thenReturn(true);

        AccessType accessType = classUnderTest.getAccessRightsValueForPolicies(context, List.of(resourcePolicy));
        Assertions.assertEquals(AccessType.RESTRICTED, accessType);
    }

    @Test
    public void getAccessRightsValueForNonAnonymousNonAdminPolicyAndEmbargoDate() throws SQLException {
        ResourcePolicy resourcePolicy = mock(ResourcePolicy.class);
        Group group = mock(Group.class);

        Calendar c = Calendar.getInstance();
        c.setTime(new Date()); // Now use today date.
        c.add(Calendar.DATE, 15);
        LocalDate startDate = c.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        when(group.getName()).thenReturn("CUSTOM_GROUP");
        when(resourcePolicy.getGroup()).thenReturn(group);
        when(resourcePolicyService.isDateValid(resourcePolicy)).thenReturn(false);
        when(resourcePolicy.getStartDate()).thenReturn(startDate);

        AccessType accessType = classUnderTest.getAccessRightsValueForPolicies(context, List.of(resourcePolicy));
        Assertions.assertEquals(AccessType.EMBARGO, accessType);
    }


}