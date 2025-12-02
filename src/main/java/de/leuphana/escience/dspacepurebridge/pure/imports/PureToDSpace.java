package de.leuphana.escience.dspacepurebridge.pure.imports;

import de.leuphana.escience.dspacepurebridge.CLIScriptContextUtils;
import de.leuphana.escience.dspacepurebridge.Constants;
import de.leuphana.escience.dspacepurebridge.DSpaceServicesContainer;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.*;
import de.leuphana.escience.dspacepurebridge.relations.EntityUtils;
import de.leuphana.escience.dspacepurebridge.relations.RelationShipUtils;
import de.leuphana.escience.dspacepurebridge.search.ItemFinder;
import de.leuphana.escience.dspacepurebridge.search.ItemProcessor;
import de.leuphana.escience.dspacepurebridge.search.SearchQueryType;
import org.apache.commons.codec.digest.DigestUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.service.*;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static de.leuphana.escience.dspacepurebridge.Constants.LAST_MODIFICATION_DATE;

public class PureToDSpace {
    private static final Logger log = LoggerFactory.getLogger(PureToDSpace.class);
    static final Map<DSpacePureEntity, Map<UUID, UUID>> pureEntityCache = new ConcurrentHashMap<>();
    static final Map<DSpacePureEntity, Map<String, UUID>> dspaceEntityCache = new ConcurrentHashMap<>();
    private final Map<DSpacePureEntity, UUID> collectionMap = new EnumMap<>(DSpacePureEntity.class);
    private final String pureWsEndpointBase;
    private final String pureWsApiKey;
    private final DSpaceServicesContainer dSpaceServicesContainer;
    private final ItemFinder itemFinder;

    private static final String PURE_WS_PAGESIZE = "dspace-pure-bridge.pure.ws.pagesize";

    RestTemplateBuilder initRestTemplateBuilder() {
        return new RestTemplateBuilder()
                .requestFactory(HttpComponentsClientHttpRequestFactory.class)
                .connectTimeout(Duration.ofMillis(60000))
                .readTimeout(Duration.ofMillis(60000))
                .defaultHeader("api-key", pureWsApiKey);
    }

    public PureToDSpace(String pureWsEndpointBase, String pureureWsApiKey, DSpaceServicesContainer dSpaceServicesContainer,
                        ItemFinder itemFinder) {
        this.pureWsEndpointBase = pureWsEndpointBase;
        this.pureWsApiKey = pureureWsApiKey;
        this.dSpaceServicesContainer = dSpaceServicesContainer;
        this.itemFinder = itemFinder;
    }

    void prepareCaches() throws SQLException, SearchServiceException {
        Context context = CLIScriptContextUtils.createReducedContext();
        for (DSpacePureEntity dSpacePureEntity : DSpacePureEntity.values()) {
            preparePureEntityCache(context, dSpacePureEntity);
            prepareDspaceEntityCache(context, dSpacePureEntity);
        }

        CLIScriptContextUtils.closeContext(context);
    }

    void prepareDspaceEntityCache(Context context, DSpacePureEntity dSpacePureEntity) throws SearchServiceException {
        dspaceEntityCache.putIfAbsent(dSpacePureEntity, new ConcurrentHashMap<>());
        SearchQueryType searchQueryType = getDSpaceEntitiesSearchQueryType(dSpacePureEntity);
        itemFinder.processAllItems(context, dSpaceServicesContainer.getSearchService(), searchQueryType, (ItemProcessor) item -> {
            String entityHash = null;

            if (DSpacePureEntity.ORGANIZATION.equals(dSpacePureEntity)) {
                entityHash = EntityUtils.generateOrgUnitNameEntityHash(dSpaceServicesContainer.getItemService(), item);
            } else if (DSpacePureEntity.PERSON.equals(dSpacePureEntity)) {
                entityHash = EntityUtils.generatePersonEntityHash(dSpaceServicesContainer.getItemService(), item);
            } else if (DSpacePureEntity.PROJECT.equals(dSpacePureEntity)) {
                entityHash = EntityUtils.generateProjectNameEntityHash(dSpaceServicesContainer.getItemService(), item);
            }
            if (entityHash != null) {
                dspaceEntityCache.get(dSpacePureEntity).putIfAbsent(entityHash, item.getID());
            }
        });
        log.info("Number of 'DSpace' entities for type '{}': {}", dSpacePureEntity, dspaceEntityCache.get(dSpacePureEntity).size());
    }

    void preparePureEntityCache(Context context, DSpacePureEntity dSpacePureEntity) throws SQLException, SearchServiceException {
        pureEntityCache.putIfAbsent(dSpacePureEntity, new ConcurrentHashMap<>());

        Collection entityCollection = (Collection) dSpaceServicesContainer.getHandleService()
                .resolveToObject(context,
                        dSpacePureEntity.getDspacePureEntityCollectionHandle(dSpaceServicesContainer.getConfigurationService()));
        collectionMap.put(dSpacePureEntity, entityCollection.getID());
        Iterator<Item> entities = dSpaceServicesContainer.getItemService().findByCollection(context, entityCollection);
        while (entities.hasNext()) {
            Item item = entities.next();
            List<MetadataValue> metadata =
                    dSpaceServicesContainer.getItemService().getMetadata(item, Constants.SCHEME, Constants.ELEMENT, Constants.UUID_QUALIFIER,
                            Item.ANY,
                            false);
            if (!metadata.isEmpty()) {
                pureEntityCache.get(dSpacePureEntity)
                        .putIfAbsent(UUID.fromString(metadata.get(0).getValue()), item.getID());
            }
        }
        log.info("Number of 'Pure' entities for type '{}': {}", dSpacePureEntity, pureEntityCache.get(dSpacePureEntity).size());
    }

    @Nullable
    private static SearchQueryType getDSpaceEntitiesSearchQueryType(DSpacePureEntity dSpacePureEntity) {
        SearchQueryType searchQueryType = null;
        if (DSpacePureEntity.PERSON.equals(dSpacePureEntity)) {
            searchQueryType = SearchQueryType.PERSON_CACHE_IMPORT;
        } else if (DSpacePureEntity.ORGANIZATION.equals(dSpacePureEntity)) {
            searchQueryType = SearchQueryType.ORGANIZATION_CACHE_IMPORT;
        } else if (DSpacePureEntity.PROJECT.equals(dSpacePureEntity)) {
            searchQueryType = SearchQueryType.PROJECT_CACHE_IMPORT;
        }
        return searchQueryType;
    }

    Context createContext() throws SQLException {
        return CLIScriptContextUtils.createReducedContext();
    }

    public void syncObjects() throws SQLException, SearchServiceException {
        prepareCaches();

        int pureWsPageSize = 100;
        if (dSpaceServicesContainer.getConfigurationService().hasProperty(PURE_WS_PAGESIZE)) {
            pureWsPageSize = dSpaceServicesContainer.getConfigurationService().getIntProperty(PURE_WS_PAGESIZE);
        }

        RestTemplateBuilder clientBuilder = initRestTemplateBuilder();
        RestTemplate restTemplate = clientBuilder.build();

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        log.info("Available processors: {}", availableProcessors);
        ExecutorService pureSyncerThreadPool =
                Executors.newFixedThreadPool(availableProcessors);

        for (DSpacePureEntity dSpacePureEntity : DSpacePureEntity.values()) {
            int offset = 0;
            boolean itemsLeft = true;

            do {
                String webServiceUrl =
                        getPureWsEndpointBase() + dSpacePureEntity.getEndpoint() + "?offset=" + offset + "&size=" +
                                pureWsPageSize + "&fields" +
                                "=uuid,info.modifiedDate,title.*,name.*";
                log.info("WebService Call: {}", webServiceUrl);
                ResponseEntity<? extends PureWSResults<? extends PureWSResultItem>> responseEntity =
                        restTemplate.getForEntity(webServiceUrl,
                                dSpacePureEntity.getResultsClass());

                PureWSResults<? extends PureWSResultItem> pureWSResults = responseEntity.getBody();
                if (pureWSResults.getItems() == null || pureWSResults.getItems().size() < pureWsPageSize) {
                    itemsLeft = false;
                } else {
                    offset += pureWsPageSize;
                }

                for (PureWSResultItem pureWSResultItem : pureWSResults.getItems()) {
                    pureSyncerThreadPool.execute(() -> {
                        Context context = null;
                        AtomicBoolean pureItemChanged = new AtomicBoolean(false);
                        try {
                            context = createContext();

                            Item resultItem;
                            if (pureEntityCache.get(dSpacePureEntity).containsKey(pureWSResultItem.getUuid())) {
                                resultItem = dSpaceServicesContainer.getItemService().find(context,
                                        pureEntityCache.get(dSpacePureEntity)
                                                .get(pureWSResultItem.getUuid()));
                                List<MetadataValue> lastModified =
                                        dSpaceServicesContainer.getItemService().getMetadata(resultItem, Constants.SCHEME, Constants.ELEMENT,
                                                LAST_MODIFICATION_DATE,
                                                Item.ANY);
                                if (!lastModified.isEmpty() &&
                                        lastModified.get(0).getValue()
                                                .equals(pureWSResultItem.getModifiedDate())) {
                                    log.info("Skipping {} with pureId: {}, lastModificationDate not changed",
                                            dSpacePureEntity.getDspacePureEntity(), pureWSResultItem.getUuid());
                                } else {
                                    pureItemChanged.set(true);
                                    log.info("Updating {} with pureId: {}", dSpacePureEntity.getDspacePureEntity(),
                                            pureWSResultItem.getUuid());
                                }
                            } else {
                                Context finalContext = context;
                                UUID itemId =
                                        pureEntityCache.get(dSpacePureEntity)
                                                .computeIfAbsent(pureWSResultItem.getUuid(), k -> {
                                                    log.info("Creating {} with pureId: {}",
                                                            dSpacePureEntity.getDspacePureEntity(),
                                                            pureWSResultItem.getUuid());
                                                    try {
                                                        Collection collection =
                                                                dSpaceServicesContainer.getCollectionService().find(finalContext, collectionMap
                                                                        .get
                                                                                (dSpacePureEntity));

                                                        WorkspaceItem workspaceItem =
                                                                dSpaceServicesContainer.getWorkspaceItemService().create(finalContext, collection, false);
                                                        Item createdItem = workspaceItem.getItem();
                                                        dSpaceServicesContainer.getItemService().addMetadata(finalContext, createdItem, "dspace", "entity",
                                                                "type",
                                                                null,
                                                                dSpacePureEntity.getDspacePureEntity());
                                                        dSpaceServicesContainer.getItemService().addMetadata(finalContext, createdItem, Constants.SCHEME,
                                                                Constants.ELEMENT,
                                                                Constants.UUID_QUALIFIER, null,
                                                                String.valueOf(pureWSResultItem.getUuid()));

                                                        dSpaceServicesContainer.getInstallItemService().installItem(finalContext, workspaceItem);
                                                        pureItemChanged.set(true);
                                                        return createdItem.getID();
                                                    } catch (Exception e) {
                                                        return null;
                                                    }
                                                });
                                resultItem = dSpaceServicesContainer.getItemService().find(finalContext, itemId);
                            }

                            if (pureItemChanged.get()) {
                                replaceMetadataFromPure(context, resultItem, Constants.SCHEME, Constants.ELEMENT,
                                        LAST_MODIFICATION_DATE,
                                        pureWSResultItem.getModifiedDate());
                                replaceMetadataFromPure(context, resultItem, Constants.SCHEME, Constants.ELEMENT,
                                        Constants.VISIBILITY_QUALIFIER,
                                        pureWSResultItem.getVisibility().getKey());
                                replaceMetadataFromPure(context, resultItem, Constants.SCHEME, Constants.ELEMENT,
                                        Constants.ID_QUALIFIER,
                                        String.valueOf(pureWSResultItem.getPureId()));
                            }
                            String pureEntityHash = null;
                            if (dSpacePureEntity.equals(DSpacePureEntity.ORGANIZATION)) {
                                PureWSResultOrganizationItem orgUnitItem =
                                        (PureWSResultOrganizationItem) pureWSResultItem;
                                if (pureItemChanged.get()) {
                                    replaceMetadataFromPure(context, resultItem, "organization", "legalName", null,
                                            orgUnitItem.getName().getText());
                                }
                                pureEntityHash = DigestUtils.sha256Hex(orgUnitItem.getName().getText());
                            } else if (dSpacePureEntity.equals(DSpacePureEntity.PROJECT)) {
                                PureWSResultProjectItem projectItem = (PureWSResultProjectItem) pureWSResultItem;
                                if (pureItemChanged.get()) {
                                    replaceMetadataFromPure(context, resultItem, "dc", "title", null,
                                            projectItem.getTitle().getText());
                                }
                                pureEntityHash = DigestUtils.sha256Hex(projectItem.getTitle().getText());
                            } else if (dSpacePureEntity.equals(DSpacePureEntity.PERSON)) {
                                PureWSResultPersonItem personItem = (PureWSResultPersonItem) pureWSResultItem;
                                if (pureItemChanged.get()) {
                                    replaceMetadataFromPure(context, resultItem, "person", "givenName", null,
                                            personItem.getName().getFirstName());
                                    replaceMetadataFromPure(context, resultItem, "person", "identifier", "orcid",
                                            personItem.getOrcid());
                                    replaceMetadataFromPure(context, resultItem, "person", "familyName", null,
                                            personItem.getName().getLastName());
                                }
                                pureEntityHash = DigestUtils.sha256Hex(personItem.getName().getLastName() +
                                        personItem.getName().getFirstName() +
                                        personItem.getOrcid());
                            }

                            if (pureEntityHash != null &&
                                    dspaceEntityCache.get(dSpacePureEntity).containsKey(pureEntityHash)) {
                                boolean relate = true;
                                UUID relatedUUID = dspaceEntityCache.get(dSpacePureEntity).get(pureEntityHash);
                                RelationshipType relationshipType = RelationShipUtils.getRelationshipTypeForName(
                                        context,
                                        dSpaceServicesContainer.getRelationshipTypeService(),
                                        dSpacePureEntity.getDspaceEntityRelationshipType());
                                List<Relationship> byItemAndRelationshipType =
                                        dSpaceServicesContainer.getRelationshipService().findByItemAndRelationshipType(
                                                context, resultItem, relationshipType);

                                for (Relationship relationship : byItemAndRelationshipType) {
                                    if (relatedUUID.equals(relationship.getLeftItem().getID())) {
                                        relate = false;
                                        break;
                                    }
                                }
                                if (relate) {
                                    Item relatedItem = dSpaceServicesContainer.getItemService().find(context, relatedUUID);
                                    log.info("Creating relationship {} between {} and {}",
                                            dSpacePureEntity.getDspaceEntityRelationshipType(),
                                            relatedItem.getHandle(),
                                            resultItem.getHandle());
                                    Relationship persistedRelationship = dSpaceServicesContainer.getRelationshipService().create(context,
                                            relatedItem,
                                            resultItem,
                                            relationshipType, 0,
                                            -1);
                                    dSpaceServicesContainer.getRelationshipService().update(context, persistedRelationship);
                                }
                            }
                        } catch (SQLException | AuthorizeException e) {
                            if (context != null) {
                                context.abort();
                            }
                            throw new RuntimeException(e);
                        } finally {
                            CLIScriptContextUtils.closeContext(context);
                        }
                    });
                }

            } while (itemsLeft);
        }
        pureSyncerThreadPool.shutdown();
        try {
            pureSyncerThreadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    void replaceMetadataFromPure(Context context, Item dspaceEntity, String scheme, String element, String qualifier,
                                 String value) throws SQLException {
        List<MetadataValue> existingMetadata = dSpaceServicesContainer.getItemService().getMetadata(dspaceEntity, scheme, element,
                qualifier, Item.ANY);
        if (!existingMetadata.isEmpty() && value != null && value.equals(existingMetadata.get(0).getValue())) {
            return;
        }
        dSpaceServicesContainer.getItemService().clearMetadata(context, dspaceEntity, scheme, element, qualifier, Item.ANY);
        if (value != null) {
            dSpaceServicesContainer.getItemService().addMetadata(context, dspaceEntity, scheme, element, qualifier, null, value);
        }
    }

    String getPureWsEndpointBase() {
        return pureWsEndpointBase;
    }
}
