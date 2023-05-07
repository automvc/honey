package org.teasoft.honey.osql.dialect;

import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.honey.osql.core.HoneyUtil;

public class OffsetFetchPaging implements DbFeature {

	private static final String PagingLower = " offset #1# rows fetch next #2# rows"; // skip, size
	private static final String PagingUpper = " OFFSET #1# ROWS FETCH NEXT #2# ROWS"; // skip, size

	@Override
	public String toPageSql(String sql, int start, int size) {
		String paging = isUpper() ? PagingUpper : PagingLower;
		sql = HoneyUtil.deleteLastSemicolon(sql);

		String forUpdateClause = null;
		boolean isForUpdate = false;
		final int forUpdateIndex = sql.toLowerCase().lastIndexOf("for update");
		if (forUpdateIndex > -1) {
			forUpdateClause = sql.substring(forUpdateIndex);
			sql = sql.substring(0, forUpdateIndex - 1);
			isForUpdate = true;
		}

		if (HoneyUtil.isRegPagePlaceholder()) {
			paging = paging.replace("#1#", "?").replace("#2#", "?");
			int array[] = new int[2];
			array[0] = start;
			array[1] = size;
			HoneyUtil.regPageNumArray(array);
		} else {
			paging = paging.replace("#1#", start + "").replace("#2#", size + "");
		}

		sql += paging;

		if (isForUpdate) {
			sql += " " + forUpdateClause;
		}

		return sql;
	}

	@Override
	public String toPageSql(String sql, int size) {
		return toPageSql(sql, 0, size);
	}

	private boolean isUpper() {
		return HoneyUtil.isSqlKeyWordUpper();
	}

}
