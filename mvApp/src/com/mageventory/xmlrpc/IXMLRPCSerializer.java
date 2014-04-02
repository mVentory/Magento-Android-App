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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public interface IXMLRPCSerializer {
    String TAG_NAME = "name";
    String TAG_MEMBER = "member";
    String TAG_VALUE = "value";
    String TAG_DATA = "data";

    String TYPE_INT = "int";
    String TYPE_I4 = "i4";
    String TYPE_I8 = "i8";
    String TYPE_DOUBLE = "double";
    String TYPE_BOOLEAN = "boolean";
    String TYPE_STRING = "string";
    String TYPE_DATE_TIME_ISO8601 = "dateTime.iso8601";
    String TYPE_BASE64 = "base64";
    String TYPE_ARRAY = "array";
    String TYPE_STRUCT = "struct";
    // This added by mattias.ellback as part of issue #19
    String TYPE_NULL = "nil";

    String DATETIME_FORMAT = "yyyyMMdd'T'HH:mm:ss";

    void serialize(XmlSerializer serializer, Object object) throws IOException;

    Object deserialize(XmlPullParser parser) throws XmlPullParserException, IOException;
}
