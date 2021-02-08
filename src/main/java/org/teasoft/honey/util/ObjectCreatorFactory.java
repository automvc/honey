/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

/**
 * @author Kingstar
 * @since  1.9
 */
public class ObjectCreatorFactory {
	
	@SuppressWarnings("rawtypes")
	public static Object create(String s, Class c) {

		if (c == null) return null;
		
		if (c.equals(String.class))
			return ObjectCreator.createString(s);
			
		s=s.trim();
		if (c.equals(int.class) || c.equals(Integer.class)) {
			return ObjectCreator.createInt(s);
		} else if (c.equals(short.class) || c.equals(Short.class)) {
			return ObjectCreator.createShort(s);
		} else if (c.equals(byte.class) || c.equals(Byte.class)) {
			return ObjectCreator.createByte(s);
		} else if (c.equals(double.class) || c.equals(Double.class)) {
			return ObjectCreator.createDouble(s);
		} else if (c.equals(long.class) || c.equals(Long.class)) {
			return ObjectCreator.createLong(s);
		} else if (c.equals(boolean.class) || c.equals(Boolean.class)) {
			return ObjectCreator.createBoolean(s);
		} else {
			return null;
//			throw new Exception("Not support the type: " + c.getName());
		}
	}
}
