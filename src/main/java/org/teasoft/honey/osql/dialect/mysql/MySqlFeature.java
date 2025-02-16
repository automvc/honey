package org.teasoft.honey.osql.dialect.mysql;

import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.K;

/**
 * @author Kingstar
 * @since  1.0
 */
public class MySqlFeature implements DbFeature {

	public String toPageSql(String sql, int start, int size) {
		sql = HoneyUtil.deleteLastSemicolon(sql);

		String forUpdateClause = null;
		boolean isForUpdate = false;
		final int forUpdateIndex = sql.toLowerCase().lastIndexOf("for update");
		if (forUpdateIndex > -1) {
			forUpdateClause = sql.substring(forUpdateIndex);
			sql = sql.substring(0, forUpdateIndex - 1);
			isForUpdate = true;
		}
		String limitStament = "";
		if (HoneyUtil.isRegPagePlaceholder()) {
			int array[] = new int[2];
			limitStament = " " + K.limit + " ?,?";
			array[0] = start;
			array[1] = size;
			HoneyUtil.regPageNumArray(array);
		} else {
			limitStament = " " + K.limit + " " + start + "," + size;
		}

		sql += limitStament;

		if (isForUpdate) {
			sql += " " + forUpdateClause;
		}

		return sql;
	}

	public String toPageSql(String sql, int size) {
		return toPageSql(sql, 0, size);
	}
}
