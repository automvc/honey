package org.honey.osql.dialect.sqlserver;

import org.bee.osql.dialect.DbFeature;

/**
 * @author Kingstar
 * @since  1.0
 */
public class SqlServerFeature implements DbFeature {

		public String toPageSql(String sql, int from, int size) {
			//TODO
			return null;
		}
		
		public String toPageSql(String sql, int size) {
			sql=sql.replace(";", ""); //去掉原来有的分号,如果有
			sql = "top "+size+" "+sql+" ;";
			return sql;
		}
}
