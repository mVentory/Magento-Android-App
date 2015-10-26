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

public class XMLRPCFault extends XMLRPCException {
    private static final long serialVersionUID = 5676562456612956519L;

    private String faultString;
    private int faultCode;

    public XMLRPCFault(String faultString, int faultCode) {
        super("XMLRPC Fault: " + faultString + " [code " + faultCode + "]");
        this.faultString = faultString;
        this.faultCode = faultCode;
    }

    public String getFaultString() {
        return faultString;
    }

    public int getFaultCode() {
        return faultCode;
    }
}
