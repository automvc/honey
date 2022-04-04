/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.type;

import java.util.HashMap;
import java.util.Map;

import org.teasoft.bee.osql.Registry;
import org.teasoft.bee.osql.type.TypeHandler;
import org.teasoft.honey.osql.core.HoneyConfig;

/**
 * Field Type Handler Registry.
 * This Registry work for handle ResultSet by select.
 * @author Kingstar
 * @since  1.11
 */
public final class TypeHandlerRegistry implements Registry {

	private static final String PRIORITY = "1";
	private static final Map<Class<?>, TypeHandler<?>> handlersMap = new HashMap<>();
	private static final Map<Class<?>, String> priorityMap = new HashMap<>();
	
	private static final Map<String,Map<Class<?>, TypeHandler<?>>> handlersMapForSpecialDB = new HashMap<>();

	/**
	 * register TypeHandler,it will effect if can not process by default.
	 * @param fieldType Javabean field type.
	 * @param handler handler for Javabean field type.
	 */
	public static <T> void register(Class<T> fieldType, TypeHandler<? extends T> handler) {
		handlersMap.put(fieldType, handler);
	}
	
	
	public static <T> void register(Class<T> fieldType, TypeHandler<? extends T> handler,String database) {
		Map<Class<?>, TypeHandler<?>> map=handlersMapForSpecialDB.get(database);
		if(map==null) map=new HashMap<>(); 
		map.put(fieldType, handler);
		handlersMapForSpecialDB.put(database, map);
	}

	/**
	 * register TypeHandler
	 * @param fieldType Javabean field type.
	 * @param handler handler for Javabean field type.
	 * @param isPriority whether use custom handler before default one.
	 */
	public static <T> void register(Class<T> fieldType, TypeHandler<? extends T> handler, boolean isPriority) {
		handlersMap.put(fieldType, handler);
		if (isPriority) priorityMap.put(fieldType, PRIORITY);
	}

	/**
	 * return the priority of the fieldType.
	 * @param fieldType Javabean field type.
	 * @return boolean value of priority
	 */
	public static <T> boolean isPriorityType(Class<T> fieldType) {
		return priorityMap.get(fieldType) == null ? false : true;
	}

	/**
	 * return the register Handler of fieldType.
	 * Use the special DB first.
	 * @param fieldType Javabean field type.
	 * @return the register Handler of fieldType.
	 */
	@SuppressWarnings("unchecked")
	public static <T> TypeHandler<T> getHandler(Class<T> fieldType) {
//		return (TypeHandler<T>) handlersMap.get(fieldType);
		
		TypeHandler<?> handler = null;
		Map<Class<?>, TypeHandler<?>> map = handlersMapForSpecialDB
				.get(HoneyConfig.getHoneyConfig().getDbName());
		if (map != null) handler = map.get(fieldType);
		if (handler == null) {
			handler = handlersMap.get(fieldType);
		}
		
		return (TypeHandler<T>) handler;
	}

	/**
	 * process the result get from ResultSet with TypeHandler,and return new processed result.
	 * @param fieldType Javabean field type.
	 * @param result  result get from ResultSet.
	 * @return the result processed by TypeHandler.
	 */
	public static <T> T handlerProcess(Class<T> fieldType, Object result) {
//		return getHandler(fieldType).process(fieldType, result);
		TypeHandler<T> handler=getHandler(fieldType);
		if(handler!=null) handler.process(fieldType, result);
		return (T)result;
	}
}
