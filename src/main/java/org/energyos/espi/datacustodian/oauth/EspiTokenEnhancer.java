package org.energyos.espi.datacustodian.oauth;

import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.NoResultException;

import org.energyos.espi.common.domain.Authorization;
import org.energyos.espi.common.domain.DateTimeInterval;
import org.energyos.espi.common.domain.RetailCustomer;
import org.energyos.espi.common.domain.Routes;
import org.energyos.espi.common.domain.Subscription;
import org.energyos.espi.common.domain.UsagePoint;
import org.energyos.espi.common.domain.User;
import org.energyos.espi.common.service.ApplicationInformationService;
import org.energyos.espi.common.service.AuthorizationService;
import org.energyos.espi.common.service.ResourceService;
import org.energyos.espi.common.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.transaction.annotation.Transactional;

public class EspiTokenEnhancer implements TokenEnhancer {

	@Autowired
	private ApplicationInformationService applicationInformationService;

	@Autowired
	private SubscriptionService subscriptionService;

	@Autowired
	private ResourceService resourceService;

	@Autowired
	private AuthorizationService authorizationService;

	@Autowired
	private JdbcClientDetailsService clientDetailsService;

	private String baseURL; // "baseURL" is a "tokenEnhancer" bean property
							// defined in the oauth-AS-config.xml file

	@Transactional(rollbackFor = { javax.xml.bind.JAXBException.class }, noRollbackFor = {
			javax.persistence.NoResultException.class, org.springframework.dao.EmptyResultDataAccessException.class })
	@Override
	public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {

		DefaultOAuth2AccessToken result = new DefaultOAuth2AccessToken(accessToken);

		System.err.printf("EspiTokenEnhancer: OAuth2Request Parameters = %s\n", authentication.getOAuth2Request()
				.getRequestParameters());

		Map<String, String> requestParameters = authentication.getOAuth2Request().getRequestParameters();
		String grantType = requestParameters.get(OAuth2Utils.GRANT_TYPE);
		grantType = grantType.toLowerCase();

		OAuth2Request oAuth2Request = authentication.getOAuth2Request();
		Long usagePointId = 0l;
		if (oAuth2Request.getExtensions() != null && oAuth2Request.getExtensions().containsKey("usagepoint")) {
			usagePointId = (Long) oAuth2Request.getExtensions().get("usagepoint");

		}
		// base resource URI of datacustodian resource end-point
		baseURL = applicationInformationService.getDataCustodianResourceEndpoint().substring(0,
				applicationInformationService.getDataCustodianResourceEndpoint().lastIndexOf("/espi/"));

		System.err.println(" grantType " + grantType);

		// Is this a "client_credentials" access token grant_type request?
		if (grantType.contentEquals("client_credentials")) {

			System.err.println("oAuth2Request.getExtensions() " + oAuth2Request.getExtensions());
			System.err.println("oAuth2Request.getDetails() " + authentication.getDetails());
			System.err.println("accessToken.getAdditionalInformation() " + accessToken.getAdditionalInformation());
			System.err.println("accessToken.loadClientByClientId() "
					+ clientDetailsService.loadClientByClientId(authentication.getOAuth2Request().getClientId())
							.getAdditionalInformation().get("client_id"));
			System.err.println("accessToken.getCredentials() " + authentication.getCredentials());
			System.err.println("accessToken.getPrincipal() " + authentication.getPrincipal());

			// Processing a "client_credentials" access token grant_type
			// request.

			// Create Subscription and add resourceURI to /oath/token response

			// DJ commented, does this require subscription. all subscription
			// usage is commented
			// Subscription subscription =
			// subscriptionService.createSubscription(authentication,
			// usagePointId);
			// result.getAdditionalInformation().put("resourceURI",
			// baseURL + Routes.BATCH_SUBSCRIPTION.replace("{subscriptionId}",
			// subscription.getId().toString()));

			// Create Authorization and add authorizationURI to /oath/token
			// response
			Authorization authorization = authorizationService.createAuthorization(null, result.getValue());
			result.getAdditionalInformation().put(
					"authorizationURI",
					baseURL
							+ Routes.DATA_CUSTODIAN_AUTHORIZATION.replace("{AuthorizationID}", authorization.getId()
									.toString()));

			// Update Data Custodian subscription structure
			// subscription.setAuthorization(authorization);
			// subscription.setUpdated(new GregorianCalendar());
			// subscriptionService.merge(subscription);

			// Update Data Custodian authorization structure
			ClientDetails clintDtl = clientDetailsService.loadClientByClientId(authentication.getOAuth2Request()
					.getClientId());
			authorization.setApplicationInformation(applicationInformationService.findByClientId((String) clintDtl
					.getAdditionalInformation().get("client_id")));
			/*
			 * String clientIdTmp =
			 * authentication.getOAuth2Request().getClientId(); if
			 * (clientIdTmp.equals("data_custodian_admin")) {
			 * List<ApplicationInformation> list=applicationInformationService
			 * .findByKind("DATA_CUSTODIAN_ADMIN");
			 * authorization.setApplicationInformation(list.get(list.size()-1));
			 * } else if (clientIdTmp.contains("REGISTRATION_")) { clientIdTmp =
			 * clientIdTmp.substring("REGISTRATION_".length());
			 * authorization.setApplicationInformation
			 * (applicationInformationService.findByClientId(clientIdTmp)); }
			 * else {
			 * authorization.setApplicationInformation(applicationInformationService
			 * .findByClientId(clientIdTmp)); }
			 */

			authorization.setThirdParty(authentication.getOAuth2Request().getClientId());
			authorization.setAccessToken(accessToken.getValue());
			authorization.setTokenType(accessToken.getTokenType());
			authorization.setExpiresIn((long) accessToken.getExpiresIn());
			// authorization.setAuthorizedPeriod(new DateTimeInterval((long) 0,
			// (long) 0));
			// authorization.setPublishedPeriod(new DateTimeInterval((long) 0,
			// (long) 0));

			authorization
					.setAuthorizedPeriod(new DateTimeInterval(24 * 3600 * 1000 * 365l, System.currentTimeMillis()));
			authorization.setPublishedPeriod(new DateTimeInterval(24 * 3600 * 1000 * 365l, System.currentTimeMillis()));

			if (accessToken.getRefreshToken() != null) {
				authorization.setRefreshToken(accessToken.getRefreshToken().toString());
			}

			// Remove "[" and "]" surrounding Scope in accessToken structure
			authorization.setScope(accessToken.getScope().toString()
					.substring(1, (accessToken.getScope().toString().length() - 1)));

			// set the authorizationUri
			authorization.setAuthorizationURI(baseURL
					+ Routes.DATA_CUSTODIAN_AUTHORIZATION
							.replace("{AuthorizationID}", authorization.getId().toString()));

			// Determine resourceURI value based on Client's Role
			Set<String> role = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

			if (role.contains("ROLE_DC_ADMIN")) {
				authorization.setResourceURI(baseURL + Routes.DATA_CUSTODIAN_RESOURCE_MANAGEMENT);

			} else {
				if (role.contains("ROLE_TP_ADMIN")) {
					authorization.setResourceURI(baseURL + Routes.BATCH_BULK_MEMBER.replace("{bulkId}", "**"));

				} else {
					if (role.contains("ROLE_UL_ADMIN")) {
						authorization.setResourceURI(baseURL
								+ Routes.BATCH_UPLOAD_MY_DATA.replace("{RetailCustomerID}", "**"));
					}
				}
			}

			authorization.setUpdated(new GregorianCalendar());
			authorization.setStatus("1"); // Set authorization record status as
											// "Active"
			authorizationService.merge(authorization);

		} else if (grantType.contentEquals("authorization_code")) {

			try {
				// Is this a refresh_token grant_type request?
				Authorization authorization = authorizationService.findByRefreshToken(result.getRefreshToken()
						.getValue());

				// Yes, update access token
				authorization.setAccessToken(accessToken.getValue());
				authorizationService.merge(authorization);

			} catch (NoResultException | EmptyResultDataAccessException e) {
				// No, process as initial access token request

				// Create Subscription and add resourceURI to /oath/token
				// response
				Subscription subscription = subscriptionService.createSubscription(authentication, usagePointId);
				result.getAdditionalInformation().put(
						"resourceURI",
						baseURL
								+ Routes.BATCH_SUBSCRIPTION
										.replace("{subscriptionId}", subscription.getId().toString()));

				// Create Authorization and add authorizationURI to /oath/token
				// response
				Authorization authorization = authorizationService.createAuthorization(subscription, result.getValue());
				result.getAdditionalInformation().put(
						"authorizationURI",
						baseURL
								+ Routes.DATA_CUSTODIAN_AUTHORIZATION.replace("{AuthorizationID}", authorization
										.getId().toString()));

				// Update Data Custodian subscription structure
				subscription.setAuthorization(authorization);
				subscription.setUpdated(new GregorianCalendar());
				subscriptionService.merge(subscription);

				// DC RetailCustomer retailCustomer = (RetailCustomer)
				// authentication.getPrincipal();
				RetailCustomer retailCustomer = ((User) authentication.getPrincipal()).getRetailCustomer();

				// link in the usage points associated with this subscription
				System.out.println("Linking with usagepoint----@ESPITokenEnhancer" + usagePointId);
				if (usagePointId > 0) {

					UsagePoint up = resourceService.findById(usagePointId, UsagePoint.class);
					up.setSubscription(subscription);
					resourceService.persist(up); // maybe not needed??
				} else {
					List<Long> usagePointIds = resourceService.findAllIdsByXPath(retailCustomer.getId(),
							UsagePoint.class);
					Iterator<Long> it = usagePointIds.iterator();
					while (it.hasNext()) {
						UsagePoint up = resourceService.findById(it.next(), UsagePoint.class);
						//DJ up.setSubscription(subscription);
						//DJ resourceService.persist(up); // maybe not needed??
					}
				}

				// Update Data Custodian authorization structure
				authorization.setApplicationInformation(applicationInformationService.findByClientId(authentication
						.getOAuth2Request().getClientId()));
				authorization.setThirdParty(authentication.getOAuth2Request().getClientId());
				authorization.setRetailCustomer(retailCustomer);
				authorization.setAccessToken(accessToken.getValue());
				authorization.setTokenType(accessToken.getTokenType());
				authorization.setExpiresIn((long) accessToken.getExpiresIn());

				if (accessToken.getRefreshToken() != null) {
					authorization.setRefreshToken(accessToken.getRefreshToken().toString());
				}

				// Remove "[" and "]" surrounding Scope in accessToken structure
				authorization.setScope(accessToken.getScope().toString()
						.substring(1, (accessToken.getScope().toString().length() - 1)));
				authorization.setAuthorizationURI(baseURL
						+ Routes.DATA_CUSTODIAN_AUTHORIZATION.replace("{AuthorizationID}", authorization.getId()
								.toString()));
				authorization.setResourceURI(baseURL
						+ Routes.BATCH_SUBSCRIPTION.replace("{subscriptionId}", subscription.getId().toString()));
				authorization.setUpdated(new GregorianCalendar());
				authorization.setStatus("1"); // Set authorization record status
												// as "Active"
				authorization.setSubscription(subscription);
				// authorization.setAuthorizedPeriod(new DateTimeInterval((long)
				// 0, (long) 0));
				// authorization.setPublishedPeriod(new DateTimeInterval((long)
				// 0, (long) 0));
				authorization.setAuthorizedPeriod(new DateTimeInterval(24 * 3600 * 1000 * 365l, System
						.currentTimeMillis()));
				authorization.setPublishedPeriod(new DateTimeInterval(24 * 3600 * 1000 * 365l, System
						.currentTimeMillis()));

				authorizationService.merge(authorization);
			}

		} else {

			System.out.printf("EspiTokenEnhancer: Invalid Grant_Type processed by Spring Security OAuth2 Framework:\n"
					+ "OAuth2Request Parameters = %s\n", authentication.getOAuth2Request().getRequestParameters());
			throw new AccessDeniedException(String.format("Unsupported ESPI OAuth2 grant_type"));
		}

		return result;
	}

	public void setBaseURL(String baseURL) {
		this.baseURL = baseURL;
	}

	public void setApplicationInformationService(ApplicationInformationService applicationInformationService) {
		this.applicationInformationService = applicationInformationService;
	}

	public ApplicationInformationService getApplicationInformationService() {
		return this.applicationInformationService;
	}

	public void setSubscriptionService(SubscriptionService subscriptionService) {
		this.subscriptionService = subscriptionService;
	}

	public SubscriptionService getSubscriptionService() {
		return this.subscriptionService;
	}

	public void setResourceService(ResourceService resourceService) {
		this.resourceService = resourceService;
	}

	public ResourceService getResourceService() {
		return this.resourceService;
	}

	public void setAuthorizationService(AuthorizationService authorizationService) {
		this.authorizationService = authorizationService;
	}

	public AuthorizationService getAuthorizationService() {
		return this.authorizationService;
	}

	public JdbcClientDetailsService getClientDetailsService() {
		return clientDetailsService;
	}

	public void setClientDetailsService(JdbcClientDetailsService clientDetailsService) {
		this.clientDetailsService = clientDetailsService;
	}

}
