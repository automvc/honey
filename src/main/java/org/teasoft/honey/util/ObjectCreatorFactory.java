/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

import java.util.HashMap;
import java.util.Map;

import org.teasoft.honey.osql.core.Logger;

/**
 * @author Kingstar
 * @since  1.9
 */
public class ObjectCreatorFactory {

	private ObjectCreatorFactory() {}

	@SuppressWarnings("rawtypes")
	public static Object create(String s, Class c) {

		if (c == null) return null;
		if (s == null) return null; // fix bug 2021-05-24

		if (c.equals(String.class)) return ObjectCreator.createString(s);

		s = s.trim();
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
		} else if (c.equals(float.class) || c.equals(Float.class)) {
			return ObjectCreator.createFloat(s);
		} else {
			Logger.warn("when create Object, do not support this type :" + c.getName());
			return null;
		}
	}

	@SuppressWarnings("rawtypes")
	private static Map<String, Class> classMap = null;
	static {
		classMap = new HashMap<>();
		classMap.put("int", int.class);
		classMap.put("short", short.class);
		classMap.put("byte", byte.class);
		classMap.put("double", double.class);
		classMap.put("float", float.class);
		classMap.put("long", long.class);
		classMap.put("boolean", boolean.class);

		classMap.put("java.lang.Integer", Integer.class);
		classMap.put("java.lang.Short", Short.class);
		classMap.put("java.lang.Byte", Byte.class);
		classMap.put("java.lang.Double", Double.class);
		classMap.put("java.lang.Float", Float.class);
		classMap.put("java.lang.Long", Long.class);
		classMap.put("java.lang.Boolean", Boolean.class);

		classMap.put("java.lang.String", String.class);
	}

	public static Object createByString(String s, String className) {
		return create(className, classMap.get(className));
	}
}
