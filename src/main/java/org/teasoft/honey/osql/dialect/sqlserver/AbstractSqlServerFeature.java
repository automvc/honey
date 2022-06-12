/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.dialect.sqlserver;

import org.teasoft.honey.osql.core.HoneyUtil;
import org.teasoft.honey.osql.core.K;

/**
 * @author Kingstar
 * @since  1.17
 */
public class AbstractSqlServerFeature {
	
	public String toPageSql(String sql, int size) {
		sql=HoneyUtil.deleteLastSemicolon(sql);

		int index1=sql.toLowerCase().indexOf("select");
		int index2=sql.toLowerCase().indexOf("select distinct");
		int insertIndex=index1 + (index2 == index1 ? 15 : 6);

		StringBuilder sb=new StringBuilder(sql.length() + 6 + (size + "").length()).append(sql);
		if(index2 != index1) sb.insert(insertIndex, " " + K.top + " " + size );
		else                 sb.insert(insertIndex, " " + K.top + " " + size + " ");
		
		return sb.toString();
	}
	
}
