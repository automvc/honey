/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.chain;

import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.honey.osql.core.BeeFactory;

/**
 * @author Kingstar
 * @since  1.3
 */
public class AbstractSelectToSql extends AbstractToSql{
	protected int start;
	protected int size;
	
	public String toSQL() {
		return toSQL(false);
	}

	public String toSQL(boolean noSemicolon) {
		if (noSemicolon){
			return addPage(sql.toString());
		}else{
			return addPage(sql.toString())+";";
		}
	}
	private DbFeature dbFeature = BeeFactory.getHoneyFactory().getDbDialect();
	private String addPage(String sql){
		if (this.start != 0 && size != 0) {
			sql= dbFeature.toPageSql(sql, start,size);
		}else if (size != 0){
			sql= dbFeature.toPageSql(sql.toString(), size);
		}
		return sql;
	}
}
