/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package de.leuphana.escience.dspacepurebridge.pure.imports;

import static de.leuphana.escience.dspacepurebridge.pure.Constants.LAST_MODIFICATION_DATE;

import java.sql.SQLException;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import de.leuphana.escience.dspacepurebridge.CLIScriptContextUtils;
import de.leuphana.escience.dspacepurebridge.pure.Constants;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResultItem;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResultOrganizationItem;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResultPersonItem;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResultProjectItem;
import de.leuphana.escience.dspacepurebridge.pure.apiobjects.PureWSResults;
import de.leuphana.escience.dspacepurebridge.relations.EntityUtils;
import de.leuphana.escience.dspacepurebridge.relations.RelationShipUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class PureToDSpace {
    private static final Logger log = LoggerFactory.getLogger(PureToDSpace.class);
    static final Map<DSpacePureEntity, Map<UUID, UUID>> pureEntityCache = new ConcurrentHashMap<>();
    static final Map<DSpacePureEntity, Map<String, UUID>> dspaceEntityCache = new ConcurrentHashMap<>();
    private final Map<DSpacePureEntity, UUID> collectionMap = new EnumMap<>(DSpacePureEntity.class);
    private final String pureWsEndpointBase;
    private final String pureWsApiKey;
    private final HandleService handleService;
    private final ItemService itemService;
    private final CollectionService collectionService;
    private final WorkspaceItemService workspaceItemService;
    private final InstallItemService installItemService;
    private final RelationshipTypeService relationshipTypeService;
    private final RelationshipService relationshipService;
    private final ConfigurationService configurationService;

    private static final String PURE_WS_PAGESIZE = "dspace-pure-bridge.pure.ws.pagesize";

    public PureToDSpace(String pureWsEndpointBase, String pureureWsApiKey, HandleService handleService,
                        ItemService itemService, CollectionService collectionService,
                        WorkspaceItemService workspaceItemService,
                        InstallItemService installItemService, RelationshipTypeService relationshipTypeService,
                        RelationshipService relationshipService, ConfigurationService configurationService) {
        this.pureWsEndpointBase = pureWsEndpointBase;
        this.pureWsApiKey = pureureWsApiKey;
        this.handleService = handleService;
        this.itemService = itemService;
        this.collectionService = collectionService;
        this.workspaceItemService = workspaceItemService;
        this.installItemService = installItemService;
        this.relationshipTypeService = relationshipTypeService;
        this.relationshipService = relationshipService;
        this.configurationService = configurationService;
    }

    RestTemplateBuilder initRestTemplateBuilder() {
        return new RestTemplateBuilder()
            .requestFactory(HttpComponentsClientHttpRequestFactory.class)
            .connectTimeout(Duration.ofMillis(60000))
            .readTimeout(Duration.ofMillis(60000))
            .defaultHeader("api-key", pureWsApiKey);
    }

    void prepareCaches() throws SQLException {
        Context context = CLIScriptContextUtils.createReducedContext();
        for (DSpacePureEntity dSpacePureEntity : DSpacePureEntity.values()) {
            pureEntityCache.putIfAbsent(dSpacePureEntity, new ConcurrentHashMap<>());
            dspaceEntityCache.putIfAbsent(dSpacePureEntity, new ConcurrentHashMap<>());
            Collection entityCollection = (Collection) handleService
                .resolveToObject(context,
                    dSpacePureEntity.getDspacePureEntityCollectionHandle(configurationService));
            collectionMap.put(dSpacePureEntity, entityCollection.getID());
            Iterator<Item> entities = itemService.findByCollection(context, entityCollection);
            while (entities.hasNext()) {
                Item item = entities.next();
                List<MetadataValue> metadata =
                    itemService.getMetadata(item, Constants.SCHEME, Constants.ELEMENT, Constants.UUID_QUALIFIER,
                        Item.ANY,
                        false);
                if (!metadata.isEmpty()) {
                    pureEntityCache.get(dSpacePureEntity)
                        .putIfAbsent(UUID.fromString(metadata.get(0).getValue()), item.getID());
                }
            }

            Collection dspaceEntityCollection = (Collection) handleService
                .resolveToObject(context,
                    dSpacePureEntity.getDspaceEntityCollectionHandle(configurationService));
            Iterator<Item> dspaceEntities = itemService.findByCollection(context, dspaceEntityCollection);
            while (dspaceEntities.hasNext()) {
                Item item = dspaceEntities.next();
                String entityHash = null;

                if (DSpacePureEntity.ORGANIZATION.equals(dSpacePureEntity)) {
                    entityHash = EntityUtils.generateOrgUnitNameEntityHash(itemService, item);
                } else if (DSpacePureEntity.PERSON.equals(dSpacePureEntity)) {
                    entityHash = EntityUtils.generatePersonEntityHash(itemService, item);
                } else if (DSpacePureEntity.PROJECT.equals(dSpacePureEntity)) {
                    entityHash = EntityUtils.generateProjectNameEntityHash(itemService, item);
                }
                if (entityHash != null) {
                    dspaceEntityCache.get(dSpacePureEntity).putIfAbsent(entityHash, item.getID());
                }
            }
        }
        CLIScriptContextUtils.closeContext(context);
    }

    Context createContext() throws SQLException {
        return CLIScriptContextUtils.createReducedContext();
    }

    public void syncObjects() throws SQLException {
        prepareCaches();

        int pureWsPageSize = 100;
        if (configurationService.hasProperty(PURE_WS_PAGESIZE)) {
            pureWsPageSize = configurationService.getIntProperty(PURE_WS_PAGESIZE);
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
                                resultItem = itemService.find(context,
                                    pureEntityCache.get(dSpacePureEntity)
                                        .get(pureWSResultItem.getUuid()));
                                List<MetadataValue> lastModified =
                                    itemService.getMetadata(resultItem, Constants.SCHEME, Constants.ELEMENT,
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
                                                    collectionService.find(finalContext, collectionMap
                                                        .get
                                                            (dSpacePureEntity));

                                                WorkspaceItem workspaceItem =
                                                    workspaceItemService.create(finalContext, collection, false);
                                                Item createdItem = workspaceItem.getItem();
                                                itemService.addMetadata(finalContext, createdItem, "dspace", "entity",
                                                    "type",
                                                    null,
                                                    dSpacePureEntity.getDspacePureEntity());
                                                itemService.addMetadata(finalContext, createdItem, Constants.SCHEME,
                                                    Constants.ELEMENT,
                                                    Constants.UUID_QUALIFIER, null,
                                                    String.valueOf(pureWSResultItem.getUuid()));

                                                installItemService.installItem(finalContext, workspaceItem);
                                                pureItemChanged.set(true);
                                                return createdItem.getID();
                                            } catch (Exception e) {
                                                return null;
                                            }
                                        });
                                resultItem = itemService.find(finalContext, itemId);
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
                                    relationshipTypeService,
                                    dSpacePureEntity.getDspaceEntityRelationshipType());
                                List<Relationship> byItemAndRelationshipType =
                                    relationshipService.findByItemAndRelationshipType(
                                        context, resultItem, relationshipType);

                                for (Relationship relationship : byItemAndRelationshipType) {
                                    if (relatedUUID.equals(relationship.getLeftItem().getID())) {
                                        relate = false;
                                        break;
                                    }
                                }
                                if (relate) {
                                    Item relatedItem = itemService.find(context, relatedUUID);
                                    log.info("Creating relationship {} between {} and {}",
                                        dSpacePureEntity.getDspaceEntityRelationshipType(),
                                        relatedItem.getHandle(),
                                        resultItem.getHandle());
                                    Relationship persistedRelationship = relationshipService.create(context,
                                        relatedItem,
                                        resultItem,
                                        relationshipType, 0,
                                        -1);
                                    relationshipService.update(context, persistedRelationship);
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
        List<MetadataValue> existingMetadata = itemService.getMetadata(dspaceEntity, scheme, element,
            qualifier, Item.ANY);
        if (!existingMetadata.isEmpty() && value != null && value.equals(existingMetadata.get(0).getValue())) {
            return;
        }
        itemService.clearMetadata(context, dspaceEntity, scheme, element, qualifier, Item.ANY);
        if (value != null) {
            itemService.addMetadata(context, dspaceEntity, scheme, element, qualifier, null, value);
        }
    }

    String getPureWsEndpointBase() {
        return pureWsEndpointBase;
    }
}
