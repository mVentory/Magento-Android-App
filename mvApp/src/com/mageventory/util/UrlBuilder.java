
package com.mageventory.util;

public class UrlBuilder {

    private static final String SEP = "/";

    public static String join(String url, String path) {
        if (url.endsWith(SEP) == false) {
            url = url.concat(SEP);
        }
        if (path.startsWith(SEP)) {
            path = path.substring(1);
        }
        return url.concat(path);
    }

}
