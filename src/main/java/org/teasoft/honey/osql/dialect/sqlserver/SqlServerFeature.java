package org.teasoft.honey.osql.dialect.sqlserver;

import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.NotSupportedException;
import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.K;

/**
 * @author Kingstar
 * @since  1.0
 */
public class SqlServerFeature implements DbFeature {

	public String toPageSql(String sql, int start, int size) {
		if (start > 1) throw new NotSupportedException("select result did not support paging skip");
        //sql server 是从1开始数. start 为0或1, 相当于只取size
		return toPageSql(sql, size);
	}

	public String toPageSql(String sql, int size) {
		sql=HoneyUtil.deleteLastSemicolon(sql);

		int index1=sql.toLowerCase().indexOf("select");
		int index2=sql.toLowerCase().indexOf("select distinct");
		int insertIndex=index1 + (index2 == index1 ? 15 : 6);

		StringBuilder sb=new StringBuilder(sql.length() + 6 + (size + "").length()).append(sql);
		sb.insert(insertIndex, " "+K.top+" " + size);
		
		return sb.toString();
	}
}
