package org.teasoft.honey.osql.core;

import org.teasoft.bee.osql.exception.SqlNullException;
import org.teasoft.honey.osql.util.PropertiesReader;

/**
 * 用户自定义SQL 管理类.Custom SQL manage class.
 * @author Kingstar
 * @since  1.0
 */
public class CustomSql {

	private static PropertiesReader customSqlProp;

	static {
		customSqlProp = new PropertiesReader("bee.sql.properties");
	}

	private CustomSql() {}

	public static String getCustomSql(String sqlId) {

		String sql = customSqlProp.getValue(sqlId);
		if (sql == null) {
			throw new SqlNullException("The sql statement string get by sqlId:" + sqlId + ", is Null !");
		} else if ("".equals(sql.trim())) {
			throw new SqlNullException("The sql statement string get by sqlId:" + sqlId + ", is empty !");
		}

		return sql;
	}

}
