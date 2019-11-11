package org.teasoft.honey.osql.dialect.oracle;

import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.honey.osql.core.HoneyUtil;

/**
 * @author Kingstar
 * @since  1.0
 */
public class OracleFeature implements DbFeature {

	@Override
	public String toPageSql(String sql, int start, int size) {
		return getLimitSql(sql, true, start, size);
	}

	@Override
	public String toPageSql(String sql, int size) {
		return getLimitSql(sql, false, -1, size);
	}

	private String getLimitSql(String sql, boolean isStartSize, int start, int size) {
		sql=HoneyUtil.deleteLastSemicolon(sql);
		String forUpdateClause = null;
		boolean isForUpdate = false;
		final int forUpdateIndex = sql.toLowerCase().lastIndexOf("for update");
		if (forUpdateIndex > -1) {
			forUpdateClause = sql.substring(forUpdateIndex);
			sql = sql.substring(0, forUpdateIndex - 1);
			isForUpdate = true;
		}

		StringBuilder pageSql = new StringBuilder(sql.length() + 130);
		if (isStartSize) {
			pageSql.append("select * from ( select table_.*, rownum rn_ from ( "); 
		} else {
			pageSql.append("select * from ( ");
		}
		
		pageSql.append(sql);
		
		if (isStartSize) {
			pageSql.append(" ) table_ where rownum < " + (start + size) + ") where rn_ >= " + start);
		} else {
			pageSql.append(" ) where rownum <= " + size);
		}

		if (isForUpdate) {
			pageSql.append(" ");
			pageSql.append(forUpdateClause);
		}

		return pageSql.toString();
	}

}
