/* Copyright (c) 2014 mVentory Ltd. (http://mventory.com)
 * 
* License       http://creativecommons.org/licenses/by-nc-nd/4.0/
* 
* NonCommercial — You may not use the material for commercial purposes. 
* NoDerivatives — If you compile, transform, or build upon the material,
* you may not distribute the modified material. 
* Attribution — You must give appropriate credit, provide a link to the license,
* and indicate if changes were made. You may do so in any reasonable manner, 
* but not in any way that suggests the licensor endorses you or your use. 
*/

package com.mageventory.xmlrpc;

import org.apache.http.HttpStatus;

public class XMLRPCException extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = 7499675036625522379L;

    private int httpStatusCode = HttpStatus.SC_OK;

    public XMLRPCException(Exception e) {
        super(e);
    }

    public XMLRPCException(String string) {
        super(string);
    }

    public XMLRPCException(String string, int httpStatusCode) {
        this(string);
        this.httpStatusCode = httpStatusCode;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }
}
