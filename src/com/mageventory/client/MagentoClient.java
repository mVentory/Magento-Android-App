package com.mageventory.client;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import com.mageventory.util.Log;

import com.mageventory.settings.Settings;
import com.mageventory.xmlrpc.XMLRPCClient;
import com.mageventory.xmlrpc.XMLRPCException;

@Deprecated
public class MagentoClient {

	private String sessionId;
	private XMLRPCClient client;
	private boolean valid;
	private String apiPath = "/index.php/api/xmlrpc/";
	private String user;
	private String pass;
	private String url;
	private Context ctx;

	/**
	 * Contains the last error message. Can be obtained by calling
	 * {@link MagentoClient#getLastError()}.
	 */
	private String lastError;

	public MagentoClient(Context act) {
		valid = false;
		ctx = act;
		String session = "";
		Settings settings = new Settings(act);
		this.url = settings.getUrl() + apiPath;
		this.user = settings.getUser();
		this.pass = settings.getPass();
		try {
			client = new XMLRPCClient(settings.getUrl() + apiPath);
			session = (String) client.call("login", settings.getUser(), settings.getPass());
			valid = true;
			this.url = settings.getUrl() + apiPath;
			this.user = settings.getUser();
			this.pass = settings.getPass();
		} catch (XMLRPCException e) {
			Log.logCaughtException(e);
			return;
		}
		sessionId = session;
	}

	public String getLastError() {
		return lastError;
	}

	public void relog(String url, String user, String pass) {
		String session = "";
		this.url = url + apiPath;
		this.user = user;
		this.pass = pass;
		valid = false;
		try {
			client = new XMLRPCClient(url + apiPath);
			session = (String) client.call("login", user, pass);
			valid = true;
		} catch (XMLRPCException e) {
			lastError = e.getMessage();
			return;
		}
		sessionId = session;
	}

	public MagentoClient(String url, String user, String pass) {
		valid = false;
		client = new XMLRPCClient(url + apiPath);
		Log.d("config", "login " + user + " " + pass + " " + url + apiPath);

		this.url = url + apiPath;
		this.user = user;
		this.pass = pass;
	}

	public Object execute(String method) {
		Object result = null;
		try {
			result = client.callEx("call", new Object[] { sessionId, method });
		} catch (XMLRPCException e) {
			Log.logCaughtException(e);
			try {
				/* check if session expired re login */
				Settings settings = new Settings(ctx);
				this.url = settings.getUrl() + apiPath;
				this.user = settings.getUser();
				this.pass = settings.getPass();
				relog(this.url, this.user, this.pass);
				result = client.callEx("call", new Object[] { sessionId, method });
			} catch (Exception e2) {
				Log.logCaughtException(e2);
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
			Log.logCaughtException(e);
			try {
				/* check if session expired re login */

				Settings settings = new Settings(ctx);
				this.url = settings.getUrl() + apiPath;
				this.user = settings.getUser();
				this.pass = settings.getPass();
				relog(this.url, this.user, this.pass);
				String session = (String) client.call("login", this.user, this.pass);
				this.sessionId = session;
				result = client.callEx("call", (Object[]) paramex.toArray());
			} catch (Exception e2) {
				Log.logCaughtException(e2);
			}

		}
		return result;

	}

	public Object execute(String method, HashMap params) {
		Object result = null;

		Object[] o = new Object[] { sessionId, method, new Object[] { params } };
		try {

			result = client.callEx("call", o);
		} catch (XMLRPCException e) {
			Log.logCaughtException(e);
		}
		return result;

	}

	public boolean isValid() {
		return valid;
	}
	
	public void setValid(boolean v)
	{
		valid = v;
	}
}
