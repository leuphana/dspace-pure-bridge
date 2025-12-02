package de.leuphana.escience.dspacepurebridge;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.service.*;
import org.dspace.discovery.SearchService;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;

public class DSpaceServicesContainer {
    private final ConfigurationService configurationService;
    private final BitstreamService bitstreamService;
    private final ResourcePolicyService resourcePolicyService;
    private final AuthorizeService authorizeService;
    private final ItemService itemService;
    private final HandleService handleService;
    private final SearchService searchService;
    private final CollectionService collectionService;
    private final WorkspaceItemService workspaceItemService;
    private final InstallItemService installItemService;
    private final RelationshipTypeService relationshipTypeService;
    private final RelationshipService relationshipService;

    public DSpaceServicesContainer(Builder builder) {
        this.configurationService = builder.configurationService;
        this.resourcePolicyService = builder.resourcePolicyService;
        this.bitstreamService = builder.bitstreamService;
        this.authorizeService = builder.authorizeService;
        this.itemService = builder.itemService;
        this.handleService = builder.handleService;
        this.searchService = builder.searchService;
        this.collectionService = builder.collectionService;
        this.workspaceItemService = builder.workspaceItemService;
        this.installItemService = builder.installItemService;
        this.relationshipTypeService = builder.relationshipTypeService;
        this.relationshipService = builder.relationshipService;
    }

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public ResourcePolicyService getResourcePolicyService() {
        return resourcePolicyService;
    }

    public BitstreamService getBitstreamService() {
        return bitstreamService;
    }

    public AuthorizeService getAuthorizeService() {
        return authorizeService;
    }

    public ItemService getItemService() {
        return itemService;
    }

    public HandleService getHandleService() {
        return handleService;
    }

    public SearchService getSearchService() {
        return searchService;
    }

    public CollectionService getCollectionService() {
        return collectionService;
    }

    public WorkspaceItemService getWorkspaceItemService() {
        return workspaceItemService;
    }

    public InstallItemService getInstallItemService() {
        return installItemService;
    }

    public RelationshipTypeService getRelationshipTypeService() {
        return relationshipTypeService;
    }

    public RelationshipService getRelationshipService() {
        return relationshipService;
    }


    public static class Builder {
        public SearchService searchService;
        private ConfigurationService configurationService;
        private ResourcePolicyService resourcePolicyService;
        private BitstreamService bitstreamService;
        private AuthorizeService authorizeService;
        private ItemService itemService;
        private HandleService handleService;
        private CollectionService collectionService;
        private WorkspaceItemService workspaceItemService;
        private InstallItemService installItemService;
        private RelationshipTypeService relationshipTypeService;
        private RelationshipService relationshipService;

        public Builder configurationService(ConfigurationService configurationService) {
            this.configurationService = configurationService;
            return this;
        }

        public Builder resourcePolicyService(ResourcePolicyService resourcePolicyService) {
            this.resourcePolicyService = resourcePolicyService;
            return this;
        }

        public Builder bitstreamService(BitstreamService bitstreamService) {
            this.bitstreamService = bitstreamService;
            return this;
        }

        public Builder authorizeService(AuthorizeService authorizeService) {
            this.authorizeService = authorizeService;
            return this;
        }

        public Builder itemService(ItemService itemService) {
            this.itemService = itemService;
            return this;
        }

        public Builder handleService(HandleService handleService) {
            this.handleService = handleService;
            return this;
        }

        public Builder searchService(SearchService searchService) {
            this.searchService = searchService;
            return this;
        }

        public Builder collectionService(CollectionService collectionService) {
            this.collectionService = collectionService;
            return this;
        }

        public Builder workspaceItemService(WorkspaceItemService workspaceItemService) {
            this.workspaceItemService = workspaceItemService;
            return this;
        }

        public Builder installItemService(InstallItemService installItemService) {
            this.installItemService = installItemService;
            return this;
        }

        public Builder relationshipTypeService(RelationshipTypeService relationshipTypeService) {
            this.relationshipTypeService = relationshipTypeService;
            return this;
        }

        public Builder relationshipService(RelationshipService relationshipService) {
            this.relationshipService = relationshipService;
            return this;
        }
    }
}
