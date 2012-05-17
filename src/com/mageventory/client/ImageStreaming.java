package com.mageventory.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Base64;
import com.mageventory.util.Log;

import com.mageventory.xmlrpc.XMLRPCClient;
import com.mageventory.xmlrpc.XMLRPCException;
import com.mageventory.xmlrpc.XMLRPCFault;



public class ImageStreaming {
	
	static final String METHOD_RESPONSE = "methodResponse";
	static final String PARAMS = "params";
	static final String PARAM = "param";
	static final String FAULT = "fault";
	static final String FAULT_CODE = "faultCode";
	static final String FAULT_STRING = "faultString";
	
	static final String TAG_NAME = "name";
	static final String TAG_MEMBER = "member";
	static final String TAG_VALUE = "value";
	static final String TAG_DATA = "data";	
	static final String TYPE_INT = "int";
	static final String TYPE_I4 = "i4";
	static final String TYPE_I8 = "i8";
	static final String TYPE_DOUBLE = "double";
	static final String TYPE_BOOLEAN = "boolean";
	static final String TYPE_STRING = "string";
	static final String TYPE_DATE_TIME_ISO8601 = "dateTime.iso8601";
	static final String TYPE_BASE64 = "base64";
	static final String TYPE_ARRAY = "array";
	static final String TYPE_STRUCT = "struct";
	static final String TYPE_NULL = "nil";
	static final String DATETIME_FORMAT = "yyyyMMdd'T'HH:mm:ss";
	
	
	
	/**
	 * Create String contains XML Request for Uploading Image "Part1 --> till base64"
	 * @param method
	 * @param sessionID
	 * @param apiName
	 * @param data
	 * @return
	 */	
	private static String xmlRequestBuilderPartOne(String method, String sessionID, String apiName, Object [] data)
	{
		
		
		String result = "";
		@SuppressWarnings("unchecked")
		Map<String,Object> imgInfo = (Map<String,Object>) data[1];
		
		result = "<?xml version='1.0' ?><methodCall><methodName>" + method + "</methodName><params><param><value><string>" + sessionID +"</string></value></param>";
		result += "<param><value><string>" + apiName + "</string></value></param><param><value><array><data><value><string>" + data[0].toString()+ "</string></value>";
		result += "<value><struct>";				
				
		// Check if "types" key exists then set it
		if(imgInfo.containsKey("types"))
		{
			result += "<member><name>types</name><value><array><data>";
			
			Object [] values = (Object []) imgInfo.get("types");
			for(int i=0;i<values.length;i++)
			{
				result += "<value><string>" + values[i].toString() + "</string></value>";												
			}
			
			result += "</data></array></value></member>";								
		}
		
		// Check if "position" key exists then set it
		if(imgInfo.containsKey("position"))
		{
			result += "<member><name>position</name><value><i4>" + imgInfo.get("position").toString() + "</i4></value></member>";
		}
		
		// Check if "exclude" key exists then set it
		if(imgInfo.containsKey("exclude"))
		{
			result += "<member><name>exclude</name><value><i4>" + imgInfo.get("exclude").toString() + "</i4></value></member>";
		}
				
		// Check if "file" key exists then set it
		if(imgInfo.containsKey("file"))
		{
			@SuppressWarnings("unchecked")
			Map<String,Object> fileInfo = (Map<String,Object>) imgInfo.get("file");
			
			result += "<member><name>file</name><value><struct>";
			
			// Check if "name" Key exists then set it
			if(fileInfo.containsKey("name"))
			{
				result += "<member><name>name</name><value><string>" + fileInfo.get("name").toString() +"</string></value></member>";
			}
						
			// Check if "mime" Key exists then set it
			if(fileInfo.containsKey("mime"))
			{
				result += "<member><name>mime</name><value><string>" + fileInfo.get("mime").toString() + "</string></value></member>";
			}
			
			// Check if "content" Key exists then set it
			if(fileInfo.containsKey("content"))
			{
				result += "<member><name>content</name><value><base64>";
			}
		}
				
		return result;
	}
	
	/**
	 * Create String contains XML Request for Uploading Image "Part2 --> start after base64"
	 * @return
	 */
	private static String xmlRequestBuilderPartTwo()
	{
		String result = "";
		
		result += "</base64></value></member></struct></value></member></struct></value></data></array></value></param></params></methodCall>";
		
		return result;
	}
	
	public static interface StreamUploadCallback
	{
		void onUploadProgress(int progress, int max);
	}
	
	/**
	 * Stream Function 
	 * to Upload Files 
	 */
	public static Object streamUpload(URL url,String method, String sessionID, String apiName,
			Object [] data, StreamUploadCallback callback, XMLRPCClient client) throws XMLRPCException
	{
		Object result = null;
		int requestLength = 0;

		try {
			//	Get 1st and Second Parts of XML
			String xmlRequestPart1 = xmlRequestBuilderPartOne(method, sessionID, apiName, data);
			String xmlRequestPart2 = xmlRequestBuilderPartTwo();

			// Get the Content Length 
			@SuppressWarnings("unchecked")
			Map<String,Object> imgInfo = (Map<String,Object>) data[1];			
			@SuppressWarnings("unchecked")
			Map<String,Object> fileInfo = (Map<String,Object>) imgInfo.get("file");
			
			File imgFile = new File(fileInfo.get("content").toString());
			RandomAccessFile f = new RandomAccessFile(imgFile, "r");

			int imgFileSize = ((int)f.length());
			int chunk_size = 30000;
			int counter = imgFileSize / chunk_size ;
			int chunk_count = ((imgFileSize-1) / chunk_size) + 1; 
			int readBytes = 0;
			byte[] content = new byte[chunk_size];
			int base46_length = 0;
			
			if (callback != null)
				callback.onUploadProgress(0, chunk_count);
			
			for(int i=0;i<counter;i++)
			{
				f.seek(readBytes);				
				// Calculate Base64 Encoded Length							
				f.read(content);
				// 	Calculate the request Length
				base46_length += (new String(Base64Coder_magento.encode((byte[])(Base64.encode(content, Base64.DEFAULT))))).length();
				readBytes += chunk_size;
			}
			
			if(readBytes<imgFileSize)
			{				
				byte[] remainingBytes = new byte[(imgFileSize-readBytes)];
				f.seek(readBytes);						
				f.read(remainingBytes);
				base46_length += (new String(Base64Coder_magento.encode((byte[])(Base64.encode(remainingBytes, Base64.DEFAULT))))).length();				
			}
	
			requestLength = xmlRequestPart1.length() + xmlRequestPart2.length() + base46_length;
			
			
			
			// Get the Connection to Server
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			
			// Set Connection Properties and Parameters
			connection.setDoOutput(true);
			connection.setDoInput(true);
			/*connection.setChunkedStreamingMode(0);*/			
			
			// Set Request Parameters
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-length", String.valueOf(requestLength));		
			connection.setRequestProperty("Content-Type", "text/xml");
			connection.setRequestProperty("Connection", "Keep-Alive");
				
			OutputStream out = connection.getOutputStream();
			OutputStreamWriter uploadStream = new OutputStreamWriter(out, "UTF-8");
	
			uploadStream.write(xmlRequestPart1);
			uploadStream.flush();
			
/*			String encodedDataStr = new String(Base64Coder_magento.encode((byte[])(Base64.encode(content, Base64.DEFAULT))));
			uploadStream.write(encodedDataStr);
			uploadStream.flush();
*/
			readBytes = 0;
			// Load Chunks - encode and send
			for(int i=0;i<counter;i++)
			{
				// Calculate Base64 Encoded Length							
				f.seek(readBytes);				
				f.read(content);			
				uploadStream.write(new String(Base64Coder_magento.encode((byte[])(Base64.encode(content, Base64.DEFAULT)))));
				uploadStream.flush();
				readBytes += chunk_size;
				
				if (callback != null)
					callback.onUploadProgress(i, chunk_count);
			}
			
			if(readBytes<imgFileSize)
			{				
				byte[] remainingBytes = new byte[(imgFileSize-readBytes)];
				f.seek(readBytes);
				f.read(remainingBytes);
				uploadStream.write(new String(Base64Coder_magento.encode((byte[])(Base64.encode(remainingBytes, Base64.DEFAULT)))));
				uploadStream.flush();
				readBytes += chunk_size;
				
				if (callback != null)
					callback.onUploadProgress(chunk_count, chunk_count);
			}
			uploadStream.write(xmlRequestPart2);
			uploadStream.flush();
			
			uploadStream.close();
			out.close();
			
			// Delete Image after Streaming
			f.close();
			imgFile.delete();
			
			// Get the response
			InputStream inputStream = connection.getInputStream();
			
			return client.readServerResponse(inputStream);
		} catch (IOException e) {
			Log.logCaughtException(e);
		} catch (XmlPullParserException e) {
			Log.logCaughtException(e);
		}
				
		return result;
	}

	
	private static Object deserialize(XmlPullParser parser) throws XmlPullParserException, IOException {
		
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATETIME_FORMAT);
		parser.require(XmlPullParser.START_TAG, null, TAG_VALUE);

		if (parser.isEmptyElementTag()) {
			// degenerated <value />, return empty string
			return "";
		}
		
		Object obj;
		boolean hasType = true;
		String typeNodeName = null;
		try {
			parser.nextTag();
			typeNodeName = parser.getName();
			if (typeNodeName.equals(TAG_VALUE) && parser.getEventType() == XmlPullParser.END_TAG) {
				// empty <value></value>, return empty string
				return "";
			}

			
		} catch (XmlPullParserException e) {
			hasType = false;
		}
		if (hasType) {
			// This code submitted by mattias.ellback in issue #19
			if (typeNodeName.equals(TYPE_NULL)){
				parser.nextTag();
				obj = null;
			}
			else
			if (typeNodeName.equals(TYPE_INT) || typeNodeName.equals(TYPE_I4)) {
				String value = parser.nextText();
				obj = Integer.parseInt(value);
			} else
			if (typeNodeName.equals(TYPE_I8)) {
				String value = parser.nextText();
				obj = Long.parseLong(value);
			} else
			if (typeNodeName.equals(TYPE_DOUBLE)) {
				String value = parser.nextText();
				obj = Double.parseDouble(value);
			} else
			if (typeNodeName.equals(TYPE_BOOLEAN)) {
				String value = parser.nextText();
				obj = value.equals("1") ? Boolean.TRUE : Boolean.FALSE;
			} else
			if (typeNodeName.equals(TYPE_STRING)) {
				obj = parser.nextText();
			} else
			if (typeNodeName.equals(TYPE_DATE_TIME_ISO8601)) {
				String value = parser.nextText();
				try {
					obj = dateFormat.parseObject(value);
				} catch (ParseException e) {
					throw new IOException("Cannot deserialize dateTime " + value); 
				}
			} else
			if (typeNodeName.equals(TYPE_BASE64)) {
				String value = parser.nextText();
				BufferedReader reader = new BufferedReader(new StringReader(value));
				String line;
				StringBuffer sb = new StringBuffer();
				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}
				obj = Base64Coder_magento.decode(sb.toString());
			} else
			if (typeNodeName.equals(TYPE_ARRAY)) {
				parser.nextTag(); // TAG_DATA (<data>)
				parser.require(XmlPullParser.START_TAG, null, TAG_DATA);
	
				parser.nextTag();
				List<Object> list = new ArrayList<Object>();
				while (parser.getName().equals(TAG_VALUE)) {
					list.add(deserialize(parser));
					parser.nextTag();
				}
				parser.require(XmlPullParser.END_TAG, null, TAG_DATA);
				parser.nextTag(); // TAG_ARRAY (</array>)
				parser.require(XmlPullParser.END_TAG, null, TYPE_ARRAY);
				obj = list.toArray();
			} else
			if (typeNodeName.equals(TYPE_STRUCT)) {
				parser.nextTag();
				Map<String, Object> map = new HashMap<String, Object>();
				while (parser.getName().equals(TAG_MEMBER)) {
					String memberName = null;
					Object memberValue = null;
					while (true) {
						parser.nextTag();
						String name = parser.getName();
						if (name.equals(TAG_NAME)) {
							memberName = parser.nextText();
						} else
						if (name.equals(TAG_VALUE)) {
							memberValue = deserialize(parser);
						} else {
							break;
						}
					}
					if (memberName != null && memberValue != null) {
						map.put(memberName, memberValue);
					}
					parser.require(XmlPullParser.END_TAG, null, TAG_MEMBER);
					parser.nextTag();
				}
				parser.require(XmlPullParser.END_TAG, null, TYPE_STRUCT);
				obj = map;
			} else {
				throw new IOException("Cannot deserialize " + parser.getName());
			}
		} else {
			// TYPE_STRING (<string>) is not required
			obj = parser.getText();
		}
		parser.nextTag(); // TAG_VALUE (</value>)
		parser.require(XmlPullParser.END_TAG, null, TAG_VALUE);
		return obj;
	}

}
