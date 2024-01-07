/*
 * Copyright 2016-2023 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.chain;

import org.teasoft.bee.osql.chain.Insert;
import org.teasoft.honey.osql.core.AbstractToSql;
import org.teasoft.honey.osql.core.K;

/**
 * @author Kingstar
 * @since  2.4.0
 */

public class InsertImpl extends AbstractToSql implements Insert {
//	public class InsertImpl extends WhereImpl<Insert> implements Insert {
	
	private static final String QU_MARK = "?";
	private boolean isStartField = true;
	private static final String L_PARENTHESES = "(";
	private static final String R_PARENTHESES = ")";
	private static final String COMMA = ",";
	
	private StringBuffer v = new StringBuffer();
	
	@Override
	public Insert insert(String table) {
		checkExpression(table);
		_appendTable(table);

		sql.append(K.insert).append(K.space).append(K.into).append(K.space);
		sql.append(table);

		return this;
	}

	@Override
	public Insert column(String column) {
//		checkField(column);
		checkExpression(column);
		if (isStartField) {
			sql.append(L_PARENTHESES);
			sql.append(column);
			sql.append(R_PARENTHESES);
			isStartField = false;
			if(isUsePlaceholder()) v.append(QU_MARK);
		} else {
			sql.deleteCharAt(sql.length()-1);
			sql.append(COMMA);
			sql.append(column);
			sql.append(R_PARENTHESES);
			
			if(isUsePlaceholder()) v.append(COMMA).append(QU_MARK);
		}

		return this;
	}
	
	public String toSQL() {

		if (isUsePlaceholder())  //TODO
		   sql.append(K.space).append(K.values).append(K.space).append(L_PARENTHESES).append(v).append(R_PARENTHESES);

		return super.toSQL();
	}
	
	
	private void _appendTable(String table) {
		super.appendTable(table);
	}
}