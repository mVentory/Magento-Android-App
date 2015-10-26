package com.mageventory.util.run;

/**
 * Simple callable with parameter and result interface. Can be used to pass
 * execution logic to methods as a parameter
 * 
 * @author Eugene Popovich
 * @param <PARAM>
 * @param <RESULT>
 */
public interface CallableWithParameterAndResult<PARAM, RESULT> {
    RESULT call(PARAM p);
}
