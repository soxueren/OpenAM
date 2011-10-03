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

import com.sun.identity.authentication.spi.AuthLoginException;
import javax.servlet.http.HttpServletRequest;

import com.sun.identity.shared.debug.Debug;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javax.servlet.http.Cookie;

public class OAuthUtil implements OAuthParam {

    private static Debug debug = Debug.getInstance("amAuth");

    public static String findCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        Cookie returnCookie = null;
        String value = "";
        if (cookies != null) {
            for (int k = 0; k < cookies.length; k++) {
                if (cookieName.equalsIgnoreCase(cookies[k].getName())) {
                    returnCookie = cookies[k];
                    value = returnCookie.getValue();
                    debugMessage("OAuth.findCookie()" + "Cookie "
                                + cookieName
                                + " found. "
                                + "Content is: " + value);
                    break;
                }
            }
        }
        return value;
    }
    
    
    public static Cookie deleteCookie(String strCookieName, String serverName, String strPath) {
        Cookie cookie = new Cookie(strCookieName, "");
        cookie.setMaxAge(0);
        cookie.setDomain(serverName);
        cookie.setPath(strPath);
        return cookie;
    }    
    
    public static String encodeUriToRedirect(String originalUrl) throws 
            AuthLoginException {
        try {
            String query = "";
            URI url_URL;
            url_URL = new URI(originalUrl);
            query = url_URL.getQuery();
            if (query != null && query.length() != 0) {
                StringBuilder encodedQueryValues = new StringBuilder();
                String[] paramsArray = query.split("\\&");
                int countParams = paramsArray.length;
                for (int i = 0; i < countParams; i++) {
                    String[] parts = paramsArray[i].split("=");
                    encodedQueryValues.append(parts[0]).append("=").
                            append(URLEncoder.encode(parts[1],"UTF-8"));
                    if (i < countParams - 1) {
                        encodedQueryValues.append("&");
                    }
                    debugMessage("encodedQueryValues=" + encodedQueryValues);
                }

                originalUrl = url_URL.getScheme() + "://" + url_URL.getHost()
                        + url_URL.getPath() + "?" + encodedQueryValues;
                debugMessage("RED URL: " + originalUrl);
                originalUrl = URLEncoder.encode(originalUrl,"UTF-8");
            }
        } catch (URISyntaxException ex) {
            debugError("URI manipulation", ex);
        } catch (UnsupportedEncodingException ex) {
            debugError("Problem encoding the originalUrl" + originalUrl, ex);
            throw new AuthLoginException("Problem encoding the originalUrl");
        }   
        return originalUrl;
    }
    
    public static String getParamValue(String query, String param) {

        String paramValue = "";
        if (query != null && query.length() != 0) {
            String[] paramsArray = query.split("\\&");
            for (String parameter : paramsArray) {
                if (parameter.startsWith(param)) {
                    paramValue = parameter.substring(parameter.indexOf("=") + 1);
                    break;
                }
            }
        }
        return paramValue;
    }
    
    public static Cookie setCookie(String cookieName, String cookieValue, 
            String serverName, String path) {
                Cookie returnURLCookie = new Cookie(cookieName, cookieValue);
                returnURLCookie.setDomain(serverName);
                returnURLCookie.setPath(path);
                return returnURLCookie;
    }

    static boolean isEmpty(String value) {
        return value == null || "".equals(value);
    }
    
    public static Set addToSet(Set set, String attribute) {
	set.add(attribute);
	return set;
    }
    
    public static void sendEmail(String uid, String emailAddress, String activCode,
              Map SMTPConfig, ResourceBundle bundle, String linkURL)
    throws NoEmailSentException {
        try {
            String gatewayEmailImplClass = (String) SMTPConfig.get(KEY_EMAIL_GWY_IMPL);
            if (uid != null || emailAddress != null) {
                String from = bundle.getString(MESSAGE_FROM);
                String subject = bundle.getString(MESSAGE_SUBJECT);
                String message = bundle.getString(MESSAGE_BODY);
                message = message.replace("#ACTIVATION_CODE#", activCode);
                
                String link = "";
                try {
                     link = linkURL + "?" + PARAM_ACTIVATION + "=" +
                     URLEncoder.encode(activCode, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                   debugError("Error while encoding", ex);
                }

                message = message.replace("#ACTIVATION_LINK#", link.toString());
                EmailGateway gateway = (EmailGateway)
                            Class.forName(gatewayEmailImplClass).newInstance();
                gateway.sendEmail(from, emailAddress, subject, message, SMTPConfig);
                debugMessage("OAuthUtil.sendEmail() : sent email to " +
                            emailAddress);
            } else {
                  debugMessage("OAuthUtil.sendEmail() : unable to send email");

            }
        } catch (ClassNotFoundException cnfe) {
            debugError("CommunityRegisterAuth.sendEmail() : " + "class not found " +
                        "EmailGateway class", cnfe);
        } catch (InstantiationException ie) {
            debugError("CommunityRegisterAuth.sendEmail() : " + "can not instantiate " +
                        "EmailGateway class", ie);
        } catch (IllegalAccessException iae) {
            debugError("CommunityRegisterAuth.sendEmail() : " + "can not access " +
                        "EmailGateway class", iae);
        }
    }
    
    public static void debugMessage(String message) {
        if (debug.messageEnabled()) {
            debug.message(message);
        }
    }

    public static void debugWarning(String message) {
        if (debug.warningEnabled()) {
            debug.warning(message);
        }
    }
    
    public static void debugError(String message, Throwable t) {
        if (debug.errorEnabled()) {
            debug.error(message, null);
        }
    }
    
    public static void debugError(String message) {
        if (debug.errorEnabled()) {
            debug.error(message);
        }
    }
}
