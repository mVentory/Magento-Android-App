package com.mageventory.res;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import android.os.Handler;
import android.os.Message;

public class SmartCallback implements Handler.Callback {

	private final Map<Integer, Method> whatToMethod = new HashMap<Integer, Method>();

	public SmartCallback() {
		// for each method
		for (final Method method : getClass().getMethods()) {
			// check for this particular annotation
			if (method.isAnnotationPresent(OnMessageWhat.class)) {
				// check return type
				if ("boolean".equalsIgnoreCase(method.getReturnType()
						.toString()) == false) {
					throw new RuntimeException(
							"Method return type should be boolean: " + method);
				}
				// check parameters
				final Class<?>[] params = method.getParameterTypes();
				if (params.length != 1
						|| Message.class.equals(params[0]) == false) {
					throw new RuntimeException(
							"Method should accept only one parameter of type Message: "
									+ method);
				}
				// all good, add it to map
				final OnMessageWhat annotation = (OnMessageWhat) method
						.getAnnotations()[0];
				whatToMethod.put(annotation.value(), method);
			}
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		final Method method = whatToMethod.get(msg.what);
		if (method == null) {
			// handled
			throw new IllegalArgumentException("Message cannot be handled: "
					+ msg);
		}
		try {
			return (Boolean) method.invoke(this, msg);
		} catch (Throwable e) {
			return false;
		}
	}

}
