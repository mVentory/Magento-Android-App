/* Copyright (c) 2014 mVentory Ltd. (http://mventory.com)
 * 
* License       http://creativecommons.org/licenses/by-nc-nd/4.0/
* 
* NonCommercial — You may not use the material for commercial purposes. 
* NoDerivatives — If you compile, transform, or build upon the material,
* you may not distribute the modified material. 
* Attribution — You must give appropriate credit, provide a link to the license,
* and indicate if changes were made. You may do so in any reasonable manner, 
* but not in any way that suggests the licensor endorses you or your use. 
*/
package com.mageventory.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebUtils {
    /**
     * The pattern to extract top level domain from the host information
     * including domains like <something>.co.nz and so forth. Supported 3
     * letters subdomain near the domain zone
     */
    public static final Pattern TOP_LEVEL_DOMAIN_HOST_PATTERN = Pattern
            .compile(".*?([^.]+\\.(?:\\w{1,3}\\.)?[^.]+)");

    /**
     * Convert a InputStream into String
     * 
     * @param is inputStream to be converted into a string
     * @return content of InputStream in form of String
     * @throws IOException
     */
    public static String convertStreamToString(final InputStream is) throws IOException {
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }

    /**
     * Extract the top level domain from the host.
     * 
     * <p/>Example:
     * <br/>Input: <b>some.long.domain.co.nz</b>, Output: <b>domain.co.nz</b>
     * <br/>Input: <b>www.gooogle.com</b>, Output: <b>google.com</b>
     * 
     * @param host the host to extract top level domain from
     * @return
     */
    public static String getTopLevelDomainFromHost(String host)
    {
        Matcher m = TOP_LEVEL_DOMAIN_HOST_PATTERN.matcher(host);
        if (m.matches()) {
            host = m.group(1);
        }
        return host;
    }
}
