
package com.mageventory.util;

/**
 * Interface for loading handler
 * 
 * @author Eugene Popovich
 */
public interface LoadingControl
{
    void startLoading();

    void stopLoading();

    boolean isLoading();
}
