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

import java.util.ArrayList;

public class MethodCall {

    private static final int TOPIC = 1;
    String methodName;
    ArrayList<Object> params = new ArrayList<Object>();

    public String getMethodName() {
        return methodName;
    }

    void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public ArrayList<Object> getParams() {
        return params;
    }

    void setParams(ArrayList<Object> params) {
        this.params = params;
    }

    public String getTopic() {
        return (String) params.get(TOPIC);
    }
}
