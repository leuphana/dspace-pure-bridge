package de.leuphana.escience.dspacepurebridge.pure.imports;

import de.leuphana.escience.dspacepurebridge.CLIScriptContextUtils;
import de.leuphana.escience.dspacepurebridge.Constants;
import de.leuphana.escience.dspacepurebridge.DSpaceServicesContainer;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.*;
import de.leuphana.escience.dspacepurebridge.relations.EntityUtils;
import de.leuphana.escience.dspacepurebridge.search.ItemFinder;
import de.leuphana.escience.dspacepurebridge.search.SearchQueryType;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.service.*;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.sql.SQLException;
import java.util.*;

import static de.leuphana.escience.dspacepurebridge.Constants.LAST_MODIFICATION_DATE;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PureToDSpaceTest {

    @Mock
    private Context context;

    @Mock
    private ItemService itemService;

    @Mock
    private InstallItemService installItemService;

    @Mock
    private WorkspaceItemService workspaceItemService;

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private CollectionService collectionService;

    @Mock
    private HandleService handleService;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RelationshipTypeService relationshipTypeService;

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private SearchService searchService;

    @Spy
    private ItemFinder itemFinder;

    @Mock
    DSpaceServicesContainer dSpaceServicesContainer;

    @Spy
    @InjectMocks
    private PureToDSpace classUnderTest;

    @BeforeEach
    void setup() throws SearchServiceException {
        lenient().when(dSpaceServicesContainer.getConfigurationService()).thenReturn(configurationService);
        lenient().when(dSpaceServicesContainer.getItemService()).thenReturn(itemService);
        lenient().when(dSpaceServicesContainer.getHandleService()).thenReturn(handleService);
        lenient().when(dSpaceServicesContainer.getSearchService()).thenReturn(searchService);
        lenient().when(dSpaceServicesContainer.getCollectionService()).thenReturn(collectionService);
        lenient().when(dSpaceServicesContainer.getInstallItemService()).thenReturn(installItemService);
        lenient().when(dSpaceServicesContainer.getWorkspaceItemService()).thenReturn(workspaceItemService);
        lenient().when(dSpaceServicesContainer.getRelationshipTypeService()).thenReturn(relationshipTypeService);
        lenient().when(dSpaceServicesContainer.getRelationshipService()).thenReturn(relationshipService);
        PureToDSpace.pureEntityCache.clear();
        PureToDSpace.dspaceEntityCache.clear();
        doReturn(Collections.emptyIterator()).when(itemFinder).findItems(eq(context), eq(searchService), eq(SearchQueryType.PERSON_CACHE_IMPORT), anyInt(), anyInt());
        doReturn(Collections.emptyIterator()).when(itemFinder).findItems(eq(context), eq(searchService), eq(SearchQueryType.ORGANIZATION_CACHE_IMPORT), anyInt(), anyInt());
        doReturn(Collections.emptyIterator()).when(itemFinder).findItems(eq(context), eq(searchService), eq(SearchQueryType.PROJECT_CACHE_IMPORT), anyInt(), anyInt());
    }


    @Test
    void syncObjectsCreateThreadSafe() throws SQLException, AuthorizeException, SearchServiceException {
        try (MockedStatic<CLIScriptContextUtils> mockedUtils = mockStatic(CLIScriptContextUtils.class)) {
            mockedUtils.when(CLIScriptContextUtils::createReducedContext).thenReturn(context);

            String endpointBase = "http://localhost:8080/pure/";
            UUID uuid = UUID.randomUUID();

            when(classUnderTest.initRestTemplateBuilder()).thenReturn(restTemplateBuilder);
            when(classUnderTest.getPureWsEndpointBase()).thenReturn(endpointBase);
            doReturn(context).when(classUnderTest).createContext();
            doNothing().when(classUnderTest).replaceMetadataFromPure(any(), any(), any(), any(), any(), any());

            Collection collection = mock(Collection.class);
            when(handleService.resolveToObject(any(), any())).thenReturn(collection);
            Iterator<Item> iterator = mock(Iterator.class);
            when(iterator.hasNext()).thenReturn(false);
            when(itemService.findByCollection(context, collection)).thenReturn(iterator);

            RestTemplate restTemplate = mock(RestTemplate.class);
            when(restTemplateBuilder.build()).thenReturn(restTemplate);

            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityPersons =
                mock(ResponseEntity.class);
            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityProjects =
                mock(ResponseEntity.class);
            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityOrgUnits =
                mock(ResponseEntity.class);
            DSpacePureEntity.PureWSPersonResults persons = mock(DSpacePureEntity.PureWSPersonResults.class);
            DSpacePureEntity.PureWSOrganizationResults orgunits =
                mock(DSpacePureEntity.PureWSOrganizationResults.class);
            DSpacePureEntity.PureWSProjectResults projects = mock(DSpacePureEntity.PureWSProjectResults.class);

            PureWSResultPersonItem person1 = mock(PureWSResultPersonItem.class, Mockito.RETURNS_DEEP_STUBS);
            when(person1.getName()).thenReturn(new PersonName());
            PureWSResultPersonItem person2 = mock(PureWSResultPersonItem.class, Mockito.RETURNS_DEEP_STUBS);
            when(person2.getName()).thenReturn(new PersonName());

            when(person1.getUuid()).thenReturn(uuid);
            when(person2.getUuid()).thenReturn(uuid);
            when(persons.getItems()).thenReturn(List.of(person1, person2));
            when(orgunits.getItems()).thenReturn(Collections.emptyList());
            when(projects.getItems()).thenReturn(Collections.emptyList());

            WorkspaceItem workspaceItem = mock(WorkspaceItem.class);
            when(workspaceItemService.create(any(), any(), eq(false))).thenReturn(workspaceItem);
            Item createdItem = mock(Item.class);
            when(createdItem.getID()).thenReturn(UUID.randomUUID());
            when(workspaceItem.getItem()).thenReturn(createdItem);

            when(responseEntityPersons.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> persons);
            when(responseEntityOrgUnits.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> orgunits);
            when(responseEntityProjects.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> projects);
            when(restTemplate.getForEntity(anyString(), any())).thenAnswer(
                (Answer<ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>>>) invocationOnMock -> {
                    String url = invocationOnMock.getArgument(0);
                    if (url.contains(DSpacePureEntity.PERSON.getEndpoint())) {
                        return responseEntityPersons;
                    } else if (url.contains(DSpacePureEntity.ORGANIZATION.getEndpoint())) {
                        return responseEntityOrgUnits;
                    } else if (url.contains(DSpacePureEntity.PROJECT.getEndpoint())) {
                        return responseEntityProjects;
                    }
                    return null;
                });

            classUnderTest.syncObjects();
            Assertions.assertEquals(1, PureToDSpace.pureEntityCache.get(DSpacePureEntity.PERSON).size());
        }
    }

    @Test
    void syncPersonsRelate() throws SQLException, AuthorizeException, SearchServiceException {
        try (MockedStatic<CLIScriptContextUtils> mockedUtils = mockStatic(CLIScriptContextUtils.class)) {
            mockedUtils.when(CLIScriptContextUtils::createReducedContext).thenReturn(context);

            String firstName = "Karl";
            String lastName = "Schmidt";
            String endpointBase = "http://localhost:8080/pure/";
            UUID uuid = UUID.randomUUID();
            UUID dspaceItemUuid = UUID.randomUUID();
            UUID dspaceRelatedItemUuid = UUID.randomUUID();
            Item relatedItem = mock(Item.class);
            MetadataValue relatedItemLastnameMetadataValue = mock(MetadataValue.class);
            when(relatedItemLastnameMetadataValue.getValue()).thenReturn(lastName);
            MetadataValue relatedItemFirstnameMetadataValue = mock(MetadataValue.class);
            when(relatedItemFirstnameMetadataValue.getValue()).thenReturn(firstName);

            PureToDSpace.dspaceEntityCache.put(DSpacePureEntity.PERSON, Map.of(EntityUtils.generateEntityHash(
                    List.of(relatedItemLastnameMetadataValue), List.of(relatedItemFirstnameMetadataValue)),
                dspaceRelatedItemUuid));
            PureToDSpace.pureEntityCache.put(DSpacePureEntity.PERSON, Map.of(uuid, dspaceItemUuid));

            when(classUnderTest.initRestTemplateBuilder()).thenReturn(restTemplateBuilder);
            when(classUnderTest.getPureWsEndpointBase()).thenReturn(endpointBase);
            doReturn(context).when(classUnderTest).createContext();
            doNothing().when(classUnderTest).replaceMetadataFromPure(any(), any(), any(), any(), any(), any());

            Collection collection = mock(Collection.class);
            when(handleService.resolveToObject(any(), any())).thenReturn(collection);
            Iterator<Item> iterator = mock(Iterator.class);
            when(iterator.hasNext()).thenReturn(false);
            when(itemService.findByCollection(context, collection)).thenReturn(iterator);
            when(itemService.find(context, dspaceRelatedItemUuid)).thenReturn(relatedItem);

            RestTemplate restTemplate = mock(RestTemplate.class);
            when(restTemplateBuilder.build()).thenReturn(restTemplate);

            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityPersons =
                mock(ResponseEntity.class);
            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityProjects =
                mock(ResponseEntity.class);
            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityOrgUnits =
                mock(ResponseEntity.class);
            DSpacePureEntity.PureWSPersonResults persons = mock(DSpacePureEntity.PureWSPersonResults.class);
            DSpacePureEntity.PureWSOrganizationResults orgunits =
                mock(DSpacePureEntity.PureWSOrganizationResults.class);
            DSpacePureEntity.PureWSProjectResults projects = mock(DSpacePureEntity.PureWSProjectResults.class);

            PureWSResultPersonItem person1 = mock(PureWSResultPersonItem.class, Mockito.RETURNS_DEEP_STUBS);
            PersonName personName = mock(PersonName.class);
            when(personName.getLastName()).thenReturn(lastName);
            when(personName.getFirstName()).thenReturn(firstName);
            when(person1.getName()).thenReturn(personName);
            when(person1.getModifiedDate()).thenReturn("date2");

            when(person1.getUuid()).thenReturn(uuid);
            when(person1.getOrcid()).thenReturn("");
            when(persons.getItems()).thenReturn(List.of(person1));
            when(orgunits.getItems()).thenReturn(Collections.emptyList());
            when(projects.getItems()).thenReturn(Collections.emptyList());

            Item existingItem = mock(Item.class);
            MetadataValue lastModified = mock(MetadataValue.class);
            when(lastModified.getValue()).thenReturn("date1");
            when(itemService.find(context, dspaceItemUuid)).thenReturn(existingItem);
            when(itemService.getMetadata(existingItem, Constants.SCHEME, Constants.ELEMENT, LAST_MODIFICATION_DATE,
                Item.ANY)).thenReturn(List.of(lastModified));

            RelationshipType relationshipType = mock(RelationshipType.class);
            when(relationshipTypeService.findByLeftwardOrRightwardTypeName(context, "isPurePersonOfPerson")).thenReturn(
                Collections.singletonList(relationshipType));
            Relationship persistedRelationship = mock(Relationship.class);
            when(relationshipService.create(context,
                relatedItem,
                existingItem,
                relationshipType, 0,
                -1)).thenReturn(persistedRelationship);

            when(responseEntityPersons.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> persons);
            when(responseEntityOrgUnits.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> orgunits);
            when(responseEntityProjects.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> projects);
            when(restTemplate.getForEntity(anyString(), any())).thenAnswer(
                (Answer<ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>>>) invocationOnMock -> {
                    String url = invocationOnMock.getArgument(0);
                    if (url.contains(DSpacePureEntity.PERSON.getEndpoint())) {
                        return responseEntityPersons;
                    } else if (url.contains(DSpacePureEntity.ORGANIZATION.getEndpoint())) {
                        return responseEntityOrgUnits;
                    } else if (url.contains(DSpacePureEntity.PROJECT.getEndpoint())) {
                        return responseEntityProjects;
                    }
                    return null;
                });

            classUnderTest.syncObjects();

            verify(relationshipService, times(1)).create(context,
                relatedItem,
                existingItem,
                relationshipType, 0,
                -1);
            verify(relationshipService, times(1)).update(context, persistedRelationship);
        }
    }

    @Test
    void syncPersonsNoRelateAlreadyDone() throws SQLException, AuthorizeException, SearchServiceException {
        try (MockedStatic<CLIScriptContextUtils> mockedUtils = mockStatic(CLIScriptContextUtils.class)) {
            mockedUtils.when(CLIScriptContextUtils::createReducedContext).thenReturn(context);

            String firstName = "Karl";
            String lastName = "Schmidt";
            String endpointBase = "http://localhost:8080/pure/";
            UUID uuid = UUID.randomUUID();
            UUID dspaceItemUuid = UUID.randomUUID();
            UUID dspaceRelatedItemUuid = UUID.randomUUID();
            Item relatedItem = mock(Item.class);
            when(relatedItem.getID()).thenReturn(dspaceRelatedItemUuid);
            MetadataValue relatedItemLastnameMetadataValue = mock(MetadataValue.class);
            when(relatedItemLastnameMetadataValue.getValue()).thenReturn(lastName);
            MetadataValue relatedItemFirstnameMetadataValue = mock(MetadataValue.class);
            when(relatedItemFirstnameMetadataValue.getValue()).thenReturn(firstName);

            PureToDSpace.dspaceEntityCache.put(DSpacePureEntity.PERSON, Map.of(EntityUtils.generateEntityHash(
                    List.of(relatedItemLastnameMetadataValue), List.of(relatedItemFirstnameMetadataValue)),
                dspaceRelatedItemUuid));
            PureToDSpace.pureEntityCache.put(DSpacePureEntity.PERSON, Map.of(uuid, dspaceItemUuid));

            when(classUnderTest.initRestTemplateBuilder()).thenReturn(restTemplateBuilder);
            when(classUnderTest.getPureWsEndpointBase()).thenReturn(endpointBase);
            doReturn(context).when(classUnderTest).createContext();
            doNothing().when(classUnderTest).replaceMetadataFromPure(any(), any(), any(), any(), any(), any());

            Collection collection = mock(Collection.class);
            when(handleService.resolveToObject(any(), any())).thenReturn(collection);
            Iterator<Item> iterator = mock(Iterator.class);
            when(iterator.hasNext()).thenReturn(false);
            when(itemService.findByCollection(context, collection)).thenReturn(iterator);

            RestTemplate restTemplate = mock(RestTemplate.class);
            when(restTemplateBuilder.build()).thenReturn(restTemplate);

            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityPersons =
                mock(ResponseEntity.class);
            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityProjects =
                mock(ResponseEntity.class);
            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityOrgUnits =
                mock(ResponseEntity.class);
            DSpacePureEntity.PureWSPersonResults persons = mock(DSpacePureEntity.PureWSPersonResults.class);
            DSpacePureEntity.PureWSOrganizationResults orgunits =
                mock(DSpacePureEntity.PureWSOrganizationResults.class);
            DSpacePureEntity.PureWSProjectResults projects = mock(DSpacePureEntity.PureWSProjectResults.class);

            PureWSResultPersonItem person1 = mock(PureWSResultPersonItem.class, Mockito.RETURNS_DEEP_STUBS);
            PersonName personName = mock(PersonName.class);
            when(personName.getLastName()).thenReturn(lastName);
            when(personName.getFirstName()).thenReturn(firstName);
            when(person1.getName()).thenReturn(personName);
            when(person1.getModifiedDate()).thenReturn("date2");

            when(person1.getUuid()).thenReturn(uuid);
            when(person1.getOrcid()).thenReturn("");
            when(persons.getItems()).thenReturn(List.of(person1));
            when(orgunits.getItems()).thenReturn(Collections.emptyList());
            when(projects.getItems()).thenReturn(Collections.emptyList());

            Item existingItem = mock(Item.class);
            MetadataValue lastModified = mock(MetadataValue.class);
            when(lastModified.getValue()).thenReturn("date1");
            when(itemService.find(context, dspaceItemUuid)).thenReturn(existingItem);
            when(itemService.getMetadata(existingItem, Constants.SCHEME, Constants.ELEMENT, LAST_MODIFICATION_DATE,
                Item.ANY)).thenReturn(List.of(lastModified));

            RelationshipType relationshipType = mock(RelationshipType.class);
            when(relationshipTypeService.findByLeftwardOrRightwardTypeName(context, "isPurePersonOfPerson")).thenReturn(
                Collections.singletonList(relationshipType));
            Relationship existingRelationship = mock(Relationship.class);
            when(relationshipService.findByItemAndRelationshipType(context, existingItem, relationshipType)).thenReturn(
                Collections.singletonList(existingRelationship));

            when(existingRelationship.getLeftItem()).thenReturn(relatedItem);

            when(responseEntityPersons.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> persons);
            when(responseEntityOrgUnits.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> orgunits);
            when(responseEntityProjects.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> projects);
            when(restTemplate.getForEntity(anyString(), any())).thenAnswer(
                (Answer<ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>>>) invocationOnMock -> {
                    String url = invocationOnMock.getArgument(0);
                    if (url.contains(DSpacePureEntity.PERSON.getEndpoint())) {
                        return responseEntityPersons;
                    } else if (url.contains(DSpacePureEntity.ORGANIZATION.getEndpoint())) {
                        return responseEntityOrgUnits;
                    } else if (url.contains(DSpacePureEntity.PROJECT.getEndpoint())) {
                        return responseEntityProjects;
                    }
                    return null;
                });

            classUnderTest.syncObjects();

            verify(relationshipService, times(0)).create(context,
                relatedItem,
                existingItem,
                relationshipType, 0,
                -1);
            verify(relationshipService, times(0)).update(any(), ArgumentMatchers.<Relationship>any());
        }
    }

    @Test
    void syncPersonsModify() throws SQLException, SearchServiceException {
        try (MockedStatic<CLIScriptContextUtils> mockedUtils = mockStatic(CLIScriptContextUtils.class)) {
            mockedUtils.when(CLIScriptContextUtils::createReducedContext).thenReturn(context);

            String endpointBase = "http://localhost:8080/pure/";
            UUID uuid = UUID.randomUUID();
            UUID dspaceItemUuid = UUID.randomUUID();

            PureToDSpace.pureEntityCache.put(DSpacePureEntity.PERSON, Map.of(uuid, dspaceItemUuid));

            when(classUnderTest.initRestTemplateBuilder()).thenReturn(restTemplateBuilder);
            when(classUnderTest.getPureWsEndpointBase()).thenReturn(endpointBase);
            doReturn(context).when(classUnderTest).createContext();
            doNothing().when(classUnderTest).replaceMetadataFromPure(any(), any(), any(), any(), any(), any());

            Collection collection = mock(Collection.class);
            when(handleService.resolveToObject(any(), any())).thenReturn(collection);
            Iterator<Item> iterator = mock(Iterator.class);
            when(iterator.hasNext()).thenReturn(false);
            when(itemService.findByCollection(context, collection)).thenReturn(iterator);

            RestTemplate restTemplate = mock(RestTemplate.class);
            when(restTemplateBuilder.build()).thenReturn(restTemplate);

            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityPersons =
                mock(ResponseEntity.class);
            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityProjects =
                mock(ResponseEntity.class);
            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityOrgUnits =
                mock(ResponseEntity.class);
            DSpacePureEntity.PureWSPersonResults persons = mock(DSpacePureEntity.PureWSPersonResults.class);
            DSpacePureEntity.PureWSOrganizationResults orgunits =
                mock(DSpacePureEntity.PureWSOrganizationResults.class);
            DSpacePureEntity.PureWSProjectResults projects = mock(DSpacePureEntity.PureWSProjectResults.class);

            PureWSResultPersonItem person1 = mock(PureWSResultPersonItem.class, Mockito.RETURNS_DEEP_STUBS);
            when(person1.getName()).thenReturn(new PersonName());
            when(person1.getModifiedDate()).thenReturn("date2");

            when(person1.getUuid()).thenReturn(uuid);
            when(persons.getItems()).thenReturn(List.of(person1));
            when(orgunits.getItems()).thenReturn(Collections.emptyList());
            when(projects.getItems()).thenReturn(Collections.emptyList());

            Item existingItem = mock(Item.class);
            MetadataValue lastModified = mock(MetadataValue.class);
            when(lastModified.getValue()).thenReturn("date1");
            when(itemService.find(context, dspaceItemUuid)).thenReturn(existingItem);
            when(itemService.getMetadata(existingItem, Constants.SCHEME, Constants.ELEMENT, LAST_MODIFICATION_DATE,
                Item.ANY)).thenReturn(List.of(lastModified));

            when(responseEntityPersons.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> persons);
            when(responseEntityOrgUnits.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> orgunits);
            when(responseEntityProjects.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> projects);
            when(restTemplate.getForEntity(anyString(), any())).thenAnswer(
                (Answer<ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>>>) invocationOnMock -> {
                    String url = invocationOnMock.getArgument(0);
                    if (url.contains(DSpacePureEntity.PERSON.getEndpoint())) {
                        return responseEntityPersons;
                    } else if (url.contains(DSpacePureEntity.ORGANIZATION.getEndpoint())) {
                        return responseEntityOrgUnits;
                    } else if (url.contains(DSpacePureEntity.PROJECT.getEndpoint())) {
                        return responseEntityProjects;
                    }
                    return null;
                });

            classUnderTest.syncObjects();
            verify(classUnderTest, atLeast(1)).replaceMetadataFromPure(any(), any(), any(), any(), any(), any());
        }
    }

    @Test
    void syncPersonsModifyNoChange() throws SQLException, SearchServiceException {
        try (MockedStatic<CLIScriptContextUtils> mockedUtils = mockStatic(CLIScriptContextUtils.class)) {
            mockedUtils.when(CLIScriptContextUtils::createReducedContext).thenReturn(context);

            String endpointBase = "http://localhost:8080/pure/";
            UUID uuid = UUID.randomUUID();
            UUID dspaceItemUuid = UUID.randomUUID();

            PureToDSpace.pureEntityCache.put(DSpacePureEntity.PERSON, Map.of(uuid, dspaceItemUuid));

            when(classUnderTest.initRestTemplateBuilder()).thenReturn(restTemplateBuilder);
            when(classUnderTest.getPureWsEndpointBase()).thenReturn(endpointBase);
            doReturn(context).when(classUnderTest).createContext();

            Collection collection = mock(Collection.class);
            when(handleService.resolveToObject(any(), any())).thenReturn(collection);
            Iterator<Item> iterator = mock(Iterator.class);
            when(iterator.hasNext()).thenReturn(false);
            when(itemService.findByCollection(context, collection)).thenReturn(iterator);

            RestTemplate restTemplate = mock(RestTemplate.class);
            when(restTemplateBuilder.build()).thenReturn(restTemplate);

            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityPersons =
                mock(ResponseEntity.class);
            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityProjects =
                mock(ResponseEntity.class);
            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityOrgUnits =
                mock(ResponseEntity.class);
            DSpacePureEntity.PureWSPersonResults persons = mock(DSpacePureEntity.PureWSPersonResults.class);
            DSpacePureEntity.PureWSOrganizationResults orgunits =
                mock(DSpacePureEntity.PureWSOrganizationResults.class);
            DSpacePureEntity.PureWSProjectResults projects = mock(DSpacePureEntity.PureWSProjectResults.class);

            PureWSResultPersonItem person1 = mock(PureWSResultPersonItem.class, Mockito.RETURNS_DEEP_STUBS);
            when(person1.getName()).thenReturn(new PersonName());
            when(person1.getModifiedDate()).thenReturn("date1");

            when(person1.getUuid()).thenReturn(uuid);
            when(persons.getItems()).thenReturn(List.of(person1));
            when(orgunits.getItems()).thenReturn(Collections.emptyList());
            when(projects.getItems()).thenReturn(Collections.emptyList());

            Item existingItem = mock(Item.class);
            MetadataValue lastModified = mock(MetadataValue.class);
            when(lastModified.getValue()).thenReturn("date1");
            when(itemService.find(context, dspaceItemUuid)).thenReturn(existingItem);
            when(itemService.getMetadata(existingItem, Constants.SCHEME, Constants.ELEMENT, LAST_MODIFICATION_DATE,
                Item.ANY)).thenReturn(List.of(lastModified));

            when(responseEntityPersons.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> persons);
            when(responseEntityOrgUnits.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> orgunits);
            when(responseEntityProjects.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> projects);
            when(restTemplate.getForEntity(anyString(), any())).thenAnswer(
                (Answer<ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>>>) invocationOnMock -> {
                    String url = invocationOnMock.getArgument(0);
                    if (url.contains(DSpacePureEntity.PERSON.getEndpoint())) {
                        return responseEntityPersons;
                    } else if (url.contains(DSpacePureEntity.ORGANIZATION.getEndpoint())) {
                        return responseEntityOrgUnits;
                    } else if (url.contains(DSpacePureEntity.PROJECT.getEndpoint())) {
                        return responseEntityProjects;
                    }
                    return null;
                });

            classUnderTest.syncObjects();
            verify(classUnderTest, never()).replaceMetadataFromPure(any(), any(), any(), any(), any(), any());
        }
    }

    @Test
    void syncOrganizationsModify() throws SQLException, SearchServiceException {
        try (MockedStatic<CLIScriptContextUtils> mockedUtils = mockStatic(CLIScriptContextUtils.class)) {
            mockedUtils.when(CLIScriptContextUtils::createReducedContext).thenReturn(context);

            String endpointBase = "http://localhost:8080/pure/";
            UUID uuid = UUID.randomUUID();
            UUID dspaceItemUuid = UUID.randomUUID();

            PureToDSpace.pureEntityCache.put(DSpacePureEntity.ORGANIZATION, Map.of(uuid, dspaceItemUuid));

            when(classUnderTest.initRestTemplateBuilder()).thenReturn(restTemplateBuilder);
            when(classUnderTest.getPureWsEndpointBase()).thenReturn(endpointBase);
            doReturn(context).when(classUnderTest).createContext();
            doNothing().when(classUnderTest).replaceMetadataFromPure(any(), any(), any(), any(), any(), any());

            Collection collection = mock(Collection.class);
            when(handleService.resolveToObject(any(), any())).thenReturn(collection);
            Iterator<Item> iterator = mock(Iterator.class);
            when(iterator.hasNext()).thenReturn(false);
            when(itemService.findByCollection(context, collection)).thenReturn(iterator);

            RestTemplate restTemplate = mock(RestTemplate.class);
            when(restTemplateBuilder.build()).thenReturn(restTemplate);

            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityPersons =
                mock(ResponseEntity.class);
            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityProjects =
                mock(ResponseEntity.class);
            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityOrgUnits =
                mock(ResponseEntity.class);
            DSpacePureEntity.PureWSPersonResults persons = mock(DSpacePureEntity.PureWSPersonResults.class);
            DSpacePureEntity.PureWSOrganizationResults orgunits =
                mock(DSpacePureEntity.PureWSOrganizationResults.class);
            DSpacePureEntity.PureWSProjectResults projects = mock(DSpacePureEntity.PureWSProjectResults.class);

            PureWSResultOrganizationItem organization1 =
                mock(PureWSResultOrganizationItem.class, Mockito.RETURNS_DEEP_STUBS);
            Text name = mock(Text.class);
            when(name.getText()).thenReturn("name");
            when(organization1.getName()).thenReturn(name);
            when(organization1.getModifiedDate()).thenReturn("date2");

            when(organization1.getUuid()).thenReturn(uuid);
            when(persons.getItems()).thenReturn(Collections.emptyList());
            when(orgunits.getItems()).thenReturn(List.of(organization1));
            when(projects.getItems()).thenReturn(Collections.emptyList());

            Item existingItem = mock(Item.class);
            MetadataValue lastModified = mock(MetadataValue.class);
            when(lastModified.getValue()).thenReturn("date1");
            when(itemService.find(context, dspaceItemUuid)).thenReturn(existingItem);
            when(itemService.getMetadata(existingItem, Constants.SCHEME, Constants.ELEMENT, LAST_MODIFICATION_DATE,
                Item.ANY)).thenReturn(List.of(lastModified));

            when(responseEntityPersons.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> persons);
            when(responseEntityOrgUnits.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> orgunits);
            when(responseEntityProjects.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> projects);
            when(restTemplate.getForEntity(anyString(), any())).thenAnswer(
                (Answer<ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>>>) invocationOnMock -> {
                    String url = invocationOnMock.getArgument(0);
                    if (url.contains(DSpacePureEntity.PERSON.getEndpoint())) {
                        return responseEntityPersons;
                    } else if (url.contains(DSpacePureEntity.ORGANIZATION.getEndpoint())) {
                        return responseEntityOrgUnits;
                    } else if (url.contains(DSpacePureEntity.PROJECT.getEndpoint())) {
                        return responseEntityProjects;
                    }
                    return null;
                });

            classUnderTest.syncObjects();
            verify(classUnderTest, atLeast(1)).replaceMetadataFromPure(any(), any(), any(), any(), any(), any());
        }
    }

    @Test
    void syncOrganizationsModifyNoChange() throws SQLException, SearchServiceException {
        try (MockedStatic<CLIScriptContextUtils> mockedUtils = mockStatic(CLIScriptContextUtils.class)) {
            mockedUtils.when(CLIScriptContextUtils::createReducedContext).thenReturn(context);

            String endpointBase = "http://localhost:8080/pure/";
            UUID uuid = UUID.randomUUID();
            UUID dspaceItemUuid = UUID.randomUUID();

            PureToDSpace.pureEntityCache.put(DSpacePureEntity.ORGANIZATION, Map.of(uuid, dspaceItemUuid));

            when(classUnderTest.initRestTemplateBuilder()).thenReturn(restTemplateBuilder);
            when(classUnderTest.getPureWsEndpointBase()).thenReturn(endpointBase);
            doReturn(context).when(classUnderTest).createContext();

            Collection collection = mock(Collection.class);
            when(handleService.resolveToObject(any(), any())).thenReturn(collection);
            Iterator<Item> iterator = mock(Iterator.class);
            when(iterator.hasNext()).thenReturn(false);
            when(itemService.findByCollection(context, collection)).thenReturn(iterator);

            RestTemplate restTemplate = mock(RestTemplate.class);
            when(restTemplateBuilder.build()).thenReturn(restTemplate);

            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityPersons =
                mock(ResponseEntity.class);
            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityProjects =
                mock(ResponseEntity.class);
            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityOrgUnits =
                mock(ResponseEntity.class);
            DSpacePureEntity.PureWSPersonResults persons = mock(DSpacePureEntity.PureWSPersonResults.class);
            DSpacePureEntity.PureWSOrganizationResults orgunits =
                mock(DSpacePureEntity.PureWSOrganizationResults.class);
            DSpacePureEntity.PureWSProjectResults projects = mock(DSpacePureEntity.PureWSProjectResults.class);

            PureWSResultOrganizationItem organization1 =
                mock(PureWSResultOrganizationItem.class, Mockito.RETURNS_DEEP_STUBS);
            Text name = mock(Text.class);
            when(name.getText()).thenReturn("name");
            when(organization1.getName()).thenReturn(name);
            when(organization1.getModifiedDate()).thenReturn("date1");

            when(organization1.getUuid()).thenReturn(uuid);
            when(persons.getItems()).thenReturn(Collections.emptyList());
            when(orgunits.getItems()).thenReturn(List.of(organization1));
            when(projects.getItems()).thenReturn(Collections.emptyList());

            Item existingItem = mock(Item.class);
            MetadataValue lastModified = mock(MetadataValue.class);
            when(lastModified.getValue()).thenReturn("date1");
            when(itemService.find(context, dspaceItemUuid)).thenReturn(existingItem);
            when(itemService.getMetadata(existingItem, Constants.SCHEME, Constants.ELEMENT, LAST_MODIFICATION_DATE,
                Item.ANY)).thenReturn(List.of(lastModified));

            when(responseEntityPersons.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> persons);
            when(responseEntityOrgUnits.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> orgunits);
            when(responseEntityProjects.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> projects);
            when(restTemplate.getForEntity(anyString(), any())).thenAnswer(
                (Answer<ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>>>) invocationOnMock -> {
                    String url = invocationOnMock.getArgument(0);
                    if (url.contains(DSpacePureEntity.PERSON.getEndpoint())) {
                        return responseEntityPersons;
                    } else if (url.contains(DSpacePureEntity.ORGANIZATION.getEndpoint())) {
                        return responseEntityOrgUnits;
                    } else if (url.contains(DSpacePureEntity.PROJECT.getEndpoint())) {
                        return responseEntityProjects;
                    }
                    return null;
                });

            classUnderTest.syncObjects();
            verify(classUnderTest, never()).replaceMetadataFromPure(any(), any(), any(), any(), any(), any());
        }
    }

    @Test
    void syncProjectsModify() throws SQLException, SearchServiceException {
        try (MockedStatic<CLIScriptContextUtils> mockedUtils = mockStatic(CLIScriptContextUtils.class)) {
            mockedUtils.when(CLIScriptContextUtils::createReducedContext).thenReturn(context);

            String endpointBase = "http://localhost:8080/pure/";
            UUID uuid = UUID.randomUUID();
            UUID dspaceItemUuid = UUID.randomUUID();

            PureToDSpace.pureEntityCache.put(DSpacePureEntity.PROJECT, Map.of(uuid, dspaceItemUuid));

            when(classUnderTest.initRestTemplateBuilder()).thenReturn(restTemplateBuilder);
            when(classUnderTest.getPureWsEndpointBase()).thenReturn(endpointBase);
            doReturn(context).when(classUnderTest).createContext();
            doNothing().when(classUnderTest).replaceMetadataFromPure(any(), any(), any(), any(), any(), any());

            Collection collection = mock(Collection.class);
            when(handleService.resolveToObject(any(), any())).thenReturn(collection);
            Iterator<Item> iterator = mock(Iterator.class);
            when(iterator.hasNext()).thenReturn(false);
            when(itemService.findByCollection(context, collection)).thenReturn(iterator);

            RestTemplate restTemplate = mock(RestTemplate.class);
            when(restTemplateBuilder.build()).thenReturn(restTemplate);

            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityPersons =
                mock(ResponseEntity.class);
            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityProjects =
                mock(ResponseEntity.class);
            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityOrgUnits =
                mock(ResponseEntity.class);
            DSpacePureEntity.PureWSPersonResults persons = mock(DSpacePureEntity.PureWSPersonResults.class);
            DSpacePureEntity.PureWSOrganizationResults orgunits =
                mock(DSpacePureEntity.PureWSOrganizationResults.class);
            DSpacePureEntity.PureWSProjectResults projects = mock(DSpacePureEntity.PureWSProjectResults.class);

            PureWSResultProjectItem project1 = mock(PureWSResultProjectItem.class, Mockito.RETURNS_DEEP_STUBS);
            Text name = mock(Text.class);
            when(name.getText()).thenReturn("name");
            when(project1.getTitle()).thenReturn(name);
            when(project1.getModifiedDate()).thenReturn("date2");

            when(project1.getUuid()).thenReturn(uuid);
            when(persons.getItems()).thenReturn(Collections.emptyList());
            when(orgunits.getItems()).thenReturn(Collections.emptyList());
            when(projects.getItems()).thenReturn(List.of(project1));

            Item existingItem = mock(Item.class);
            MetadataValue lastModified = mock(MetadataValue.class);
            when(lastModified.getValue()).thenReturn("date1");
            when(itemService.find(context, dspaceItemUuid)).thenReturn(existingItem);
            when(itemService.getMetadata(existingItem, Constants.SCHEME, Constants.ELEMENT, LAST_MODIFICATION_DATE,
                Item.ANY)).thenReturn(List.of(lastModified));

            when(responseEntityPersons.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> persons);
            when(responseEntityOrgUnits.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> orgunits);
            when(responseEntityProjects.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> projects);
            when(restTemplate.getForEntity(anyString(), any())).thenAnswer(
                (Answer<ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>>>) invocationOnMock -> {
                    String url = invocationOnMock.getArgument(0);
                    if (url.contains(DSpacePureEntity.PERSON.getEndpoint())) {
                        return responseEntityPersons;
                    } else if (url.contains(DSpacePureEntity.ORGANIZATION.getEndpoint())) {
                        return responseEntityOrgUnits;
                    } else if (url.contains(DSpacePureEntity.PROJECT.getEndpoint())) {
                        return responseEntityProjects;
                    }
                    return null;
                });

            classUnderTest.syncObjects();
            verify(classUnderTest, atLeast(1)).replaceMetadataFromPure(any(), any(), any(), any(), any(), any());
        }
    }

    @Test
    void syncProjectsModifyNoChange() throws SQLException, SearchServiceException {
        try (MockedStatic<CLIScriptContextUtils> mockedUtils = mockStatic(CLIScriptContextUtils.class)) {
            mockedUtils.when(CLIScriptContextUtils::createReducedContext).thenReturn(context);

            String endpointBase = "http://localhost:8080/pure/";
            UUID uuid = UUID.randomUUID();
            UUID dspaceItemUuid = UUID.randomUUID();

            PureToDSpace.pureEntityCache.put(DSpacePureEntity.PROJECT, Map.of(uuid, dspaceItemUuid));

            when(classUnderTest.initRestTemplateBuilder()).thenReturn(restTemplateBuilder);
            when(classUnderTest.getPureWsEndpointBase()).thenReturn(endpointBase);
            doReturn(context).when(classUnderTest).createContext();

            Collection collection = mock(Collection.class);
            when(handleService.resolveToObject(any(), any())).thenReturn(collection);
            Iterator<Item> iterator = mock(Iterator.class);
            when(iterator.hasNext()).thenReturn(false);
            when(itemService.findByCollection(context, collection)).thenReturn(iterator);

            RestTemplate restTemplate = mock(RestTemplate.class);
            when(restTemplateBuilder.build()).thenReturn(restTemplate);

            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityPersons =
                mock(ResponseEntity.class);
            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityProjects =
                mock(ResponseEntity.class);
            ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntityOrgUnits =
                mock(ResponseEntity.class);
            DSpacePureEntity.PureWSPersonResults persons = mock(DSpacePureEntity.PureWSPersonResults.class);
            DSpacePureEntity.PureWSOrganizationResults orgunits =
                mock(DSpacePureEntity.PureWSOrganizationResults.class);
            DSpacePureEntity.PureWSProjectResults projects = mock(DSpacePureEntity.PureWSProjectResults.class);

            PureWSResultProjectItem project1 = mock(PureWSResultProjectItem.class, Mockito.RETURNS_DEEP_STUBS);
            Text name = mock(Text.class);
            when(name.getText()).thenReturn("name");
            when(project1.getTitle()).thenReturn(name);
            when(project1.getModifiedDate()).thenReturn("date1");

            when(project1.getUuid()).thenReturn(uuid);
            when(persons.getItems()).thenReturn(Collections.emptyList());
            when(orgunits.getItems()).thenReturn(Collections.emptyList());
            when(projects.getItems()).thenReturn(List.of(project1));

            Item existingItem = mock(Item.class);
            MetadataValue lastModified = mock(MetadataValue.class);
            when(lastModified.getValue()).thenReturn("date1");
            when(itemService.find(context, dspaceItemUuid)).thenReturn(existingItem);
            when(itemService.getMetadata(existingItem, Constants.SCHEME, Constants.ELEMENT, LAST_MODIFICATION_DATE,
                Item.ANY)).thenReturn(List.of(lastModified));

            when(responseEntityPersons.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> persons);
            when(responseEntityOrgUnits.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> orgunits);
            when(responseEntityProjects.getBody()).thenAnswer(
                (Answer<? extends PureWSResults<? extends PureWSResultItem>>) invocationOnMock -> projects);
            when(restTemplate.getForEntity(anyString(), any())).thenAnswer(
                (Answer<ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>>>) invocationOnMock -> {
                    String url = invocationOnMock.getArgument(0);
                    if (url.contains(DSpacePureEntity.PERSON.getEndpoint())) {
                        return responseEntityPersons;
                    } else if (url.contains(DSpacePureEntity.ORGANIZATION.getEndpoint())) {
                        return responseEntityOrgUnits;
                    } else if (url.contains(DSpacePureEntity.PROJECT.getEndpoint())) {
                        return responseEntityProjects;
                    }
                    return null;
                });

            classUnderTest.syncObjects();
            verify(classUnderTest, never()).replaceMetadataFromPure(any(), any(), any(), any(), any(), any());
        }
    }

    @Test
    void testPrepareCaches() throws SQLException, SearchServiceException {
        Collection mockPurePersonEntityCollection = mock(Collection.class);
        Collection mockPureOrgUnitEntityCollection = mock(Collection.class);
        Collection mockPureProjectEntityCollection = mock(Collection.class);

        Mockito.reset(itemFinder);

        String personEntityHash = "personHash";
        String orgUnitEntityHash = "orgUnitHash";
        String projectEntityHash = "projectHash";

        UUID purePersonEntityUuid = UUID.randomUUID();
        UUID purePersonEntityPureId = UUID.randomUUID();
        Item mockPurePersonEntityItem = mock(Item.class);
        when(mockPurePersonEntityItem.getID()).thenReturn(purePersonEntityUuid);
        MetadataValue purePersonEntityMetadata = mock(MetadataValue.class);
        when(purePersonEntityMetadata.getValue()).thenReturn(purePersonEntityPureId.toString());
        when(itemService.getMetadata(mockPurePersonEntityItem, Constants.SCHEME, Constants.ELEMENT,
            Constants.UUID_QUALIFIER, Item.ANY, false)).thenReturn(List.of(purePersonEntityMetadata));

        UUID pureOrgUnitEntityUuid = UUID.randomUUID();
        UUID pureOrgUnitEntityPureId = UUID.randomUUID();
        Item mockPureOrgUnitEntityItem = mock(Item.class);
        MetadataValue pureOrgUnitEntityMetadata = mock(MetadataValue.class);
        when(pureOrgUnitEntityMetadata.getValue()).thenReturn(pureOrgUnitEntityPureId.toString());
        when(mockPureOrgUnitEntityItem.getID()).thenReturn(pureOrgUnitEntityUuid);
        when(itemService.getMetadata(mockPureOrgUnitEntityItem, Constants.SCHEME, Constants.ELEMENT,
            Constants.UUID_QUALIFIER, Item.ANY, false)).thenReturn(List.of(pureOrgUnitEntityMetadata));

        UUID pureProjectEntityUuid = UUID.randomUUID();
        UUID pureProjectEntityPureId = UUID.randomUUID();
        Item mockPureProjectEntityItem = mock(Item.class);
        when(mockPureProjectEntityItem.getID()).thenReturn(pureProjectEntityUuid);
        MetadataValue pureProjectEntityMetadata = mock(MetadataValue.class);
        when(pureProjectEntityMetadata.getValue()).thenReturn(pureProjectEntityPureId.toString());
        when(itemService.getMetadata(mockPureProjectEntityItem, Constants.SCHEME, Constants.ELEMENT,
            Constants.UUID_QUALIFIER, Item.ANY, false)).thenReturn(List.of(pureProjectEntityMetadata));

        Item dspacePersonEntityItem = mock(Item.class);
        UUID dspacePersonEntityUuid = UUID.randomUUID();
        when(dspacePersonEntityItem.getID()).thenReturn(dspacePersonEntityUuid);
        doReturn(List.of(dspacePersonEntityItem).iterator()).when(itemFinder).findItems(eq(context), eq(searchService), eq(SearchQueryType.PERSON_CACHE_IMPORT), anyInt(), anyInt());

        Item dspaceOrgUnitEntityItem = mock(Item.class);
        UUID dspaceOrgUnitEntityUuid = UUID.randomUUID();
        when(dspaceOrgUnitEntityItem.getID()).thenReturn(dspaceOrgUnitEntityUuid);
        doReturn(List.of(dspaceOrgUnitEntityItem).iterator()).when(itemFinder).findItems(eq(context), eq(searchService), eq(SearchQueryType.ORGANIZATION_CACHE_IMPORT), anyInt(), anyInt());

        Item dspaceProjectEntityItem = mock(Item.class);
        UUID dspaceProjectEntityUuid = UUID.randomUUID();
        when(dspaceProjectEntityItem.getID()).thenReturn(dspaceProjectEntityUuid);
        doReturn(List.of(dspaceProjectEntityItem).iterator()).when(itemFinder).findItems(eq(context), eq(searchService), eq(SearchQueryType.PROJECT_CACHE_IMPORT), anyInt(), anyInt());

        when(configurationService.getProperty("dspace-pure-bridge.entities.purePerson.collection")).thenReturn("purePersonCollection");
        when(configurationService.getProperty("dspace-pure-bridge.entities.pureOrgUnit.collection")).thenReturn("pureOrgUnitCollection");
        when(configurationService.getProperty("dspace-pure-bridge.entities.pureProject.collection")).thenReturn("pureProjectCollection");

        when(handleService.resolveToObject(context,  DSpacePureEntity.PERSON.getDspacePureEntityCollectionHandle(configurationService))).thenReturn(mockPurePersonEntityCollection);
        when(handleService.resolveToObject(context,  DSpacePureEntity.ORGANIZATION.getDspacePureEntityCollectionHandle(configurationService))).thenReturn(mockPureOrgUnitEntityCollection);
        when(handleService.resolveToObject(context,  DSpacePureEntity.PROJECT.getDspacePureEntityCollectionHandle(configurationService))).thenReturn(mockPureProjectEntityCollection);

        when(itemService.findByCollection(context, mockPurePersonEntityCollection))
                .thenReturn(List.of(mockPurePersonEntityItem).iterator());
        when(itemService.findByCollection(context, mockPureOrgUnitEntityCollection))
                .thenReturn(List.of(mockPureOrgUnitEntityItem).iterator());
        when(itemService.findByCollection(context, mockPureProjectEntityCollection))
                .thenReturn(List.of(mockPureProjectEntityItem).iterator());

        // Mock entity hash generation
        try (MockedStatic<EntityUtils> mockedEntityUtils = mockStatic(EntityUtils.class);
             MockedStatic<CLIScriptContextUtils> mockedUtils = mockStatic(CLIScriptContextUtils.class)) {
            mockedUtils.when(CLIScriptContextUtils::createReducedContext).thenReturn(context);
            mockedEntityUtils.when(() -> EntityUtils.generatePersonEntityHash(itemService, dspacePersonEntityItem))
                .thenReturn(personEntityHash);
            mockedEntityUtils.when(() -> EntityUtils.generateOrgUnitNameEntityHash(itemService, dspaceOrgUnitEntityItem))
                .thenReturn(orgUnitEntityHash);
            mockedEntityUtils.when(() -> EntityUtils.generateProjectNameEntityHash(itemService, dspaceProjectEntityItem))
                .thenReturn(projectEntityHash);

            // Act
            classUnderTest.prepareCaches();

            Assertions.assertEquals(purePersonEntityUuid, PureToDSpace.pureEntityCache.get(DSpacePureEntity.PERSON).get(purePersonEntityPureId));
            Assertions.assertEquals(pureOrgUnitEntityUuid, PureToDSpace.pureEntityCache.get(DSpacePureEntity.ORGANIZATION).get(pureOrgUnitEntityPureId));
            Assertions.assertEquals(pureProjectEntityUuid, PureToDSpace.pureEntityCache.get(DSpacePureEntity.PROJECT).get(pureProjectEntityPureId));
            Assertions.assertEquals(dspacePersonEntityUuid, PureToDSpace.dspaceEntityCache.get(DSpacePureEntity.PERSON).get(personEntityHash));
            Assertions.assertEquals(dspaceOrgUnitEntityUuid, PureToDSpace.dspaceEntityCache.get(DSpacePureEntity.ORGANIZATION).get(orgUnitEntityHash));
            Assertions.assertEquals(dspaceProjectEntityUuid, PureToDSpace.dspaceEntityCache.get(DSpacePureEntity.PROJECT).get(projectEntityHash));
        }
    }
}