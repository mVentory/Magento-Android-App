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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.Socket;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.mageventory.util.Log;

public class XMLRPCServer extends XMLRPCCommon {

    private static final String RESPONSE = "HTTP/1.1 200 OK\n" + "Connection: close\n"
            + "Content-Type: text/xml\n"
            + "Content-Length: ";
    private static final String NEWLINES = "\n\n";
    private XMLRPCSerializer iXMLRPCSerializer;

    public XMLRPCServer() {
        iXMLRPCSerializer = new XMLRPCSerializer();
    }

    public MethodCall readMethodCall(Socket socket) throws IOException, XmlPullParserException {
        MethodCall methodCall = new MethodCall();
        InputStream inputStream = socket.getInputStream();

        XmlPullParser pullParser = xmlPullParserFromSocket(inputStream);

        pullParser.nextTag();
        pullParser.require(XmlPullParser.START_TAG, null, Tag.METHOD_CALL);
        pullParser.nextTag();
        pullParser.require(XmlPullParser.START_TAG, null, Tag.METHOD_NAME);

        methodCall.setMethodName(pullParser.nextText());

        pullParser.nextTag();
        pullParser.require(XmlPullParser.START_TAG, null, Tag.PARAMS);
        pullParser.nextTag(); // <param>

        do {
            // Log.d(Tag.LOG, "type=" + pullParser.getEventType() + ", tag=" +
            // pullParser.getName());
            pullParser.require(XmlPullParser.START_TAG, null, Tag.PARAM);
            pullParser.nextTag(); // <value>

            Object param = iXMLRPCSerializer.deserialize(pullParser);
            methodCall.params.add(param); // add to return value

            pullParser.nextTag();
            pullParser.require(XmlPullParser.END_TAG, null, Tag.PARAM);
            pullParser.nextTag(); // <param> or </params>

        } while (!pullParser.getName().equals(Tag.PARAMS)); // </params>

        return methodCall;
    }

    XmlPullParser xmlPullParserFromSocket(InputStream socketInputStream) throws IOException,
            XmlPullParserException {
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(socketInputStream));
        while ((line = br.readLine()) != null && line.length() > 0)
            ; // eat the HTTP POST headers

        XmlPullParser pullParser = XmlPullParserFactory.newInstance().newPullParser();
        pullParser.setInput(br);
        return pullParser;
    }

    public void respond(Socket socket, Object[] params) throws IOException {

        String content = methodResponse(params);
        String response = RESPONSE + (content.length()) + NEWLINES + content;
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(response.getBytes());
        outputStream.flush();
        outputStream.close();
        socket.close();
        Log.d(Tag.LOG, "response:" + response);
    }

    private String methodResponse(Object[] params) throws IllegalArgumentException,
            IllegalStateException, IOException {
        StringWriter bodyWriter = new StringWriter();
        serializer.setOutput(bodyWriter);
        serializer.startDocument(null, null);
        serializer.startTag(null, Tag.METHOD_RESPONSE);

        serializeParams(params);

        serializer.endTag(null, Tag.METHOD_RESPONSE);
        serializer.endDocument();

        return bodyWriter.toString();
    }
}
