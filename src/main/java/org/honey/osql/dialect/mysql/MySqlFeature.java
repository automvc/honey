package org.honey.osql.dialect.mysql;

import org.bee.osql.dialect.DbFeature;

/**
 * @author Kingstar
 * @since  1.0
 */
public class MySqlFeature implements DbFeature {

	public String toFromSql(String sql, int from, int size) {
		sql=sql.replace(";", ""); //去掉原来有的分号
		String limitStament = " limit " + from + "," + size + ";";
		sql += limitStament;
		return sql;
	}
	
	public String toFromSql(String sql, int size) {
		sql=sql.replace(";", ""); //去掉原来有的分号
		String limitStament = " limit 0," + size + ";";
		sql += limitStament;
		return sql;
	}
}
