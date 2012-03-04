package com.mageventory.client;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.util.Log;

import com.mageventory.settings.Settings;
import com.mageventory.xmlrpc.XMLRPCClient;
import com.mageventory.xmlrpc.XMLRPCException;

public class MagentoClient {

	private String sessionId;
	private XMLRPCClient client;
	private boolean valid;
	private String apiPath = "/index.php/api/xmlrpc/";
	private String user;
	private String pass;
	private String url;

	public MagentoClient(Context act) {

		valid = false;
		String session = "";
		try {
			Settings settings = new Settings(act);

			client = new XMLRPCClient(settings.getUrl() + apiPath);
			session = (String) client.call("login", settings.getUser(), settings.getPass());
			valid = true;
			this.url = settings.getUrl() + apiPath;
			this.user = settings.getUser();
			this.pass = settings.getPass();
		} catch (XMLRPCException e) {
			e.printStackTrace();
			return;
		}
		sessionId = session;
	}

	public MagentoClient(String url, String user, String pass) {

		String session = "";
		valid = false;
		try {
			client = new XMLRPCClient(url + apiPath);
			Log.d("config", "login " + user + " " + pass + " " + url + apiPath);
			session = (String) client.call("login", user, pass);

			valid = true;
			this.url = url + apiPath;
			this.user = user;
			this.pass = pass;
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
			try {
				/*check if session expired re login*/
				String sesion = (String) client.call("login", this.user, this.pass);
				result = client.callEx("call", new Object[] { sessionId, method });
			} catch (Exception e2) {
				e2.printStackTrace();
			}

		}
		return result;
	}

	public Object execute(String method, Object[] params) {
		Object result = null;
		ArrayList<Object> paramex = new ArrayList<Object>();
		paramex.add(sessionId);
		paramex.add(method);
		paramex.add(params);
		Object[] o = new Object[] { sessionId, method };
		try {

			result = client.callEx("call", (Object[]) paramex.toArray());
		} catch (XMLRPCException e) {
			e.printStackTrace();
			try {
				/*check if session expired re login*/
				String sesion = (String) client.call("login", this.user, this.pass);
				result = client.callEx("call", (Object[]) paramex.toArray());
			} catch (Exception e2) {
				e2.printStackTrace();
			}

		}
		return result;

	}

	public boolean isValid() {
		return valid;
	}
}
