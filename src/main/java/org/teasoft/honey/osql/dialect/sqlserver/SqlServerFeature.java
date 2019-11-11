package org.teasoft.honey.osql.dialect.sqlserver;

import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.honey.osql.core.HoneyUtil;

/**
 * @author Kingstar
 * @since  1.0
 */
public class SqlServerFeature implements DbFeature {

		public String toPageSql(String sql, int start, int size) {
			//TODO
			return null;
		}
		
		public String toPageSql(String sql, int size) {
			sql=HoneyUtil.deleteLastSemicolon(sql);
			sql = "top "+size+" "+sql;
			return sql;
		}
}
