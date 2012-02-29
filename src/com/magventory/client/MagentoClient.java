package com.magventory.client;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

import com.magventory.settings.Settings;
import com.magventory.xmlrpc.XMLRPCClient;
import com.magventory.xmlrpc.XMLRPCException;

public class MagentoClient {

	String sessionId;
	XMLRPCClient client;

	public MagentoClient(Context act) {
		Settings settings = new Settings(act);
		Log.d("APP", settings.getUrl() + "/index.php/api/xmlrpc/");
		Log.d("APP", settings.getUser());
		Log.d("APP", settings.getPass());
		client = new XMLRPCClient(settings.getUrl() + "/index.php/api/xmlrpc/");

		String session = "";
		try {
			session = (String) client.call("login", settings.getUser(), settings.getPass());
		} catch (XMLRPCException e) {
			e.printStackTrace();
			return;
		}
		sessionId = session;
	}

	public MagentoClient(String url, String user, String pass) {

		client = new XMLRPCClient(url);
		String session = "";
		try {
			session = (String) client.call("login", user, pass);
		} catch (XMLRPCException e) {
			e.printStackTrace();
			return;
		}
		sessionId = session;
	}

	public Object execute(String method) {
		Object result = null;
		try {
			result = client.callEx("call", new Object[] { sessionId, method });
		} catch (XMLRPCException e) {
			e.printStackTrace();

		}
		return result;
	}

	public boolean test() {
		boolean ret = false;

		try {
			if (sessionId.length() > 1) {
				ret = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	public Object execute(String method, Object[] params) {
		Object result = null;
		ArrayList<Object> paramex = new ArrayList<Object>();
		paramex.add(sessionId);
		paramex.add(method);
		paramex.add(params);
		try {
			Object[] o = new Object[] { sessionId, method };

			result = client.callEx("call", (Object[]) paramex.toArray());
		} catch (XMLRPCException e) {
			e.printStackTrace();

		}
		return result;

	}

	public Object execute(boolean t, int b) {
		Object result = null;

		try {
			result = client.callEx("call", new Object[] { sessionId, "catalog_product.info", new Object[] { "3" } });
		} catch (XMLRPCException e) {
			e.printStackTrace();

		}
		return result;

	}
}
