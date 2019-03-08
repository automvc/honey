package org.teasoft.honey.osql.dialect.mysql;

import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.honey.osql.core.HoneyUtil;

/**
 * @author Kingstar
 * @since  1.0
 */
public class MySqlFeature implements DbFeature {

	public String toPageSql(String sql, int start, int size) {
//		sql=sql.replace(";", ""); //去掉原来有的分号   只能去掉最后一个
		sql=HoneyUtil.deleteLastSemicolon(sql);
		
		String limitStament = " limit " + start + "," + size;
		sql += limitStament;
		return sql;
	}
	
	public String toPageSql(String sql, int size) {
//		sql=sql.replace(";", ""); //去掉原来有的分号
		sql=HoneyUtil.deleteLastSemicolon(sql);
		String limitStament = " limit 0," + size;
		sql += limitStament;
		return sql;
	}
}
