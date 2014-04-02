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

import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import android.util.Xml;

class XMLRPCCommon {

    protected XmlSerializer serializer;
    protected IXMLRPCSerializer iXMLRPCSerializer;

    XMLRPCCommon() {
        serializer = Xml.newSerializer();
        iXMLRPCSerializer = new XMLRPCSerializer();
    }

    /**
     * Sets custom IXMLRPCSerializer serializer (in case when server doesn't
     * support standard XMLRPC protocol)
     * 
     * @param serializer custom serializer
     */
    public void setSerializer(IXMLRPCSerializer serializer) {
        iXMLRPCSerializer = serializer;
    }

    protected void serializeParams(Object[] params) throws IllegalArgumentException,
            IllegalStateException, IOException {
        if (params != null && params.length != 0) {
            // set method params
            serializer.startTag(null, Tag.PARAMS);
            for (int i = 0; i < params.length; i++) {
                serializer.startTag(null, Tag.PARAM).startTag(null, IXMLRPCSerializer.TAG_VALUE);
                iXMLRPCSerializer.serialize(serializer, params[i]);
                serializer.endTag(null, IXMLRPCSerializer.TAG_VALUE).endTag(null, Tag.PARAM);
            }
            serializer.endTag(null, Tag.PARAMS);
        }
    }

}
