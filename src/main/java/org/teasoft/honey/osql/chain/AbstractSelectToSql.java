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
public class AbstractSelectToSql extends WhereImpl<Select>{
	protected int start;
	protected int size;
	
	public String toSQL() {
//		return toSQL(true);  //oracle用jdbc不允许有分号
		
		String sql0=toSQL(true); //oracle用jdbc不允许有分号
		if(isUsePlaceholder()) setContext(sql0);  //但是使用pre的时候，会把它冲了; 无参数时，已不会。
		return sql0;
	}

	public String toSQL(boolean noSemicolon) {
		if (noSemicolon){
			return addPage(sql.toString());
		}else{
			return addPage(sql.toString())+";";
		}
	}
	private DbFeature getDbFeature() {
		return BeeFactory.getHoneyFactory().getDbFeature();
	}
	private String addPage(String sqlStr){
		if (this.start != 0 && size != 0) {
			sqlStr= getDbFeature().toPageSql(sqlStr, start,size);
		}else if (size != 0){
			sqlStr= getDbFeature().toPageSql(sqlStr.toString(), size);
		}
		return sqlStr;
	}
}
