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

/**
 * Allows to pass any XMLRPCSerializable object as input parameter. When
 * implementing getSerializable() you should return one of XMLRPC primitive
 * types (or another XMLRPCSerializable: be careful not going into recursion by
 * passing this object reference!)
 */
public interface XMLRPCSerializable {

    /**
     * Gets XMLRPC serialization object
     * 
     * @return object to serialize This object is most likely one of XMLRPC
     *         primitive types, however you can return also another
     *         XMLRPCSerializable
     */
    Object getSerializable();
}
