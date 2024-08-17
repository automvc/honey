/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.chain;

import org.teasoft.bee.osql.chain.Select;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.honey.osql.core.BeeFactory;

/**
 * @author Kingstar
 * @since  1.3
 */
public class AbstractSelectToSql extends WhereImpl<Select> {
	protected Integer start;
//	protected Integer size=100; //默认100
	protected Integer size;
	
	//can not add paing default, or may gen do not support sql,like below.
	//select * from orders where id in (select id from orders limit 0,100) limit 0,100

	// select的要加分页
	public String toSQL(boolean noSemicolon) { // toSQL()也会调用这个方法
		if (noSemicolon) {
			return addPage(sql.toString());
		} else {
			return addPage(sql.toString()) + ";";
		}
	}

	private DbFeature getDbFeature() {
		return BeeFactory.getHoneyFactory().getDbFeature();
	}

	private String addPage(String sqlStr) {
		if (start != null && size != null && this.start >= 0 && size > 0) {
			sqlStr = getDbFeature().toPageSql(sqlStr, start, size);
		} else if (size != null && size > 0) {
			sqlStr = getDbFeature().toPageSql(sqlStr.toString(), size);
		}
		return sqlStr;
	}
}
