/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 * Copyright © 2011 Cybernetica AS. 
 * 
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.openam.authentication.modules.oauth2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;

import com.iplanet.sso.SSOException;

import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.encode.Base64;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletResponse;
import org.owasp.esapi.ESAPI;

public class OAuth extends AMLoginModule implements OAuthParam {

    private String authenticatedUser = null;
    private Map sharedState;
    private OAuthConf config;
    String serverName = "";
    private ResourceBundle bundle = null;
    private static final SecureRandom random = new SecureRandom();    
    String data = "";
    String userPassword = "";
    String proxyURL = "";
    
    public OAuth() {
        OAuthUtil.debugMessage("OAuth()");
    }

    public void init(Subject subject, Map sharedState, Map config) {
        this.sharedState = sharedState;
        this.config = new OAuthConf(config);
        bundle = amCache.getResBundle(OAuthConf.BUNDLE_NAME, getLoginLocale());
        String authLevel = CollectionHelper.getMapAttr(config, OAuthConf.KEY_AUTH_LEVEL);
        if (authLevel != null) {
            try {
                setAuthLevel(Integer.parseInt(authLevel));
            } catch (Exception e) {
                OAuthUtil.debugError("Unable to set auth level " + authLevel, e);
            }
        }
    }

    
    public int process(Callback[] callbacks, int state) throws LoginException {

        OAuthUtil.debugMessage("process: state = " + state);
        HttpServletRequest request = getHttpServletRequest();
        HttpServletResponse response = getHttpServletResponse();

        switch (state) {
            case ISAuthConstants.LOGIN_START: {
                serverName = request.getServerName();
                String requestedURL = request.getRequestURL().toString();
                String requestedQuery = request.getQueryString();

                if (requestedQuery != null) {
                    requestedURL += "?" + requestedQuery;
                }

                // Set the return URL Cookie
                // Note: The return URL cookie from the RedirectCallback can not
                // be used because the framework changes the order of the 
                // parameters in the query. OAuth2 requires an identical URL 
                // when retrieving the token
                response.addCookie(OAuthUtil.setCookie(OAuth.COOKIE_ORIG_URL,
                        requestedURL, serverName, "/"));

                // The Proxy is used to return with a POST to the module
                proxyURL = config.getProxyURL();
                setUserSessionProperty(ISAuthConstants.FULL_LOGIN_URL,
                        requestedURL);

                setUserSessionProperty(OAuth.SESSION_LOGOUT_BEHAVIOUR,
                        config.getLogoutBhaviour());

                String authServiceUrl = config.getAuthServiceUrl(config.getProxyURL());
                OAuthUtil.debugMessage("OAuth.process(): New RedirectURL=" + authServiceUrl);

                Callback[] callbacks1 = getCallback(2);
                RedirectCallback rc = (RedirectCallback) callbacks1[0];
                RedirectCallback rcNew = new RedirectCallback(authServiceUrl,
                        null,
                        "GET",
                        rc.getStatusParameter(),
                        rc.getRedirectBackUrlCookieName());
                replaceCallback(2, 0, rcNew);
                return GET_OAUTH_TOKEN_STATE;
            }

            case GET_OAUTH_TOKEN_STATE: {
                // We are being redirected back from an OAuth 2 Identity Provider
                String code = request.getParameter(PARAM_CODE);
                if (code == null || code.isEmpty()) {
                        OAuthUtil.debugMessage("OAuth.process(): LOGIN_IGNORE");
                        return ISAuthConstants.LOGIN_START;
                }
   
                validateInput("code", code, "HTTPParameterValue", 512, false);
                
                try {
                    OAuthUtil.debugMessage("OAuth.process(): code parameter: " + code);

                    String tokenSvcResponse = getContent(
                            config.getTokenServiceUrl(code, proxyURL));
                    OAuthUtil.debugMessage("OAuth.process(): token=" + tokenSvcResponse);

                    String token = extractToken(tokenSvcResponse);

                    String logoutURL = config.getLogoutServiceUrl();
                    if (logoutURL != null && !logoutURL.isEmpty()) {
                        response.addCookie(OAuthUtil.setCookie(OAuth.COOKIE_LOGOUT_URL,
                                logoutURL, serverName, "/"));
                    }

                    setUserSessionProperty(OAuth.SESSION_OAUTH_TOKEN, token);

                    String profileSvcResponse = getContent(
                            config.getProfileServiceUrl(token));

                    OAuthUtil.debugMessage("OAuth.process(): Profile Svc "
                            + "response: " + profileSvcResponse);

                    String realm = getRequestOrg();

                    if (realm == null) {
                        realm = "/";
                    }

                    AccountMapper accountMapper = instantiateAccountMapper();
                    Map userNames = new HashMap();

                    userNames = accountMapper.getAccount(
                            config.getAccountMapperConfig(),
                            profileSvcResponse);

                    String user = getUser(realm, accountMapper, userNames);

                    if (user == null && !config.getCreateAccountFlag()) {
                        authenticatedUser = getDynamicUser(userNames);

                        if (authenticatedUser != null) {
                            if (config.getSaveAttributesToSessionFlag()) {
                                Map attributes = getAttributesMap(
                                        profileSvcResponse);
                                saveAttributes(attributes);
                            }
                            OAuthUtil.debugMessage("OAuth.process(): LOGIN_SUCCEED "
                                    + "with user " + authenticatedUser);
                            return ISAuthConstants.LOGIN_SUCCEED;
                        } else {
                            throw new AuthLoginException("No user mapped!");
                        }

                    }

                    if (user == null && config.getCreateAccountFlag()) {
                        if (config.getPromptPasswordFlag()) {
                            setUserSessionProperty("ATTRIBUTES", profileSvcResponse);
                            return SET_PASSWORD_STATE;
                        } else {
                            authenticatedUser = provisionAccountNow(
                                    realm, profileSvcResponse, getRandomData());
                            if (authenticatedUser != null) {
                                OAuthUtil.debugMessage("User created: " + authenticatedUser);
                                return ISAuthConstants.LOGIN_SUCCEED;
                            } else {
                                return ISAuthConstants.LOGIN_IGNORE;
                            }
                        }
                    }

                    if (user != null) {
                        authenticatedUser = user;
                        OAuthUtil.debugMessage("OAuth.process(): LOGIN_SUCCEED "
                                + "with user " + authenticatedUser);
                        if (config.getSaveAttributesToSessionFlag()) {
                            Map attributes = getAttributesMap(
                                    profileSvcResponse);
                            saveAttributes(attributes);
                        }
                        return ISAuthConstants.LOGIN_SUCCEED;
                    }

                } catch (JSONException je) {
                    OAuthUtil.debugError("OAuth.process(): JSONException: "
                            + je.getMessage());
                    throw new AuthLoginException(OAuthConf.BUNDLE_NAME,
                            "json", null, je);
                } catch (SSOException ssoe) {
                    OAuthUtil.debugError("OAuth.process(): SSOException: "
                            + ssoe.getMessage());
                    throw new AuthLoginException(OAuthConf.BUNDLE_NAME,
                            "ssoe", null, ssoe);
                } catch (IdRepoException ire) {
                    OAuthUtil.debugError("OAuth.process(): IdRepoException: "
                            + ire.getMessage());
                    throw new AuthLoginException(OAuthConf.BUNDLE_NAME,
                            "ire", null, ire);
                }
                break;
            }

            case SET_PASSWORD_STATE: {
                if (!config.getCreateAccountFlag()) {
                    return ISAuthConstants.LOGIN_IGNORE;
                }
                userPassword = request.getParameter(PARAM_TOKEN1);
                validateInput(PARAM_TOKEN1, userPassword, "HTTPParameterValue", 
                        512, false);
                String userPassword2 = request.getParameter(PARAM_TOKEN2);
                validateInput(PARAM_TOKEN2, userPassword2, "HTTPParameterValue", 
                        512, false);
                
                
                if (!userPassword.equals(userPassword2)) {
                    return SET_PASSWORD_STATE;
                }
                
                String terms = request.getParameter("terms");
                if (!terms.equalsIgnoreCase("accept")) {
                    return SET_PASSWORD_STATE;
                }
                String profileSvcResponse = getUserSessionProperty("ATTRIBUTES");
                data = getRandomData();
                String mail = getMail(profileSvcResponse, config.getMailAttribute());
                OAuthUtil.debugMessage("Mail found = " + mail);
                try {
                    OAuthUtil.sendEmail("", mail, data, config.getSMTPConfig(), 
                            bundle, proxyURL);
                } catch (NoEmailSentException ex) {
                    OAuthUtil.debugError("No mail sent due to error", ex);
                    return ISAuthConstants.LOGIN_IGNORE;
                }
                OAuthUtil.debugMessage("User to be created, we need to activate: " + data);
                return CREATE_USER_STATE;
            }

            case CREATE_USER_STATE: {
                String activation = request.getParameter(PARAM_ACTIVATION);
                validateInput(PARAM_ACTIVATION, activation, "HTTPParameterValue", 
                        512, false);
                OAuthUtil.debugMessage("code entered by the user: " + activation);

                if (activation == null || activation.isEmpty()
                        || !activation.trim().equals(data.trim())) {
                    return CREATE_USER_STATE;
                }

                String profileSvcResponse = getUserSessionProperty("ATTRIBUTES");
                String realm = getRequestOrg();
                if (realm == null) {
                    realm = "/";
                }

                OAuthUtil.debugMessage("Got Attributes: " + profileSvcResponse);
                authenticatedUser = provisionAccountNow(realm,profileSvcResponse, userPassword);
                if (authenticatedUser != null) {
                    OAuthUtil.debugMessage("User created: " + authenticatedUser);
                    return ISAuthConstants.LOGIN_SUCCEED;
                } else {
                    return ISAuthConstants.LOGIN_IGNORE;
                }
            }

            default: {
                OAuthUtil.debugError("OAuth.process(): Illegal State");
                return ISAuthConstants.LOGIN_IGNORE;
            }
        }
        
        throw new AuthLoginException(OAuthConf.BUNDLE_NAME,
                            "unknownState", null);
    }

    // Search for the user in the realm, using the instantiated account mapper
    private String getUser(String realm, AccountMapper accountMapper, 
            Map userNames)
            throws AuthLoginException, JSONException, SSOException, IdRepoException {

        String user = null;

        if (userNames != null) {
            AMIdentity userIdentity = accountMapper.searchUser(
                    getAMIdentityRepository(realm), userNames);
            if (userIdentity != null) {
                user = userIdentity.getName();
            }
        }
        
        return user;
    }

    // Generate random data
    private String getRandomData() {
	        byte[] pass = new byte[20];
	        random.nextBytes(pass);
	       return Base64.encode(pass);
     }

    // Create an instance of the pluggable account mapper
    private AccountMapper instantiateAccountMapper () {
                
        try {
            AccountMapper accountMapper =
                    (AccountMapper) Class.forName(config.getAccountMapper()).
                    newInstance();
            return accountMapper;
        } catch (Exception ex) {
            OAuthUtil.debugError("OAuth.getUser: Problem when trying to get the "
                    + "Account Mapper", ex);
            return null;
        }
    }
    
    // Obtain the attributes configured for the module, by using the pluggable
    // Attribute mapper
    private Map getAttributesMap (String svcPRofileResponse) {
        
        Map attributes = new HashMap();

        try {
            AttributeMapper attributeMapper =
                    (AttributeMapper) Class.forName(config.getAttributeMapper()).
                    newInstance();
            attributes = attributeMapper.getAttributes(
                    config.getAttributeMapperConfig(), svcPRofileResponse);
        } catch (Exception ex) {
            OAuthUtil.debugError("OAuth.getUser: Problem when trying to get the "
                    + "Attribute Mapper", ex);
        }
        OAuthUtil.debugMessage("OAuth.getUser: creating new user; attributes = "
                    + attributes);
        return attributes;
    }
    
    
    // Save the attributes configured for the attribute mapper as session attributes
    public void saveAttributes(Map attributes) throws AuthLoginException {

                if (attributes != null && !attributes.isEmpty()) {
                    Iterator attrsIt = attributes.keySet().iterator();
                    while (attrsIt.hasNext()) {
                        String attributeName = (String) attrsIt.next();
                        String attributeValue =
                                ((Set) attributes.get(attributeName)).iterator().next().toString();
                        setUserSessionProperty(attributeName,
                                attributeValue);

                        OAuthUtil.debugMessage("OAuth.saveAttributes: Attribute set: " +
                                attributeName + " Attribute Value: " + attributeValue);
                    }
                } else {
                    OAuthUtil.debugMessage("OAuth.saveAttributes: NO attributes to set");
                }
    }
    
    // Generate a user name, either using the anonymous user if configured or by
    // extracting the first entry of the attribute configured in the account mapper
    // or return null, if nothing was found
    private String getDynamicUser(Map userNames)
            throws AuthLoginException {

        String dynamicUser = null;
        if (config.getUseAnonymousUserFlag()) {
            String anonUser = config.getAnonymousUser();
            if (anonUser != null && !anonUser.isEmpty()) {
                dynamicUser = anonUser;
            }
        } else { // Do not use anonymous
            if (userNames != null && !userNames.isEmpty()) {
                Iterator usersIt = userNames.values().iterator();
                dynamicUser = ((Set) usersIt.next()).iterator().next().toString();
            }

        }
        return dynamicUser;
    }
    

//    private Set addToSet(Set set, String attribute) {
//	set.add(attribute);
//	return set;
//    }

    
    // Obtain the user profile information from the OAuth 2.0 Identity Provider
    // Profile service configured for this module, either using first GET and
    // POST as a fall back
    private String getContent(String serviceUrl) throws LoginException {

        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                getContentStreamByGET(serviceUrl)));

        StringBuffer buf = new StringBuffer();

        try {
            for (String str; (str = in.readLine()) != null;) {
                buf.append(str);
            }
        } catch (IOException ioe) {
            OAuthUtil.debugError("getContent: IOException: " + ioe.getMessage());
            throw new AuthLoginException(OAuthConf.BUNDLE_NAME,
                    "ioe", null, ioe);
        } finally {
            try {
                in.close();
            } catch (IOException ioe) {
                throw new AuthLoginException(OAuthConf.BUNDLE_NAME,
                        "ioe", null, ioe);
            }
        }
        return buf.toString();
    }

    // Create the account in the realm, by using the pluggable account mapper and
    // the attributes configured in the attribute mapper
    public String provisionAccountNow(String realm, String profileSvcResponse, 
            String userPassword)
            throws AuthLoginException {

            AccountMapper accountMapper = instantiateAccountMapper();
            Map attributes = getAttributesMap(profileSvcResponse);
            if (config.getSaveAttributesToSessionFlag()) {
                saveAttributes(attributes);
            }
            attributes.put("userPassword",
                    OAuthUtil.addToSet(new HashSet(), userPassword));
            attributes.put("inetuserstatus", OAuthUtil.addToSet(new HashSet(), "Active"));
            AMIdentity userIdentity =
                    accountMapper.provisionUser(getAMIdentityRepository(realm),
                    attributes);
            if (userIdentity != null) {
                return userIdentity.getName().trim();
            } else {
                return null;      
            }     
    }
    
    
    // Obtain the Profile Service information using GET
    public InputStream getContentStreamByGET(String serviceUrl)
            throws LoginException {

        OAuthUtil.debugMessage("service url: " + serviceUrl);
        try {
            InputStream is = null;
            URL urlC = new URL(serviceUrl);

            String OAuthToken = OAuthUtil.getParamValue(urlC.getQuery(),
                    OAuth.PARAM_ACCESS_TOKEN);
            
            OAuthUtil.debugMessage("OAUTHTOKEN = " + OAuthToken);

            HttpURLConnection connection = (HttpURLConnection) urlC.openConnection();
            connection.setDoOutput(true);

            if (!OAuthToken.isEmpty()) {
                connection.setRequestProperty("Authorization", "OAuth "
                        + OAuthToken);
            }

            connection.setRequestMethod("GET");

            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                OAuthUtil.debugMessage("OAuth.getContentStreamByGET: IT was OK");
                is = connection.getInputStream();
                // OK
            } else if (connection.getResponseCode() == HttpURLConnection.HTTP_BAD_METHOD) {
                // GET method not accepted by the Identity Provider
                OAuthUtil.debugMessage("OAuth.getContentStreamByGET: IT was NOT-OK: " + 
                        connection.getResponseCode());
                is = getContentStreamByPOST(serviceUrl);
            } else {
                // Server returned HTTP error code.
                String data[] = {String.valueOf(connection.getResponseCode())};
                throw new AuthLoginException(OAuthConf.BUNDLE_NAME,
                        "httpErrorCode", data);
            }

            return is;

        } catch (MalformedURLException mfe) {
            throw new AuthLoginException(OAuthConf.BUNDLE_NAME,"malformedURL", null, mfe);
        } catch (IOException ioe) {
            throw new AuthLoginException(OAuthConf.BUNDLE_NAME,"ioe", null, ioe);
        }
    }
    
    
    // Obtain the Profile Service information using GET
    public InputStream getContentStreamByPOST(String serviceUrl)
            throws LoginException {

        InputStream is = null;

        try {
            OAuthUtil.debugMessage("OAuth.getContentStreamByPOST: URL = " + serviceUrl);

            URL url = new URL(serviceUrl);

            String query = url.getQuery();
            OAuthUtil.debugMessage("OAuth.getContentStreamByPOST: Query: " + query);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(query);
            writer.close();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                OAuthUtil.debugMessage("OAuth.getContentStreamByPOST: IT was OK");
                is = connection.getInputStream();
            } else { // Error Code
                OAuthUtil.debugError("OAuth.getContentStreamByPOST: IT was NOT-OK: " + 
                        connection.getResponseCode());
                String data[] = {String.valueOf(connection.getResponseCode())};
                throw new AuthLoginException(OAuthConf.BUNDLE_NAME,
                        "httpErrorCode", data);
            }
        } catch (MalformedURLException e) {
            throw new AuthLoginException(OAuthConf.BUNDLE_NAME,"malformedURL", null, e);
        } catch (IOException e) {
            throw new AuthLoginException(OAuthConf.BUNDLE_NAME,"ioe", null, e);
        }

        return is;

    }

    // Extract the Token from the OAuth 2.0 response
    // Todo: Maybe this should be pluggable
    public String extractToken(String response) {

        String token = "";
        try {
            JSONObject jsonToken = new JSONObject(response);
            if (jsonToken != null
                    && !jsonToken.isNull(OAuth.PARAM_ACCESS_TOKEN)) {
                token = jsonToken.getString(OAuth.PARAM_ACCESS_TOKEN);
                OAuthUtil.debugMessage("access_token: " + token);
            }
        } catch (JSONException je) {
            OAuthUtil.debugMessage("OAuth.extractToken: Not in JSON format" + je);
            token = OAuthUtil.getParamValue(response, PARAM_ACCESS_TOKEN);
        }

        return token;
    }

    // Obtain the email address field from the response provided by the
    // OAuth 2.0 Profile service.
    public String getMail(String svcResponse, String mailAttribute) {
        String mail = "";
        OAuthUtil.debugMessage("mailAttribute: " + mailAttribute);
        try {
            JSONObject jsonData = new JSONObject(svcResponse);

            if (mailAttribute != null && mailAttribute.indexOf(".") != -1) {
                StringTokenizer parts = new StringTokenizer(mailAttribute, ".");
                mail = jsonData.getJSONObject(parts.nextToken()).getString(parts.nextToken());
            } else {
                mail = jsonData.getString(mailAttribute);
            }
            OAuthUtil.debugMessage("mail: " + mail);

        } catch (JSONException je) {
            OAuthUtil.debugMessage("OAuth.getMAil: Not in JSON format" + je);
        }

        return mail;
    }
    
    // Validate the field provided as input
    public void validateInput(String tag, String inputField,
            String rule, int maxLength, boolean allowNull)
            throws AuthLoginException {
        if (!ESAPI.validator().isValidInput(tag, inputField, rule, maxLength, allowNull)) {
            OAuthUtil.debugError("OAuth.process(): OAuth 2.0 Not valid input !");
            String msgdata[] = {tag, inputField};
            throw new AuthLoginException(OAuthConf.BUNDLE_NAME,
                    "invalidField", msgdata);
        };
    }
    
    
    public Principal getPrincipal() {
        if (authenticatedUser != null) {
            return new OAuthPrincipal(authenticatedUser);
        }
        return null;
    }

    public void destroyModuleState() {
        authenticatedUser = null;
    }

    public void nullifyUsedVars() {
        config = null;
        sharedState = null;
    }
    
}
