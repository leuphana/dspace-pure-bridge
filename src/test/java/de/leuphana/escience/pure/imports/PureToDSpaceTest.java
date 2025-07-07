/*
 * *
 *  * The contents of this file are subject to the license and copyright
 *  * detailed in the LICENSE and NOTICE files at the root of the source
 *  * tree and available online at
 *  *
 *  * http://www.dspace.org/license/
 *
 */

package de.leuphana.escience.pure.imports;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import de.leuphana.escience.CLIScriptContextUtils;
import de.leuphana.escience.pure.apiobjects.PersonName;
import de.leuphana.escience.pure.apiobjects.PureWSResultItem;
import de.leuphana.escience.pure.apiobjects.PureWSResultPersonItem;
import de.leuphana.escience.pure.apiobjects.PureWSResults;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@RunWith(MockitoJUnitRunner.class)
public class PureToDSpaceTest {

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

    @Spy
    @InjectMocks
    private PureToDSpace classUnderTest;

    @Test
    public void syncObjectsCreateThreadSafe() throws SQLException, AuthorizeException {
        try (MockedStatic<CLIScriptContextUtils> mockedUtils = mockStatic(CLIScriptContextUtils.class)) {
            // Mock an IP address for mydspace.edu (have it return 1.2.3.4 as the IP address)
            mockedUtils.when(() -> CLIScriptContextUtils.createReducedContext()).thenReturn(context);

            String endpointBase = "http://localhost:8080/pure/";
            UUID uuid = UUID.randomUUID();

            when(classUnderTest.initRestTemplateBuilder()).thenReturn(restTemplateBuilder);
            when(classUnderTest.getLeuphanaPureWsEndpointBase()).thenReturn(endpointBase);
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
            when(orgunits.getItems()).thenReturn(Collections.EMPTY_LIST);
            when(projects.getItems()).thenReturn(Collections.EMPTY_LIST);

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
            assertEquals(1, PureToDSpace.pureEntityCache.get(DSpacePureEntity.PERSON).size());
        }
    }
}