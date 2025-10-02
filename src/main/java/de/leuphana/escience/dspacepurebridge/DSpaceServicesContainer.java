/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package de.leuphana.escience.dspacepurebridge.pure;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;

public class DSpaceServicesContainer {
    private final ConfigurationService configurationService;
    private final BitstreamService bitstreamService;
    private final ResourcePolicyService resourcePolicyService;
    private final AuthorizeService authorizeService;
    private final ItemService itemService;
    private final HandleService handleService;

    public DSpaceServicesContainer(Builder builder) {
        this.configurationService = builder.configurationService;
        this.resourcePolicyService = builder.resourcePolicyService;
        this.bitstreamService = builder.bitstreamService;
        this.authorizeService = builder.authorizeService;
        this.itemService = builder.itemService;
        this.handleService = builder.handleService;
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

    public static class Builder {
        private ConfigurationService configurationService;
        private ResourcePolicyService resourcePolicyService;
        private BitstreamService bitstreamService;
        private AuthorizeService authorizeService;
        private ItemService itemService;
        private HandleService handleService;

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
    }
}
