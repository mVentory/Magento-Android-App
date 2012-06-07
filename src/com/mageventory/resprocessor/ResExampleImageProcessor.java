package com.mageventory.resprocessor;

import java.io.BufferedInputStream;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.res.ResourceStateDao;
import com.mageventory.res.ResourceCache;

public class ResExampleImageProcessor implements IProcessor,
		MageventoryConstants {

	private static final DefaultHttpClient HTTP_CLIENT = new DefaultHttpClient();

	@Override
	public Bundle process(Context context, String[] params, Bundle extras,
			String resourceUri, ResourceStateDao state, ResourceCache store) {
		try {
			final String imageUrl = params[0];

			state.addResource(resourceUri);
			state.setState(resourceUri, STATE_BUILDING);

			state.setTransacting(resourceUri, true);
			final HttpGet get = new HttpGet(imageUrl);
			HttpResponse resp = HTTP_CLIENT.execute(get);
			final HttpEntity entity = resp.getEntity();
			final InputStream content = new BufferedInputStream(
					entity.getContent());
			store.store(context, resourceUri, content);
			state.setTransacting(resourceUri, false);
			state.setState(resourceUri, STATE_AVAILABLE);
			return null;
		} catch (Throwable e) {
			state.setTransacting(resourceUri, false);
			state.setState(resourceUri, STATE_NONE);
			throw new RuntimeException(e);
		}
	}

}
