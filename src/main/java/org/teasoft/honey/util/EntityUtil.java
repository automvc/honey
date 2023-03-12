/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.osql.core.NameTranslateHandle;

/**
 * 实体相关工具类
 * @author Kingstar
 * @since  1.11
 */
public final class EntityUtil {
	
	private EntityUtil() {}

	/**
	 * 获取实体的字段名称,不作命名转换
	 * 会忽略部分注解字段
	 * @param entity 实体对象
	 * @return 实体的字段名称
	 */
	public static String getFieldNames(Object entity) {
		return getFieldNames(entity, false);
	}

	/**
	 * 获取实体的字段名称
	 * 会忽略部分注解字段
	 * @param entity 实体对象
	 * @param isTransform 是否命名转换标识
	 * @return 实体的字段名称
	 */
	public static String getFieldNames(Object entity, boolean isTransform) {
		if (entity == null) return "";
		Field fields[] = entity.getClass().getDeclaredFields();

		if (fields == null) return "";

		StringBuffer s = new StringBuffer();
		int len = fields.length;
		boolean isFirst = true;

		for (int i = 0; i < len; i++) {
			if (HoneyUtil.isSkipField(fields[i])) continue;
			if (HoneyUtil.isSkipFieldJustFetch(fields[i])) continue;

			if (isFirst) {
				isFirst = false;
			} else {
				s.append(",");
			}
			if (isTransform)
				s.append(NameTranslateHandle.toColumnName(fields[i].getName()));
			else
				s.append(fields[i].getName());

		}
		return s.toString();
	}
	
	public static boolean isList(Field field) {
		return List.class.isAssignableFrom(field.getType());
	}
	
	public static boolean isSet(Field field) {
		return Set.class.isAssignableFrom(field.getType());
	}
	
	public static boolean isMap(Field field) {
		return Map.class.isAssignableFrom(field.getType());
	}
	
	public static Class<?> getGenericType(Field field) {
		Type gType = field.getGenericType();
		if (gType instanceof ParameterizedType) {
			ParameterizedType paraType = (ParameterizedType) gType;
			Class<?> elementType = (Class<?>) paraType.getActualTypeArguments()[0];
			return elementType;
		} else {
			return null;
		}
	}
	public static Class<?>[] getGenericTypeArray(Field field) {
		Type gType = field.getGenericType();
		return getGenericTypeArray(gType);
		
	}
	public static Class<?>[] getGenericTypeArray(Type gType) {
		
		if (gType instanceof ParameterizedType) {
			ParameterizedType paraType = (ParameterizedType) gType;
			Type[] types=paraType.getActualTypeArguments();
			Class<?> elementTypes[]=new Class<?>[types.length];
            for (int i = 0; i < types.length; i++) {
            	if (types[i] instanceof ParameterizedType) {
            		Logger.warn("Do not support the Map element is Map,"+types[i].toString());
            	}else {
            		elementTypes[i]=(Class<?>)types[i];
            	}
			}
			return elementTypes;
		} else {
			return null;
		}
	}
	
	public static boolean isCustomBean(Field field) {
		
		if(field==null) return false;
		
		String typeName = field.getType().getName();
		
		return !(field.getType().isPrimitive() || typeName.startsWith("java.")
				|| typeName.startsWith("javax.")
				|| typeName.startsWith("org.teasoft.bee.")
				|| typeName.startsWith("org.teasoft.hoeny.")
				|| typeName.startsWith("org.teasoft.beex.")
				|| typeName.startsWith("org.teasoft.spring.")
				|| typeName.startsWith("org.w3c.") || typeName.startsWith("org.xml.")
				|| typeName.startsWith("android.") || typeName.startsWith("org.omg.")
				|| typeName.startsWith("ohos.")
				|| typeName.startsWith("sun.")
		       );
	}
	
	public static boolean isCustomBean(String typeName) {
//		String typeName = field.getType().getName();
//		field.getType().isPrimitive() //需要另外判断原生类型
		
		return !(typeName.startsWith("java.")
				|| typeName.startsWith("javax.")
				|| typeName.startsWith("org.teasoft.bee.")
				|| typeName.startsWith("org.teasoft.hoeny.")
				|| typeName.startsWith("org.teasoft.beex.")
				|| typeName.startsWith("org.teasoft.spring.")
				|| typeName.startsWith("org.w3c.") || typeName.startsWith("org.xml.")
				|| typeName.startsWith("android.") || typeName.startsWith("org.omg.")
				|| typeName.startsWith("ohos.")
				|| typeName.startsWith("sun.")
		);
	}
	
}
