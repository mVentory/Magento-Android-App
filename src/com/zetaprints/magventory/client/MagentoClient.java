package com.zetaprints.magventory.client;

import java.util.ArrayList;

import com.zetaprints.magventory.xmlrpc.XMLRPCClient;
import com.zetaprints.magventory.xmlrpc.XMLRPCException;

public class MagentoClient {

	String sessionId;
	XMLRPCClient client;

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

	public Object execute(String method, Object[] params) {
		Object result = null;
		ArrayList<Object> paramex = new ArrayList<Object>();
		paramex.add(sessionId);
		paramex.add(method);
		paramex.add(params);
		try {
			Object[] o = new Object[] { sessionId, method };

			result = client.callEx("call", (Object[])paramex.toArray());
		} catch (XMLRPCException e) {
			e.printStackTrace();

		}
		return result;

	}
	public Object execute(boolean t,int b) {
		Object result = null;


		try {
			result = client.callEx("call",new Object[]{sessionId,"catalog_product.info",new Object[]{"3"}});
		} catch (XMLRPCException e) {
			e.printStackTrace();

		}
		return result;

	}
}
