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

package com.mageventory.model;

import android.content.Intent;
import android.graphics.drawable.Drawable;

/**
 * Contains the data needed to start a CROP application
 * 
 * @author Lorensius W. L. T <lorenz@londatiga.net>
 */
public class CropOption {
    public CharSequence title;
    public Drawable icon;
    public Intent appIntent;
}
