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

import org.teasoft.honey.osql.util.AnnoUtil;

/**
 * Transform ResultSet.
 * @author Kingstar
 * @since  1.1
 */
public class TransformResultSet {
	
	private TransformResultSet() {}

	@SuppressWarnings("rawtypes")
	public static StringBuffer toJson(ResultSet rs,Class entityClass) throws SQLException {
		StringBuffer json = new StringBuffer("");
		ResultSetMetaData rmeta = rs.getMetaData();
		int columnCount = rmeta.getColumnCount();
		boolean ignoreNull = HoneyConfig.getHoneyConfig().selectJson_ignoreNull;
		String temp = "";

		boolean dateWithMillisecond = HoneyConfig.getHoneyConfig().selectJson_dateWithMillisecond;
		boolean timeWithMillisecond = HoneyConfig.getHoneyConfig().selectJson_timeWithMillisecond;
		boolean timestampWithMillisecond = HoneyConfig.getHoneyConfig().selectJson_timestampWithMillisecond;
		boolean longToString = HoneyConfig.getHoneyConfig().selectJson_longToString;
//        int rowCount=0;
        boolean isJsonString=false;
        Field currField=null;
        String fieldName="";
        String fieldTypeName="";
        
		while (rs.next()) {
//			rowCount++;
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
							currField = entityClass.getDeclaredField(fieldName);
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
		
//		JsonResultWrap wrap =new JsonResultWrap();
//		wrap.setResultJson(json.toString());
//		wrap.setRowCount(rowCount);
		
		return json;
	}
	
	@SuppressWarnings("rawtypes")
	private static String _toFieldName(String columnName,Class entityClass) {
		return NameTranslateHandle.toFieldName(columnName,entityClass);
	}
	
	//检测是否有Json注解
	private static boolean isJoson(Field field) {
		return AnnoUtil.isJson(field);
	}

	public static List<String[]> toStringsList(ResultSet rs) throws SQLException {
		List<String[]> list = new ArrayList<>();
		ResultSetMetaData rmeta = rs.getMetaData();
		int columnCount = rmeta.getColumnCount();
		boolean nullToEmptyString = HoneyConfig.getHoneyConfig().returnStringList_nullToEmptyString;
		String str[] = null;
		while (rs.next()) {
			str = new String[columnCount];
			for (int i = 0; i < columnCount; i++) {
				if (nullToEmptyString && rs.getString(i + 1) == null) {
					str[i] = "";
				} else {
					str[i] = rs.getString(i + 1);
				}
			}
			list.add(str);
		}
		return list;
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

}
