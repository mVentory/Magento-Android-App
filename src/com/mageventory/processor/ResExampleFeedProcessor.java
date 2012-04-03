package com.mageventory.processor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.res.ResourceStateDao;
import com.mageventory.res.ResourceCache;

public class ResExampleFeedProcessor implements IProcessor, MageventoryConstants {

	private static final DefaultHttpClient HTTP_CLIENT = new DefaultHttpClient();
	private static final DocumentBuilderFactory DBUILDER_FACTORY = DocumentBuilderFactory.newInstance();
	private static final DocumentBuilder DBUILDER;

	static {
		try {
			DBUILDER_FACTORY.setNamespaceAware(true);
			DBUILDER = DBUILDER_FACTORY.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Bundle process(Context context, String[] params, Bundle extras, String resourceUri, ResourceStateDao state,
			ResourceCache store) {
		try {
			final String feedUrl = params[0];

			state.addResource(resourceUri);
			state.setState(resourceUri, STATE_BUILDING);

			state.setTransacting(resourceUri, true);
			final HttpGet get = new HttpGet(feedUrl);
			HttpResponse resp = HTTP_CLIENT.execute(get);
			final HttpEntity entity = resp.getEntity();
			InputStream content = new BufferedInputStream(entity.getContent());

			Document doc = null;
			boolean parseSuccess = true;

			try {
				doc = DBUILDER.parse(content);
			} catch (Throwable e) {
				parseSuccess = false;
			} finally {
				try {
					content.close();
				} catch (IOException ignored) {
				}
			}
			state.setTransacting(resourceUri, false);

			if (parseSuccess == false) {
				state.setState(resourceUri, STATE_NONE);
				return null;
			}

			final NodeList thumbnailNodes = doc.getElementsByTagNameNS("http://search.yahoo.com/mrss/", "thumbnail");
			final List<String> thumbnails = new LinkedList<String>();
			for (int i = 0; i < thumbnailNodes.getLength(); i++) {
				final Node thumbnail = thumbnailNodes.item(i);
				try {
					thumbnails.add(thumbnail.getAttributes().getNamedItem("url").getNodeValue());
				} catch (Throwable ignored) {
					// in case of error we're just not going to add this
					// thumbnail to the list
				}
			}

			if (thumbnails.isEmpty() == false) {
				store.store(context, resourceUri, thumbnails);
				state.setState(resourceUri, STATE_AVAILABLE);
			} else {
				state.setState(resourceUri, STATE_NONE);
			}
			return null;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

}
