/*
 * Copyright 2013-2018 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.annotation.customizable.Json;
import org.teasoft.bee.osql.type.TypeHandler;
import org.teasoft.bee.sharding.ShardingSortStruct;
import org.teasoft.honey.osql.type.TypeHandlerRegistry;
import org.teasoft.honey.osql.util.AnnoUtil;
import org.teasoft.honey.sharding.ShardingUtil;

/**
 * Transform ResultSet.
 * @author Kingstar
 * @since  1.1
 */
public class TransformResultSet {
	
	private TransformResultSet() {}

	@SuppressWarnings("rawtypes")
	public static JsonResultWrap toJson(ResultSet rs,Class entityClass) throws SQLException {
		StringBuffer json = new StringBuffer("");
		ResultSetMetaData rmeta = rs.getMetaData();
		int columnCount = rmeta.getColumnCount();
		boolean ignoreNull = HoneyConfig.getHoneyConfig().selectJson_ignoreNull;
		String temp = "";

		boolean dateWithMillisecond = HoneyConfig.getHoneyConfig().selectJson_dateWithMillisecond;
		boolean timeWithMillisecond = HoneyConfig.getHoneyConfig().selectJson_timeWithMillisecond;
		boolean timestampWithMillisecond = HoneyConfig.getHoneyConfig().selectJson_timestampWithMillisecond;
		boolean longToString = HoneyConfig.getHoneyConfig().selectJson_longToString;
        int rowCount=0;
        boolean isJsonString=false;
        Field currField=null;
        String fieldName="";
        String fieldTypeName="";
        
		while (rs.next()) {
			rowCount++;
			json.append(",{");
			for (int i = 1; i <= columnCount; i++) { //1..n
				if (rs.getString(i) == null && ignoreNull) {
					continue;
				}
				
				isJsonString=false;
//				fieldName=_toFieldName(rmeta.getColumnName(i),entityClass);
				fieldName=_toFieldName(rmeta.getColumnLabel(i),entityClass);//V2.1.8
				fieldTypeName=HoneyUtil.getFieldType(rmeta.getColumnTypeName(i));
				
				json.append("\"");
				json.append(fieldName);
				json.append("\":");

				if (rs.getString(i) != null) {
					
					temp=rs.getString(i);
					
					//Json类型,不用再转换引号
					if ("JSON".equals(fieldTypeName) ) {
						isJsonString=true;
					}else if(entityClass!=null){
						try {
							currField = HoneyUtil.getField(entityClass, fieldName);
							isJsonString=isJoson(currField);
						} catch (NoSuchFieldException e) {
							//ignore
						}
					}
					
					if(isJsonString) {
						json.append(temp);
					}else if ("String".equals(fieldTypeName)) { // equals改为不区分大小写  其它几个也是.  不需要,Map中值是这种命名风格的
						json.append("\"");
						
						temp=temp.replace("\\", "\\\\"); //1
						temp=temp.replace("\"", "\\\""); //2
						
						json.append(temp);
						json.append("\"");
					} else if ("Date".equals(fieldTypeName)) {
						if (dateWithMillisecond) {
							json.append(rs.getDate(i).getTime());
						} else {
							try {
//								temp = rs.getString(i);
								Long.valueOf(temp); //test value
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
								Long.valueOf(temp); //test value
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
								Long.valueOf(temp); //test value
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

				if (i != columnCount) json.append(",");  //fixed bug.  if last field is null and ignore.
			} //one record end
			if(json.toString().endsWith(",")) json.deleteCharAt(json.length()-1); //fix bug
			json.append("}");
		}//array end
		if (json.length() > 0) {
			json.deleteCharAt(0);
		}
		json.insert(0, "[");
		json.append("]");
		
		JsonResultWrap wrap =new JsonResultWrap();
		wrap.setResultJson(json.toString());
		wrap.setRowCount(rowCount);
		
		return wrap;
	}
	
	@SuppressWarnings("rawtypes")
	private static String _toFieldName(String columnName,Class entityClass) {
		return NameTranslateHandle.toFieldName(columnName,entityClass);
	}

	public static List<String[]> toStringsList(ResultSet rs) throws SQLException {
		List<String[]> list = new ArrayList<>();
		ResultSetMetaData rmeta = rs.getMetaData();
		int columnCount = rmeta.getColumnCount();
		boolean nullToEmptyString = HoneyConfig.getHoneyConfig().returnStringList_nullToEmptyString;
		String str[] = null;
		boolean firstRow=true;
		while (rs.next()) {
			str = new String[columnCount];
			for (int i = 0; i < columnCount; i++) {
				if (nullToEmptyString && rs.getString(i + 1) == null) {
					str[i] = "";
				} else {
					str[i] = rs.getString(i + 1);
				}
				if(firstRow) { //2.0
					firstRow=false;
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
				fieldName = _toFieldName(rmeta.getColumnLabel(i + 1), null); //V2.1.8
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
		
	public static List<Map<String,Object>> toMapList(ResultSet rs) throws SQLException {
		List<Map<String,Object>> list = new ArrayList<>();
		ResultSetMetaData rmeta = rs.getMetaData();
		int columnCount = rmeta.getColumnCount();
		Map<String,Object> rowMap=null;
		while (rs.next()) {
//			rowMap=new HashMap<>();
			rowMap=new LinkedHashMap<>(); //2021-06-13
			for (int i = 1; i <= columnCount; i++) {
//				rowMap.put(_toFieldName(rmeta.getColumnName(i),null), rs.getObject(i)); //ignore Column annotation
				//V2.1.8
				rowMap.put(_toFieldName(rmeta.getColumnLabel(i),null), rs.getObject(i)); //ignore Column annotation
			}
			list.add(rowMap);
		}
		return list;
	}
	
	//检测是否有Json注解
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
	
	private static Object _getObjectByindex(ResultSet rs,Field field, int index) throws SQLException{
		return HoneyUtil.getResultObjectByIndex(rs, field.getType().getName(),index);
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
				name = _toFieldName(rmeta.getColumnLabel(i + 1), clazz); // fixed bug,V2.1.8. 获取用于打印输出和显示的指定列的建议标题。建议标题通常由 SQL AS
																			// 子句来指定。如果未指定 SQL AS，则从 getColumnLabel 返回的值将和
																			// getColumnName 方法返回的值相同。
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

}
