package dk.digitalidentity.os2faktor.security;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import dk.digitalidentity.saml.extension.SamlIdentityProviderProvider;
import dk.digitalidentity.saml.model.IdentityProvider;

@Component
public class IdentityProviderProvider implements SamlIdentityProviderProvider {
	private List<IdentityProvider> providers = new ArrayList<>();

	@Value("${login.enable.unilogin:false}")
	private boolean uniLoginEnabled;
	
	@Value("${login.enable.idp:false}")
	private boolean idpEnabled;

	@Value("${saml.metadata.idp.url:}")
	private String idpMetadataUrl;
	
	@Value("${saml.metadata.idp.entityid:}")
	private String idpMetadataentityId;
	
	@Value("${saml.metadata.unilogin.url:}")
	private String uniLoginMetadataUrl;
	
	@Value("${saml.metadata.unilogin.entityid:}")
	private String uniLoginMetadataEntityId;

	@PostConstruct
	public void init() {
		if (idpEnabled && !StringUtils.isEmpty(idpMetadataUrl)) {
			providers.add(IdentityProvider.builder().metadata(idpMetadataUrl).entityId(idpMetadataentityId).build());
		}
		
		if (uniLoginEnabled && !StringUtils.isEmpty(uniLoginMetadataUrl)) {
			providers.add(IdentityProvider.builder().metadata(uniLoginMetadataUrl).entityId(uniLoginMetadataEntityId).build());
		}
	}

	@Override
	public List<IdentityProvider> getIdentityProviders() {
		return providers;
	}

	@Override
	public IdentityProvider getByEntityId(String entityId) {
		for (IdentityProvider provider : providers) {
			if (provider.getEntityId().equals(entityId)) {
				return provider;
			}
		}

		return null;
	}
}
