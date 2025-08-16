/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.type;

import java.util.HashMap;
import java.util.Map;

import org.teasoft.bee.osql.Registry;
import org.teasoft.bee.osql.type.SetParaTypeConvert;
import org.teasoft.honey.osql.core.HoneyConfig;

/**
 * Field Type Converter Registry.
 * This Registry work for setting parameter to PreparedStatement.
 * @author Kingstar
 * @since  1.11
 */
public final class SetParaTypeConverterRegistry implements Registry {

//	private static final String PRIORITY = "1";
	private static final Map<Class<?>, SetParaTypeConvert<?>> convertersMap = new HashMap<>();
//	private static final Map<Class<?>, String> priorityMap = new HashMap<>();

	private static final Map<String, Map<Class<?>, SetParaTypeConvert<?>>> convertersMapForSpecialDB = new HashMap<>();

	/**
	 * register SetParaTypeConvert,it will effect if can not process by default.
	 * @param fieldType Javabean field type.
	 * @param converter converter for Javabean field type.
	 */
	public static <T> void register(Class<T> fieldType, SetParaTypeConvert<? extends T> converter) {
		convertersMap.put(fieldType, converter);
	}

	public static <T> void register(Class<T> fieldType, SetParaTypeConvert<? extends T> converter, String database) {
		Map<Class<?>, SetParaTypeConvert<?>> map = convertersMapForSpecialDB.get(database);
		if (map == null) map = new HashMap<>();
		map.put(fieldType, converter);
		convertersMapForSpecialDB.put(database, map);
	}

	/**
	 * register SetParaTypeConvert
	 * @param fieldType Javabean field type.
	 * @param converter converter for Javabean field type.
	 * @param isPriority whether use custom converter before default one.
	 */
//	public static <T> void register(Class<T> fieldType, SetParaTypeConvert<? extends T> converter, boolean isPriority) {
//		convertersMap.put(fieldType, converter);
//		if (isPriority) priorityMap.put(fieldType, PRIORITY);
//	}

	/**
	 * return the priority of the fieldType.
	 * @param fieldType Javabean field type.
	 * @return boolean value of priority
	 */
//	public static <T> boolean isPriorityType(Class<T> fieldType) {
//		return priorityMap.get(fieldType) == null ? false : true;
//	}

	/**
	 * return the register Converter of fieldType.
	 * Use the special DB first.
	 * @param fieldType Javabean field type.
	 * @return the register Converter of fieldType.
	 */
	@SuppressWarnings("unchecked")
	public static <T> SetParaTypeConvert<T> getConverter(Class<T> fieldType) {

		SetParaTypeConvert<?> converter = null;
		Map<Class<?>, SetParaTypeConvert<?>> map = convertersMapForSpecialDB
				.get(HoneyConfig.getHoneyConfig().getDbName());
		if (map != null) converter = map.get(fieldType);

		if (converter == null) {
			converter = convertersMap.get(fieldType);
		}
		return (SetParaTypeConvert<T>) converter;
	}

	/**
	 * process the result get from ResultSet with SetParaTypeConvert,and return new processed result.
	 * @param fieldType Javabean field type.
	 * @param value  result get from ResultSet.
	 * @return the result processed by SetParaTypeConvert.
	 */
	@SuppressWarnings("rawtypes")
	public static Object converterProcess(Class<?> fieldType, Object value) {
		SetParaTypeConvert converter = getConverter(fieldType);
		if (converter != null) value = converter.convert(value);
		return value;
	}
}
