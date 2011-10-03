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

interface OAuthParam {
    
    static final String MODULE_NAME = "OAuth";
    static final String BUNDLE_NAME = "amAuthOAuth";

    static final String KEY_CLIENT_ID = "iplanet-am-auth-oauth-client-id";
    static final String KEY_CLIENT_SECRET = "iplanet-am-auth-oauth-client-secret";
    static final String KEY_AUTH_SERVICE = "iplanet-am-auth-oauth-auth-service";
    static final String KEY_TOKEN_SERVICE = "iplanet-am-auth-oauth-token-service";
    static final String KEY_PROFILE_SERVICE = "iplanet-am-auth-oauth-user-profile-service";
    static final String KEY_SCOPE = "iplanet-am-auth-oauth-scope";
    static final String KEY_SSO_PROXY_URL = "iplanet-am-auth-oauth-sso-proxy-url";
    static final String KEY_AUTH_LEVEL = "iplanet-am-auth-oauth-auth-level";
    static final String KEY_ACCOUNT_MAPPER = "org-forgerock-auth-oauth-account-mapper";
    static final String KEY_ACCOUNT_MAPPER_CONFIG =
            "org-forgerock-auth-oauth-account-mapper-configuration";
    static final String KEY_ATTRIBUTE_MAPPER = "org-forgerock-auth-oauth-attribute-mapper";
    static final String KEY_ATTRIBUTE_MAPPER_CONFIG =
            "org-forgerock-auth-oauth-attribute-mapper-configuration";
    static final String KEY_SAVE_ATTRIBUTES_TO_SESSION = 
            "org-forgerock-auth-oauth-save-attributes-to-session-flag";
    static final String KEY_MAIL_ATTRIBUTE = "org-forgerock-auth-oauth-mail-attribute";
    static final String KEY_CREATE_ACCOUNT = "org-forgerock-auth-oauth-createaccount-flag";
    static final String KEY_PROMPT_PASSWORD = "org-forgerock-auth-oauth-prompt-password-flag";
    static final String KEY_LOGOUT_SERVICE_URL = "org-forgerock-auth-oauth-logout-service-url";
    static final String KEY_LOGOUT_BEHAVIOUR = "org-forgerock-auth-oauth-logout-behaviour";
    static final String KEY_MAP_TO_ANONYMOUS_USER_FLAG = "org-forgerock-auth-oauth-map-to-anonymous-flag";
    static final String KEY_ANONYMOUS_USER = "org-forgerock-auth-oauth-anonymous-user";
    static final String KEY_EMAIL_GWY_IMPL = "org-forgerock-auth-oauth-email-gwy-impl";
    static final String KEY_SMTP_HOSTNAME = "org-forgerock-auth-oauth-smtp-hostname";
    static final String KEY_SMTP_PORT = "org-forgerock-auth-oauth-smtp-port";
    static final String KEY_SMTP_USERNAME = "org-forgerock-auth-oauth-smtp-username";
    static final String KEY_SMTP_PASSWORD = "org-forgerock-auth-oauth-smtp-password";
    static final String KEY_SMTP_SSL_ENABLED = "org-forgerock-auth-oauth-smtp-ssl_enabled";
    
    // openam parameters
    final static String PARAM_GOTO = "goto";
    final static String PARAM_REALM = "realm";
    final static String PARAM_MODULE = "module";

    // OAuth 2.0 parameters
    final static String PARAM_CODE = "code";
    final static String PARAM_REDIRECT_URI = "redirect_uri";
    final static String PARAM_SCOPE = "scope";
    final static String PARAM_CLIENT_SECRET = "client_secret";
    final static String PARAM_CLIENT_ID = "client_id";
    final static String PARAM_ACCESS_TOKEN =  "access_token";
    
    // oauthproxy parameters
    final static String PARAM_ACTIVATION = "activation";
    final static String PARAM_TOKEN1 = "token1";
    final static String PARAM_TOKEN2 = "token2";


    // Session parameters set by the module
    final static String SESSION_OAUTH_TOKEN = "OAuthToken";
    final static String SESSION_OAUTH2 = "OAUTH2";
    final static String SESSION_LOGOUT_BEHAVIOUR = "OAuth2logoutBehaviour";
    
    // Cookies used by the module
    final static String COOKIE_ORIG_URL = "ORIG_URL";
    final static String COOKIE_PROXY_URL = "PROXY_URL";
    final static String COOKIE_LOGOUT_URL = "OAUTH_LOGOUT_URL";
    
    // Login states
    final static int LOGIN_START = 1;
    final static int GET_OAUTH_TOKEN_STATE = 2;
    final static int SET_PASSWORD_STATE = 3;
    final static int CREATE_USER_STATE = 4;
    
    final static String MESSAGE_FROM = "messageFrom";
    final static String MESSAGE_SUBJECT = "messageSubject";
    final static String MESSAGE_BODY = "messageBody";
    
}

