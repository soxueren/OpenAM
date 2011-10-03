/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.authentication.modules.oauth2;

/**
 *
 * @author steve.ferris@forgerock.com
 */
public class NoEmailSentException extends Exception {
    private Exception nestedException;

    public NoEmailSentException(Exception nestedException) {
        this(nestedException,
             nestedException == null ? "" : nestedException.getMessage());
    }

    public NoEmailSentException(
              Exception nestedException,
              String exceptionMessage) {
        this(exceptionMessage);
        this.nestedException = nestedException;
    }

    public NoEmailSentException(String message) {
        super(message);
    }

    public NoEmailSentException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoEmailSentException(Throwable cause) {
        super(cause);
    }
}
