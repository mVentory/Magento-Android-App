package com.mageventory.res;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import com.jakewharton.DiskLruCache;
import com.jakewharton.DiskLruCache.Editor;
import com.jakewharton.DiskLruCache.Snapshot;

/**
 * A cache implementation that's using the DiskLruCache (
 * https://github.com/JakeWharton/DiskLruCache ). ALL THE DATA IS PERSISTED AS
 * LONG AS THIS OBJECT IS KEPT IN JAVA MEMORY.
 */
public class ResourceCache implements ResourceConstants {

	private static ResourceCache instance;

	private static DiskLruCache sCache;

	public static boolean contains(final Context context, final String uri) {
		try {
			final DiskLruCache cache = getCache(context);
			return cache.get(uriToFilename(uri)) != null;
		} catch (Throwable e) {
			return false;
		}
	}

	private static DiskLruCache getCache(final Context context)
			throws IOException {
		if (sCache == null) {
			final File cacheDir = new File(context.getFilesDir(),
					CACHE_DIRECTORY);
			sCache = DiskLruCache.open(cacheDir, CACHE_VERSION, 1, CACHE_SIZE);
		}
		return sCache;
	}

	public static ResourceCache getInstance() {
		if (instance == null) {
			synchronized (ResourceCache.class) {
				if (instance == null) {
					instance = new ResourceCache();
				}
			}
		}
		return instance;
	}

	static String uriToFilename(String uri) {
		if (TextUtils.isEmpty(uri)) {
			throw new IllegalArgumentException("uri should not be empty");
		}
		if (uri.length() > 20) {
			uri = uri.substring(uri.length() - 20);
		}
		String filename = new String(Base64.encode(uri.getBytes(),
				Base64.NO_PADDING | Base64.NO_WRAP));
		filename = filename.replace(File.separatorChar, '_');
		return filename;
	}

	@SuppressWarnings("unchecked")
	public <T> T restore(final Context context, final String uri)
			throws IOException {
		final DiskLruCache cache = getCache(context);
		final Snapshot snapshot = cache.get(uriToFilename(uri));
		if (snapshot == null) {
			return null;
		}
		final ObjectInputStream in = new ObjectInputStream(
				snapshot.getInputStream(0));
		try {
			return (T) in.readObject();
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public void store(final Context context, final String uri, Object data)
			throws IOException {
		final DiskLruCache cache = getCache(context);
		final Editor editor = cache.edit(uriToFilename(uri));
		if (editor == null) {
			throw new IOException();
		}
		final ObjectOutputStream out = new ObjectOutputStream(
				editor.newOutputStream(0));
		out.writeObject(data);
		out.close();
		editor.commit();
	}

	public void store(final Context context, final String uri,
			final InputStream in) throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream(8 * 1024);
		final byte[] buf = new byte[8 * 1024];
		int read;
		while ((read = in.read(buf)) != -1) {
			out.write(buf, 0, read);
		}
		try {
			in.close();
		} catch (IOException ignored) {
		}
		final byte[] data = out.toByteArray();
		store(context, uri, data);
	}

}
