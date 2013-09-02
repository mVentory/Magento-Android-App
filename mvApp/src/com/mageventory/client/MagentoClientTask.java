
package com.mageventory.client;

public interface MagentoClientTask<V> {

    public V run() throws RetryAfterLoginException;

}
