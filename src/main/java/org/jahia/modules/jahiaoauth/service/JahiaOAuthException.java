package org.jahia.modules.jahiaoauth.service;

/**
 * Exception throw by Jahia OAuth
 *
 * @author dgaillard
 */
public class JahiaOAuthException extends Exception {
    private static final long serialVersionUID = -7784897643800742205L;

    public JahiaOAuthException() {
    }

    public JahiaOAuthException(String message) {
        super(message);
    }

    public JahiaOAuthException(String message, Throwable cause) {
        super(message, cause);
    }

    public JahiaOAuthException(Throwable cause) {
        super(cause);
    }
}
