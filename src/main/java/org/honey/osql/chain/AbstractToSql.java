/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.honey.osql.chain;

import org.bee.osql.chain.ToSql;

/**
 * @author Kingstar
 * @since  1.3
 */
public abstract class AbstractToSql implements ToSql{
	
	protected StringBuffer sql = new StringBuffer();
	
	public String toSQL() {
		return toSQL(false);
	}

	public String toSQL(boolean noSemicolon) {
		if (noSemicolon){
			return sql.toString();
		}else{
			return sql.toString()+";";
		}
	}

}
