/*
 * Copyright 2013-2018 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.honey.osql.core;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
		while (rs.next()) {
			json.append(",{");
			for (int i = 1; i <= columnCount; i++) { //1..n
				json.append("\"");
				json.append(rmeta.getColumnName(i));
				json.append("\":");

				if (rs.getString(i) != null && "String".equals(HoneyUtil.getFieldType(rmeta.getColumnTypeName(i)))) {
					json.append("\"");
					json.append(rs.getString(i));
					json.append("\"");
				} else {
					json.append(rs.getString(i));
				}

				if (i != columnCount) json.append(",");
			}
			json.append("}");
		}
		json.deleteCharAt(0);
		json.insert(0, "[");
		json.append("]");

		return json;
	}

	public static List<String[]> toStringsList(ResultSet rs) throws SQLException {
		List<String[]> list = new ArrayList<String[]>();
		ResultSetMetaData rmeta = rs.getMetaData();
		int columnCount = rmeta.getColumnCount();
		String str[] = null;
		while (rs.next()) {
			str = new String[columnCount];
			for (int i = 0; i < columnCount; i++) {
				str[i] = rs.getString(i + 1);
			}
			list.add(str);
		}
		return list;
	}

}
