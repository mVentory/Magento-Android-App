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

/**
 * Utilities to work with numbers
 * 
 * @author Eugene Popovich
 */
public class NumberUtils {

    /**
     * Compare numbers. Also handles null cases
     * 
     * @param number1
     * @param number2
     * @return
     */
    public static boolean equals(Double number1, Double number2) {
        if (number1 == number2) {
            return true;
        } else if (number1 == null || number2 == null) {
            return false;
        } else {
            return number1.doubleValue() == number2.doubleValue();
        }
    }
}
