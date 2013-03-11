package com.vaguehope.onosendai.util;

public class EqualHelper {
	
	static public boolean equal(Object aThis, Object aThat){
		return aThis == null ? aThat == null : aThis.equals(aThat);
	}
	
}
