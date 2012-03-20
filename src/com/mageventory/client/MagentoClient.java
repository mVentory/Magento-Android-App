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
    private Context ctx;
	public MagentoClient(Context act) {
		valid = false;
		ctx=act;
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
			e.printStackTrace();
			return;
		}
		sessionId = session;
	}
	public void relog(String url, String user, String pass){
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
				Settings settings = new Settings(ctx);
				this.url = settings.getUrl() + apiPath;
				this.user = settings.getUser();
				this.pass = settings.getPass();
				relog(this.url,this.user,  this.pass);
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
				
				Settings settings = new Settings(ctx);
				this.url = settings.getUrl() + apiPath;
				this.user = settings.getUser();
				this.pass = settings.getPass();
				relog(this.url,this.user,  this.pass);
				String session = (String) client.call("login", this.user, this.pass);
				this.sessionId=session;
				result = client.callEx("call", (Object[]) paramex.toArray());
			} catch (Exception e2) {
				e2.printStackTrace();
			}

		}
		return result;

	}
	public Object execute(String method, HashMap params) {
		Object result = null;


		Object[] o = new Object[] { sessionId, method,new Object[]{params} };
		try {

			result = client.callEx("call",o);
		} catch (XMLRPCException e) {
			e.printStackTrace();
		}
		return result;

	}

	public boolean isValid() {
		return valid;
	}
}
