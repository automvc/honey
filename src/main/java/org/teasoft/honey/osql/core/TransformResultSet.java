/*
 * Copyright 2013-2018 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transform ResultSet
 * @author Kingstar
 * @since  1.1
 */
public class TransformResultSet {

	public static StringBuffer toJson(ResultSet rs) throws SQLException {
		StringBuffer json = new StringBuffer("");
		ResultSetMetaData rmeta = rs.getMetaData();
		int columnCount = rmeta.getColumnCount();
		boolean ignoreNull = HoneyConfig.getHoneyConfig().selectJson_ignoreNull;
		String temp = "";

		boolean dateWithMillisecond = HoneyConfig.getHoneyConfig().selectJson_dateWithMillisecond;
		boolean timeWithMillisecond = HoneyConfig.getHoneyConfig().selectJson_timeWithMillisecond;
		boolean timestampWithMillisecond = HoneyConfig.getHoneyConfig().selectJson_timestampWithMillisecond;

		while (rs.next()) {
			json.append(",{");
			for (int i = 1; i <= columnCount; i++) { //1..n
				if (rs.getString(i) == null && ignoreNull) {
					continue;
				}
				json.append("\"");
//				json.append(rmeta.getColumnName(i));
				json.append(_toFieldName(rmeta.getColumnName(i)));
				json.append("\":");

				if (rs.getString(i) != null) {

					if ("String".equals(HoneyUtil.getFieldType(rmeta.getColumnTypeName(i)))) {
						json.append("\"");
						//json.append(rs.getString(i));
						temp=rs.getString(i);
						temp=temp.replace("\\", "\\\\"); //1
						temp=temp.replace("\"", "\\\""); //2
						
						json.append(temp);
						json.append("\"");
					} else if ("Date".equals(HoneyUtil.getFieldType(rmeta.getColumnTypeName(i)))) {
						if (dateWithMillisecond) {
							json.append(rs.getDate(i).getTime());
						} else {
							try {
								temp = rs.getString(i);
								Long.valueOf(temp); //test value
								json.append(temp);
							} catch (NumberFormatException e) {
								json.append("\"");
								json.append(temp.replace("\"", "\\\""));
								json.append("\"");
							}
						}
					} else if ("Time".equals(HoneyUtil.getFieldType(rmeta.getColumnTypeName(i)))) {
						if (timeWithMillisecond) {
							json.append(rs.getTime(i).getTime());
						} else {
							try {
								temp = rs.getString(i);
								Long.valueOf(temp); //test value
								json.append(temp);
							} catch (NumberFormatException e) {
								json.append("\"");
								json.append(temp.replace("\"", "\\\""));
								json.append("\"");
							}
						}
					} else if ("Timestamp".equals(HoneyUtil.getFieldType(rmeta.getColumnTypeName(i)))) {
						if (timestampWithMillisecond) {
							json.append(rs.getTimestamp(i).getTime());
						} else {
							try {
								temp = rs.getString(i);
								Long.valueOf(temp); //test value
								json.append(temp);
							} catch (NumberFormatException e) {
								json.append("\"");
								json.append(temp.replace("\"", "\\\""));
								json.append("\"");
							}
						}
					} else {
						json.append(rs.getString(i));
					}

				} else {// null
					json.append(rs.getString(i));
				}

				if (i != columnCount) json.append(",");  //bug,  if last field is null and ignore.
			} //one record end
			if(json.toString().endsWith(",")) json.deleteCharAt(json.length()-1); //fix bug
			json.append("}");
		}//array end
		if (json.length() > 0) {
			json.deleteCharAt(0);
		}
		json.insert(0, "[");
		json.append("]");

		return json;
	}
	
	private static String _toFieldName(String columnName) {
		return NameTranslateHandle.toFieldName(columnName);
	}

	public static List<String[]> toStringsList(ResultSet rs) throws SQLException {
		List<String[]> list = new ArrayList<String[]>();
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
				rowMap.put(_toFieldName(rmeta.getColumnName(i)), rs.getObject(i));
			}
			list.add(rowMap);
		}
		return list;
	}

}
