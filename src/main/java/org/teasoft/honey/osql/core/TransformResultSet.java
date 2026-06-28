/*
 * Copyright 2013-2026 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.teasoft.bee.osql.annotation.customizable.Json;
import org.teasoft.bee.osql.type.TypeHandler;
import org.teasoft.bee.sharding.ShardingSortStruct;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.type.TypeHandlerRegistry;
import org.teasoft.honey.osql.util.AnnoUtil;
import org.teasoft.honey.sharding.ShardingUtil;
import org.teasoft.honey.util.ObjectUtils;
import org.teasoft.honey.util.StringUtils;

/**
 * Transform ResultSet.
 * 
 * @author Kingstar
 * @since 1.1
 */
public class TransformResultSet {

	private TransformResultSet() {
	}

	@SuppressWarnings("rawtypes")
	public static JsonResultWrap toJson(ResultSet rs, Class entityClass) throws SQLException {
		StringBuffer json = new StringBuffer("");
		ResultSetMetaData rmeta = rs.getMetaData();
		int columnCount = rmeta.getColumnCount();
		boolean ignoreNull = HoneyConfig.getHoneyConfig().selectJson_ignoreNull;
		String temp = "";

		boolean dateWithMillisecond = HoneyConfig.getHoneyConfig().selectJson_dateWithMillisecond;
		boolean timeWithMillisecond = HoneyConfig.getHoneyConfig().selectJson_timeWithMillisecond;
		boolean timestampWithMillisecond = HoneyConfig.getHoneyConfig().selectJson_timestampWithMillisecond;
		boolean longToString = HoneyConfig.getHoneyConfig().selectJson_longToString;
		int rowCount = 0;
		boolean isJsonString = false;
		Field currField = null;
		String fieldName = "";
		String fieldTypeName = "";

		while (rs.next()) {
			rowCount++;
			json.append(",{");
			for (int i = 1; i <= columnCount; i++) { // 1..n
				if (rs.getString(i) == null && ignoreNull) {
					continue;
				}

				isJsonString = false;
//				fieldName=_toFieldName(rmeta.getColumnName(i),entityClass);
				fieldName = _toFieldName(rmeta.getColumnLabel(i), entityClass);// V2.1.8
				fieldTypeName = HoneyUtil.getFieldType(rmeta.getColumnTypeName(i));

				json.append("\"");
				json.append(fieldName);
				json.append("\":");

				if (rs.getString(i) != null) {

					temp = rs.getString(i);

					// Json类型,不用再转换引号
					if ("JSON".equals(fieldTypeName)) {
						isJsonString = true;
					} else if (entityClass != null) {
						try {
							currField = HoneyUtil.getField(entityClass, fieldName);
							isJsonString = isJoson(currField);
						} catch (NoSuchFieldException e) {
							// ignore
						}
					}

					if (isJsonString) {
						json.append(temp);
					} else if ("String".equals(fieldTypeName)) { // equals改为不区分大小写 其它几个也是. 不需要,Map中值是这种命名风格的
						json.append("\"");

						temp = temp.replace("\\", "\\\\"); // 1
						temp = temp.replace("\"", "\\\""); // 2

						json.append(temp);
						json.append("\"");
					} else if ("Date".equals(fieldTypeName)) {
						if (dateWithMillisecond) {
							json.append(rs.getDate(i).getTime());
						} else {
							try {
//								temp = rs.getString(i);
								Long.valueOf(temp); // test value
								json.append(temp);
							} catch (NumberFormatException e) {
								json.append("\"");
								json.append(temp.replace("\"", "\\\""));
								json.append("\"");
							}
						}
					} else if ("Time".equals(fieldTypeName)) {
						if (timeWithMillisecond) {
							json.append(rs.getTime(i).getTime());
						} else {
							try {
//								temp = rs.getString(i);
								Long.valueOf(temp); // test value
								json.append(temp);
							} catch (NumberFormatException e) {
								json.append("\"");
								json.append(temp.replace("\"", "\\\""));
								json.append("\"");
							}
						}
					} else if ("Timestamp".equals(fieldTypeName)) {
						if (timestampWithMillisecond) {
							json.append(rs.getTimestamp(i).getTime());
						} else {
							try {
//								temp = rs.getString(i);
								Long.valueOf(temp); // test value
								json.append(temp);
							} catch (NumberFormatException e) {
								json.append("\"");
								json.append(temp.replace("\"", "\\\""));
								json.append("\"");
							}
						}
					} else if (longToString && "Long".equals(fieldTypeName)) {
						json.append("\"");
						json.append(rs.getString(i));
						json.append("\"");
					} else {
						json.append(rs.getString(i));
					}

				} else {// null
					json.append(rs.getString(i));
				}

				if (i != columnCount) json.append(","); // fixed bug. if last field is null and ignore.
			} // one record end
			if (json.toString().endsWith(",")) json.deleteCharAt(json.length() - 1); // fix bug
			json.append("}");
		} // array end
		if (json.length() > 0) {
			json.deleteCharAt(0);
		}
		json.insert(0, "[");
		json.append("]");

		return new JsonResultWrap(json.toString(), rowCount);
	}

	@SuppressWarnings("rawtypes")
	private static String _toFieldName(String columnName, Class entityClass) {
		return NameTranslateHandle.toFieldName(columnName, entityClass);
	}

	public static List<String[]> toStringsList(ResultSet rs) throws SQLException {
		List<String[]> list = new ArrayList<>();
		ResultSetMetaData rmeta = rs.getMetaData();
		int columnCount = rmeta.getColumnCount();
		boolean nullToEmptyString = HoneyConfig.getHoneyConfig().returnStringList_nullToEmptyString;
		String str[] = null;
		boolean firstRow = true;
		while (rs.next()) {
			str = new String[columnCount];
			for (int i = 0; i < columnCount; i++) {
				if (nullToEmptyString && rs.getString(i + 1) == null) {
					str[i] = "";
				} else {
					str[i] = rs.getString(i + 1);
				}
				if (firstRow) { // 2.0
					firstRow = false;
					regSort(rmeta);
				}
			}
			list.add(str);
		}
		return list;
	}

	// 2.0
	static void regSort(ResultSetMetaData rmeta) {
		if (!ShardingUtil.hadSharding()) return;
		ShardingSortStruct struct = HoneyContext.getCurrentShardingSort();
		if (struct == null) return;
		String orderFields[] = struct.getOrderFields();
		if (orderFields == null) return;

		if (struct.isRegFlag()) return; // 如何有其它子线程已处理,则不再处理.
		struct.setRegFlag(true);

		int orderFieldsLen = orderFields.length;
		String type[] = new String[orderFieldsLen];
		int index[] = new int[orderFieldsLen];

		String fieldName;
		String javaType;
		try {
			int k = 0;
			int columnCount = rmeta.getColumnCount();
			for (int i = 0; i < columnCount; i++) {
//				fieldName = _toFieldName(rmeta.getColumnName(i + 1), null);
				fieldName = _toFieldName(rmeta.getColumnLabel(i + 1), null); // V2.1.8
//				if (!isOrderField(orderFields, fieldName)) continue;
				for (int j = 0; j < orderFieldsLen; j++) {
					if (fieldName.equals(orderFields[j])) {
						javaType = HoneyUtil.getFieldType(rmeta.getColumnTypeName(i + 1).trim());
						type[j] = javaType;
						index[k] = i;
						k++;
						break;
					}
				}
				if (k == orderFieldsLen) break;
			}
			if (k != 0) {
				struct.setIndex(index);
				struct.setType(type);
				HoneyContext.setCurrentShardingSort(struct);
			}
		} catch (SQLException e) {
			Logger.debug(e.getMessage(), e);
		}
	}

	public static List<Map<String, Object>> toMapList(ResultSet rs) throws SQLException {
		List<Map<String, Object>> list = new ArrayList<>();
		ResultSetMetaData rmeta = rs.getMetaData();
		int columnCount = rmeta.getColumnCount();
		Map<String, Object> rowMap = null;
		while (rs.next()) {
//			rowMap=new HashMap<>();
			rowMap = new LinkedHashMap<>(); // 2021-06-13
			for (int i = 1; i <= columnCount; i++) {
//				rowMap.put(_toFieldName(rmeta.getColumnName(i),null), rs.getObject(i)); //ignore Column annotation
				// V2.1.8
				rowMap.put(_toFieldName(rmeta.getColumnLabel(i), null), rs.getObject(i)); // ignore Column annotation
			}
			list.add(rowMap);
		}
		return list;
	}

	// 检测是否有Json注解
	private static boolean isJoson(Field field) {
		return AnnoUtil.isJson(field);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object jsonHandlerProcess(Field field, Object obj, TypeHandler jsonHandler) {
		if (List.class.isAssignableFrom(field.getType())) {
			Object newObj[] = new Object[2];
			newObj[0] = obj;
			newObj[1] = field;
			obj = jsonHandler.process(field.getType(), newObj);
		} else {
			obj = jsonHandler.process(field.getType(), obj);
		}
		return obj;
	}

	private static Object _getObjectByindex(ResultSet rs, Field field, int index) throws SQLException {
		return HoneyUtil.getResultObjectByIndex(rs, field.getType().getName(), index);
	}

	private static boolean openFieldTypeHandler = HoneyConfig.getHoneyConfig().openFieldTypeHandler;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> T rowToEntity(ResultSet rs, Class<T> clazz)
			throws SQLException, IllegalAccessException, InstantiationException {

		T targetObj = null;
		targetObj = clazz.newInstance();
		ResultSetMetaData rmeta = rs.getMetaData();

		if (rs.isBeforeFirst()) rs.next();

		int columnCount = rmeta.getColumnCount();
		Field field = null;
		String name = null;
		boolean firstRow = true;
		for (int i = 0; i < columnCount; i++) {
			try {
//				name = _toFieldName(rmeta.getColumnName(i + 1), clazz);
//				 支持父类时，是否有影响？ todo
				// fixed bug,V2.1.8. 获取用于打印输出和显示的指定列的建议标题。建议标题通常由 SQL AS
				// 子句来指定。如果未指定 SQL AS，则从 getColumnLabel返回的值将和getColumnName 方法返回的值相同。
				name = _toFieldName(rmeta.getColumnLabel(i + 1), clazz);
				field = HoneyUtil.getField(clazz, name);// 可能会找不到Javabean的字段
			} catch (NoSuchFieldException e) {
				continue;
			}
			if (firstRow) { // 2.0
				firstRow = false;
				regSort(rmeta);
			}
			HoneyUtil.setAccessibleTrue(field);
			Object obj = null;
			boolean isRegHandlerPriority = false;
			try {
				boolean processAsJson = false;
				// isJoson> isRegHandlerPriority(if open)
				if (isJoson(field)) {
					obj = rs.getString(i + 1);
					TypeHandler jsonHandler = TypeHandlerRegistry.getHandler(Json.class);
					if (jsonHandler != null) {
						obj = jsonHandlerProcess(field, obj, jsonHandler);
						processAsJson = true;
					}
				} else {
					if (openFieldTypeHandler) {
						isRegHandlerPriority = TypeHandlerRegistry.isPriorityType(field.getType());
					}
				}

				if (!processAsJson) obj = rs.getObject(i + 1);

				if (isRegHandlerPriority) {
					obj = TypeHandlerRegistry.handlerProcess(field.getType(), obj);
				}

//				java.lang.IllegalArgumentException: Can not set java.lang.Integer field org.teasoft.exam.moretable3.simple.TestUser.id to java.lang.Long
				HoneyUtil.setFieldValue(field, targetObj, obj); // 对相应Field设置
			} catch (IllegalArgumentException e) {
//				e.printStackTrace();
				boolean alreadyProcess = false;
				obj = _getObjectByindex(rs, field, i + 1);
//				obj = rs.getObject(i + 1,field.getType()); //oracle do not support
				if (openFieldTypeHandler) {
					Class type = field.getType();
					TypeHandler handler = TypeHandlerRegistry.getHandler(type);
					if (handler != null) {
						try {
							Object newObj = handler.process(type, obj);
							HoneyUtil.setFieldValue(field, targetObj, newObj);
							alreadyProcess = true;
						} catch (Exception e2) {
							alreadyProcess = false;
						}
					}
				}
				if (!alreadyProcess) {
					HoneyUtil.setFieldValue(field, targetObj, obj);
				}
			}
		}

		return targetObj;
	}

	public static <T> List<T> rsToListEntity(ResultSet rs, Class<T> entityClass) {
		List<T> rsList = null;
		T targetObj = null;
		try {
			rsList = new ArrayList<>();
			while (rs.next()) {
				targetObj = ResultAssemblerHandler.rowToEntity(rs, entityClass);
				rsList.add(targetObj);
			}
		} catch (Exception e) {
			throw ExceptionHelper.convert(e);
		} finally {
			targetObj = null;
		}
		return rsList;
	}
	
	private static final int ONE_ROW_TYPE_RS = 1;
	private static final int ONE_ROW_TYPE_MAP = 2;
    
	//3.0.0
	public static <T> List<MoreTableResultWrapper<T>> toMidRsForMoretable(ResultSet rs, T entity) {

		List<MoreTableResultWrapper<T>> list = new ArrayList<>();
		try {
			MoreTableResultWrapper<T> wrapper = null; // one row
			Map<String, Map<String, Field>> nameAndFieldCache = new HashMap<>();
			int rowNum = 0;
			
			Map<String, MoreTableStruct3> moreTableStructMap = ParseSqlHelper.parseJoins(entity);
			List<String> mtStructKeys = new ArrayList<>(moreTableStructMap.keySet());
			mtStructKeys.add(0, HoneyUtil.toTableName(entity.getClass()));// put main table in first.
			
			while (rs.next()) {
				if (rs.isBeforeFirst()) rs.next();
				
				rowNum++;
				wrapper=parseOneRow(entity, mtStructKeys, moreTableStructMap, nameAndFieldCache, ONE_ROW_TYPE_RS,rs,null);
				
				// add wrapper(from one row record) to list.
				list.add(wrapper);
			} // while rs

			if (!list.isEmpty()) list.get(0).rawRows = rowNum;

		} catch (Exception e) {
			throw ExceptionHelper.convert(e);
		}
		return list;
	}
	
	// for Android app //3.0.0
	public static <T> List<MoreTableResultWrapper<T>> toMidRsForMoretable(List<Map<String, String>> rsMapList, T entity) {

		List<MoreTableResultWrapper<T>> list = new ArrayList<>();
		try {
			MoreTableResultWrapper<T> wrapper = null; // one row
			Map<String, Map<String, Field>> nameAndFieldCache = new HashMap<>();
			int rowNum = 0;

			Map<String, MoreTableStruct3> moreTableStructMap = ParseSqlHelper.parseJoins(entity);
			List<String> mtStructKeys = new ArrayList<>(moreTableStructMap.keySet());
			mtStructKeys.add(0, HoneyUtil.toTableName(entity.getClass()));// put main table in first.

			Map<String, String> rsMap = null;
			for (int m = 0; m < rsMapList.size(); m++) {
				rsMap = rsMapList.get(m);

				rowNum++;
				wrapper = parseOneRow(entity, mtStructKeys, moreTableStructMap, nameAndFieldCache,ONE_ROW_TYPE_MAP,null,rsMap);

				// add wrapper(from one row record) to list.
				list.add(wrapper);

				list.add(wrapper);
			} // while rs

			if (!list.isEmpty()) list.get(0).rawRows = rowNum;

		} catch (Exception e) {
			throw ExceptionHelper.convert(e);
		}
		return list;

	}
	
	private static <T> MoreTableResultWrapper<T> parseOneRow(T entity, List<String> mtStructKeys,
			Map<String, MoreTableStruct3> moreTableStructMap,
			Map<String, Map<String, Field>> nameAndFieldCache,
			int oneRowType,
			ResultSet rs,
			Map<String, String> rsMap) {

		Object value = null;
		T mainObj = null;
		Object subObj = null;
		MoreTableStruct3 subEntityMoreTableStruct = null;
		Map<String, Object> subObjectCache = null;
		Map<String, StringBuffer> subObjValueStrCache = null;
		String aliasColumnName;

		StringBuffer mainObjValueStr = null;
		StringBuffer subObjValueStr = null;

		mainObj = null;
		mainObjValueStr = new StringBuffer();
		subObjectCache = new HashMap<>();
		subObjValueStrCache = new HashMap<>();

		String dulField;
		String columnName = null;
		// 按某个实体进行设置
//		for (int i = 0; i < fields2.length; i++) {
//		int columnCount = rmeta.getColumnCount();
		// 按查询的字段遍历
//		  for (int i = 0; i < columnCount; i++) {
//		    String aliasColumnName = rmeta.getColumnLabel(i + 1); //即使写的sql有带表名，这个也不会返回带的字段名。
//		    //除非全部加别名，不然无法区分主从表的同名字段。
		
		Field field = null;
		MoreTableResultWrapper<T> wrapper = new MoreTableResultWrapper<>(); // one row
		try {
			Map<String, String> subDulColumnMap = null;
			boolean isConfuseDuplicateFieldDB = isConfuseDuplicateFieldDB();
			for (int i = 0; i < mtStructKeys.size(); i++) {
				Map<String, Field> nameAndField = null;
				boolean isMainField = false;
				String tableName;
				if (i == 0) { // Main table
					isMainField = true;
					tableName = mtStructKeys.get(i);
					nameAndField = nameAndFieldCache.get(tableName);
					if (nameAndField == null) {
						nameAndField = HoneyUtil.getNameAndField(entity.getClass());
						nameAndFieldCache.put(tableName, nameAndField);
					}

					// fixed
					String mainAlias = moreTableStructMap.get(mtStructKeys.get(1)).mainAlias;// 第一个子表里,设置有主表的别名
					if (StringUtils.isNotBlank(mainAlias)) {
						tableName = mainAlias;
					}
				} else {
					subEntityMoreTableStruct = moreTableStructMap.get(mtStructKeys.get(i));

					if (i == 1) { // 子表重名的字段； 只在第一个子表的MoreTableStruct设置
						subDulColumnMap = moreTableStructMap.get(mtStructKeys.get(1)).overall.subDulColumnMap;
					}
					tableName = subEntityMoreTableStruct.subAlias;
					nameAndField = nameAndFieldCache.get(tableName);
					if (nameAndField == null) {
						nameAndField = HoneyUtil.getNameAndField(subEntityMoreTableStruct.subClass);
						nameAndFieldCache.put(tableName, nameAndField);
					}
				} // 以上是从多表结构中获取实体的字段信息
				  // 以下开始对字段信息进行设置。是根据字段名得到列名，然后用列名为key，从rs中获取值。
				for (Entry<String, Field> entry : nameAndField.entrySet()) {

					subObjValueStr = null;

					field = entry.getValue();
					if (HoneyUtil.isSkipField(field)) continue;

					String fieldName = entry.getKey();
					if (isMainField) columnName = _toColumnName(fieldName, entity.getClass());
					else columnName = _toColumnName(fieldName, subEntityMoreTableStruct.subClass);
//					columnName = columnName.replace("`", ""); // have bug, if use ` TODO
					boolean isRegHandlerPriority = false;
					if (openFieldTypeHandler) {
						isRegHandlerPriority = TypeHandlerRegistry.isPriorityType(field.getType());
					}

					aliasColumnName = tableName + "." + columnName;
					// 0. transfer columnName if is ConfuseDuplicateFieldDB
					if (isConfuseDuplicateFieldDB) {
						if (i == 0) {
							aliasColumnName = columnName;
						} else { // i>=1 // 不支持用tableName + "." +
							// columnName获取,只能用columnName获取.
							dulField = subDulColumnMap.get(aliasColumnName);
							if (dulField != null) { // 重名字段使用转换后的
								aliasColumnName = dulField;
							} else {
								// isConfuseDuplicateFieldDB 不支持用tableName + "." + columnName获取,只能用columnName获取.
								aliasColumnName = columnName;
							}
						}
					}

					// 1. get value
					value = null;
					subObj = null;
					dulField = "";
					try {
						if (oneRowType == ONE_ROW_TYPE_RS)
							value = rs.getObject(aliasColumnName);
						else if (oneRowType == ONE_ROW_TYPE_MAP)
							value = rsMap.get(aliasColumnName);
					} catch (SQLException e) {
						if (oneRowType == ONE_ROW_TYPE_RS) {
							try {
								value = HoneyUtil.getResultObject(rs, field.getType().getName(), aliasColumnName);
							} catch (SQLException e2) {
								continue;
							}
						} else {
							Logger.warn(e.getMessage(), e);
							throw ExceptionHelper.convert(e);
						}
					}

					// 2. process value
					boolean processAsJson = false;
					if (isJoson(field)) {
						TypeHandler jsonHandler = TypeHandlerRegistry.getHandler(Json.class);
						if (jsonHandler != null) {
							value = jsonHandlerProcess(field, value, jsonHandler);
							processAsJson = true;
						}
					}

					if (!processAsJson && isRegHandlerPriority) { // process value by handler
						value = TypeHandlerRegistry.handlerProcess(field.getType(), value);
					}

					// 3. set value
					HoneyUtil.setAccessibleTrue(field);

					if (isMainField) {
						if (mainObj == null) mainObj = (T) createObject(entity.getClass());
						try {
							HoneyUtil.setFieldValue(field, mainObj, value);
						} catch (IllegalArgumentException e) {
							if (oneRowType == ONE_ROW_TYPE_RS) {
								// 若因类型不对，设置报异常。则不可能再设置。 mysql会,但sqlite不会。
								// java.lang.IllegalArgumentException: Can not set java.lang.Integer field
								// xxx.XxxEntity.id to java.lang.Long
								try {
									value = HoneyUtil.getResultObject(rs, field.getType().getName(), aliasColumnName);
									HoneyUtil.setFieldValue(field, mainObj, value);
								} catch (SQLException e2) {
									throw ExceptionHelper.convert(e);
								}
							} else {
								Logger.warn(e.getMessage(), e);
							}
						}
						mainObjValueStr.append("#").append(String.valueOf(value));
					} else {
						subObj = subObjectCache.get(tableName);
						if (value == null && i > 0) continue; // TODO fixed 空值不设置; 如何判断查到的整个对象的属性都是null才不设置？

						if (subObj == null) {
							subObj = createObject(subEntityMoreTableStruct.subClass);
							subObjectCache.put(tableName, subObj);
							subObjValueStrCache.put(tableName, new StringBuffer());
						}
						try {
							HoneyUtil.setFieldValue(field, subObj, value);
						} catch (IllegalArgumentException e) {
							if (oneRowType == ONE_ROW_TYPE_RS) {
								try {
									value = HoneyUtil.getResultObject(rs, field.getType().getName(), aliasColumnName);
									HoneyUtil.setFieldValue(field, subObj, value);
								} catch (SQLException e2) {
									throw ExceptionHelper.convert(e);
								}
							} else {
								Logger.warn(e.getMessage(), e);
							}
						}
						subObjValueStr = subObjValueStrCache.get(tableName);
						subObjValueStr.append("#").append(String.valueOf(value));
					}
				} // for entity's fields
			} // for moreTableStructMap
		} catch (IllegalArgumentException | NoSuchMethodException | InvocationTargetException | IllegalAccessException
				| InstantiationException e) {
			throw ExceptionHelper.convert(e);
		}

		// one row
		wrapper.mainObj = mainObj;
		wrapper.mainObjValueStr = mainObjValueStr.toString();
		wrapper.subObjMap = subObjectCache;
		wrapper.subObjValueStrMap = subObjValueStrCache;

		return wrapper;
	}

	@SuppressWarnings("rawtypes")
	private static String _toColumnName(String fieldName, Class entityClass) {
		return HoneyUtil.toColumnName(fieldName, entityClass);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static Object createObject(Class c) throws IllegalAccessException, InstantiationException,
			NoSuchMethodException, InvocationTargetException {
		try {
			return c.newInstance();
		} catch (IllegalAccessException e) {
			// entity is not public
			Constructor ctor = c.getDeclaredConstructor();
			ctor.setAccessible(true);
			return ctor.newInstance();
		}
	}

	private static boolean isConfuseDuplicateFieldDB() {
		return HoneyUtil.isConfuseDuplicateFieldDB();
	}

	// 组装中间对象成最终结果 //3.0.0
	static <T> List<T> genRsViaMidRsForMoretable(List<MoreTableResultWrapper<T>> listResultWrapper,
			Class<T> entityClass,
			Map<String, MoreTableStruct3> moreTableStructMap)
			throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {

		List<T> rsList = new ArrayList<>();

		if (listResultWrapper.size() > 0)
			Logger.logSQL(" | <--  ( select raw record rows: " + listResultWrapper.get(0).rawRows + " )");

		Map<String, Field> mainNameAndField = HoneyUtil.getNameAndField(entityClass);
		Map<String, Field> subNameAndField;
		Map<String, Map<String, Field>> subNameAndFieldCache = new HashMap<>();

		// key0 or key1 : [subObject] # 对象list缓存
		Map<String, List> listCache = new HashMap<>(); // list_cache current_subObject_list_cache_dict
		// key1 or layer_key : subObject #单个对象缓存
		Map<String, Object> singleCache = new HashMap<>(); // current_single_subObject_cache_dict
		Set<String> one_to_one_for_two_layer_set = new HashSet();

		boolean noJoinAnno = ObjectUtils.isEmpty(moreTableStructMap);
		Integer no_obj_layer = null;
		Set<Integer> no_obj_layer_set = new HashSet<>();
		List<String> ptree;
		Field field;

		for (MoreTableResultWrapper<T> moreTableResultWrapper : listResultWrapper) {
			T main_obj = moreTableResultWrapper.mainObj;
			String main_key = moreTableResultWrapper.mainObjValueStr;
			Map<String, StringBuffer> sub_field_value_str_cache_dict = moreTableResultWrapper.subObjValueStrMap;
			Map<String, Object> reutrn_subObject_cache_dict = moreTableResultWrapper.subObjMap;

			if (noJoinAnno || ObjectUtils.isEmpty(reutrn_subObject_cache_dict)) {
				rsList.add(main_obj);
				continue;
			}

			int moreTableStructNum = moreTableStructMap == null ? 0 : moreTableStructMap.size();
			for (Entry<String, MoreTableStruct3> entry : moreTableStructMap.entrySet()) {
				MoreTableStruct3 mtStruct = entry.getValue();
				boolean processed = false;
				if (mtStruct.layer == 2) {

					// gen key0, layer_key
					ptree = mtStruct.parentTree;
					String key0 = main_key;
					StringBuffer sub_field_value_str = sub_field_value_str_cache_dict.get(ptree.get(0));
					if (sub_field_value_str == null) {
						no_obj_layer = mtStruct.layer; // 第一次出现
						if (mtStruct.hasNextLayer && no_obj_layer_set.add(no_obj_layer)) {
							// 有子层，要是没有查到对象，就打印
							Logger.info(
									"Not found the value in object {mtStruct.subAlias}, will ignore it and its sub layers!"
											.replace("{mtStruct.subAlias}", mtStruct.subAlias));
						}
						continue;
					}
					String current_key1 = "";
					if (sub_field_value_str.length() > 0) current_key1 = sub_field_value_str.toString();
					String layer_key = key0 + ".." + ptree.get(0) + "##" + current_key1;

					if (mtStruct.currentIsList) {
						processed = true;
						List sub_list_obj = listCache.get(key0);
						// 1:n:1 第二级是list的属性将第一层添加了； 非list的第二层属性也不用再添加第一层的对象。

						if (sub_list_obj == null) {
							sub_list_obj = new ArrayList();
							sub_list_obj.add(reutrn_subObject_cache_dict.get(mtStruct.subAlias));
							listCache.put(key0, sub_list_obj);

							field = mainNameAndField.get(mtStruct.fieldName);
							HoneyUtil.setAccessibleTrue(field);
//							只有主表层，main_obj==null,在有子表时，会自动创建。
							if (main_obj == null) main_obj = (T) TransformResultSet.createObject(entityClass); // fixed
							HoneyUtil.setFieldValue(field, main_obj, sub_list_obj);

							if (!one_to_one_for_two_layer_set.contains(key0)) {
								rsList.add(main_obj);
								one_to_one_for_two_layer_set.add(key0);
								// 1:n:1 第二级是list的属性将第一层添加了； 非list的第二层属性也不用再添加第一层的对象。
							}
							singleCache.put(layer_key, reutrn_subObject_cache_dict.get(mtStruct.subAlias));
						} else { // # 二级列表有了
//                        # 但第二级的对象还未加有，则要加到一级对象list下
							if (!singleCache.containsKey(layer_key)) {
								listCache.get(key0).add(reutrn_subObject_cache_dict.get(mtStruct.subAlias));
								singleCache.put(layer_key, reutrn_subObject_cache_dict.get(mtStruct.subAlias));
							}
						} // # else: 二级的对象已经加有了，就不用再加。已经存在，则不用放。
					} else { // # not list
						field = mainNameAndField.get(mtStruct.fieldName);
						HoneyUtil.setAccessibleTrue(field);
						if (main_obj == null) main_obj = (T) TransformResultSet.createObject(entityClass);// fixed
						HoneyUtil.setFieldValue(field, main_obj, reutrn_subObject_cache_dict.get(mtStruct.subAlias));
						// TODO fixed 1-1-n-1-1
						singleCache.put(layer_key, reutrn_subObject_cache_dict.get(mtStruct.subAlias));

						processed = true;
						// 第二层为1时，每行只需要添加一次;不然多层时会乱； 但使用这种了，要是一对多，想用这种方式查，则会忽略了主表相同的从表除第一条以外的数据。
						if (!one_to_one_for_two_layer_set.contains(key0)) {
							rsList.add(main_obj);
							if (moreTableStructNum > 1)// 只有一个子表时，还是允许一对多使用这种；即每行都添加(不放入set,则上一个if都会是true)。但多过一个子表时，则不允许。
								one_to_one_for_two_layer_set.add(key0);
						}
//                    # one has one时，第三层会找不到第二层的缓存；  因非list,第二层没放缓存。  是通过将三级子对象设置到二级子对象的属性完成对象关联的
					} // layer ==2 end
				} else if (mtStruct.layer >= 3) {
					if (no_obj_layer != null && mtStruct.layer > no_obj_layer) {
						continue; // 若该层没有数据返回，则直接不处理它的子类了，不能断层。
					} else {
						no_obj_layer = null; // reset 这样，同层的其它对象就可以被处理
					}
					if (!reutrn_subObject_cache_dict.containsKey(mtStruct.subAlias)) {
						if (mtStruct.hasNextLayer && no_obj_layer_set.add(no_obj_layer)) {
							// 有子层，要是没有查到对象，就打印
							Logger.info(
									"Not found the value in object {mtStruct.subAlias}, will ignore it and its sub layers!"
											.replace("{mtStruct.subAlias}", mtStruct.subAlias));
						}
						if (no_obj_layer == null) {
							no_obj_layer = mtStruct.layer; // 第一次出现
							continue;
						}
					}
					processed = true;

					// gen key1,layer_key
					ptree = mtStruct.parentTree;
					String key0 = main_key;
					StringBuffer sub_field_value_str = sub_field_value_str_cache_dict.get(ptree.get(0));
					String current_key1 = sub_field_value_str.toString();
					String key1 = key0 + ".." + ptree.get(0) + "##" + current_key1;

					// # ptree不存root,长度会比层数少1; key1只计算到ptree倒数第二层.
					int loop_n = mtStruct.layer - 2 - 1;
					if (loop_n >= 1) {
						for (int i = 1; i < loop_n + 1; i++) {
							sub_field_value_str = sub_field_value_str_cache_dict.get(ptree.get(i));
							String current_key_i = sub_field_value_str.toString();
							key1 = key1 + ".." + ptree.get(i) + "##" + current_key_i;
						}
					}

					StringBuffer sub_field_value_str2 = sub_field_value_str_cache_dict
							.get(ptree.get(mtStruct.layer - 2));

					if (sub_field_value_str2 == null) sub_field_value_str2 = new StringBuffer();// maybe no need

					String current_key2 = sub_field_value_str2.toString();
					String layer_key = key1 + ".." + ptree.get(mtStruct.layer - 2) + "##" + current_key2;

					Object current_single_subObject = singleCache.get(key1); // 用key1取?? TODO
					if (current_single_subObject != null) {
						if (mtStruct.currentIsList) { // 3-has-list
							List sub_list_obj = listCache.get(key1);
							if (sub_list_obj == null) {
								sub_list_obj = new ArrayList<>();
								sub_list_obj.add(reutrn_subObject_cache_dict.get(mtStruct.subAlias));
								subNameAndField = getSubNameAndField(subNameAndFieldCache, mtStruct.mainAlias,
										mtStruct.typeTree.get(mtStruct.layer - 2));
								field = subNameAndField.get(mtStruct.fieldName);
								if (field == null) {
									continue;
								}
								HoneyUtil.setAccessibleTrue(field);
								HoneyUtil.setFieldValue(field, current_single_subObject, sub_list_obj);

								listCache.put(key1, sub_list_obj);
								singleCache.put(layer_key, reutrn_subObject_cache_dict.get(mtStruct.subAlias));
							} else { // # 第三级列表是有了，
								// # 但，第三级的对象还未加有，则要加到二级对象list下
								if (!singleCache.containsKey(layer_key))
									listCache.get(key1).add(reutrn_subObject_cache_dict.get(mtStruct.subAlias));
							}
						} else { // is not list //3- has - not_list
							subNameAndField = getSubNameAndField(subNameAndFieldCache, mtStruct.mainAlias,
									mtStruct.typeTree.get(mtStruct.layer - 2));
							field = subNameAndField.get(mtStruct.fieldName);
							if (field == null) {
								continue;
							}
							HoneyUtil.setAccessibleTrue(field);
							HoneyUtil.setFieldValue(field, reutrn_subObject_cache_dict.get(mtStruct.mainAlias),
									reutrn_subObject_cache_dict.get(mtStruct.subAlias));

							// TODO fixed 1-n-1-n-1
							singleCache.put(layer_key, reutrn_subObject_cache_dict.get(mtStruct.subAlias));
						}
					} else {
//						System.err.println("-----------key1没有放缓存------------"); // TODO
						if (mtStruct.currentIsList) { // 3 - null -list
							// 还没放缓存； 也是list; 如: 1-1-n-1-1 第三层时，就要走这里。
//							System.err.println("还没放缓存； 也是list; 如:  1-1-n-1-1 第三层时，就要走这里。 layer: " + mtStruct.layer); // TODO
						} else { // 3 - null -not_list
							subNameAndField = getSubNameAndField(subNameAndFieldCache, mtStruct.mainAlias,
									mtStruct.typeTree.get(mtStruct.layer - 2));
							field = subNameAndField.get(mtStruct.fieldName);
							HoneyUtil.setAccessibleTrue(field);
							// eg: 设置layer3时，layer不能为null
							if (reutrn_subObject_cache_dict.get(mtStruct.mainAlias) == null) continue;

							HoneyUtil.setFieldValue(field, reutrn_subObject_cache_dict.get(mtStruct.mainAlias),
									reutrn_subObject_cache_dict.get(mtStruct.subAlias));
						}
					}
				}

				if (!processed) {
					rsList.add(main_obj);
				}
			} // for loop map of MoreTableStruct3 for each row result
		} // for loop ResultWrapper

		return rsList;
	}

	private static Map<String, Field> getSubNameAndField(Map<String, Map<String, Field>> nameAndFieldCache,
			String tableName, Class entityClass) {
		Map<String, Field> nameAndField = nameAndFieldCache.get(tableName);
		if (nameAndField == null) {
			nameAndField = HoneyUtil.getNameAndField(entityClass);
			nameAndFieldCache.put(tableName, nameAndField);
		}
		return nameAndField;
	}

}
