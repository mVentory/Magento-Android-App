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

package com.mageventory;

/**
 * Extended {@link RuntimeException} for the easier error logging data analyze
 */
public class MageventoryRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code MageventoryRuntimeException} that includes the
     * current stack trace.
     */
    public MageventoryRuntimeException() {
    }

    /**
     * Constructs a new {@code MageventoryRuntimeException} with the current
     * stack trace and the specified detail message.
     * 
     * @param detailMessage the detail message for this exception.
     */
    public MageventoryRuntimeException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * Constructs a new {@code MageventoryRuntimeException} with the current
     * stack trace, the specified detail message and the specified cause.
     * 
     * @param detailMessage the detail message for this exception.
     * @param throwable the cause of this exception.
     */
    public MageventoryRuntimeException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    /**
     * Constructs a new {@code MageventoryRuntimeException} with the current
     * stack trace and the specified cause.
     * 
     * @param throwable the cause of this exception.
     */
    public MageventoryRuntimeException(Throwable throwable) {
        super(throwable);
    }

}
