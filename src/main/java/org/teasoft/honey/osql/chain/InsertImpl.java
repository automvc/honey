/*
 * Copyright 2020-2024 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.chain;

import org.teasoft.bee.osql.chain.Insert;
import org.teasoft.honey.osql.core.AbstractToSqlForChain;
import org.teasoft.honey.osql.core.K;

/**
 * @author Kingstar
 * @since  2.4.0
 */

public class InsertImpl extends AbstractToSqlForChain implements Insert {

	private static final String Q_MARK = "?";
	private boolean isStartField = true;
	private static final String L_PARENTHESES = "(";
	private static final String R_PARENTHESES = ")";
	private static final String COMMA = ",";

	private StringBuffer valueSql = new StringBuffer();

	@Override
	public Insert insert(String table) {
		checkExpression(table);
		_appendTable(table);

		sql.append(K.insert).append(K.space).append(K.into).append(K.space);
		sql.append(table);

		return this;
	}

	@Override
	public Insert columnAndValue(String column, Object value) {
		checkExpression(column);
		if (isStartField) {
			isStartField = false;

			sql.append(L_PARENTHESES);
			sql.append(column);
			sql.append(R_PARENTHESES);
		} else {
			sql.deleteCharAt(sql.length() - 1); // delete ')'
			sql.append(COMMA);
			sql.append(column);
			sql.append(R_PARENTHESES);

			valueSql.append(COMMA);
		}

		if (isUsePlaceholder()) {
			valueSql.append(Q_MARK);
			addValue(value);
		} else {
			appendValue(valueSql, value);
		}

		return this;
	}

	@Override
	public String toSQL() {
		StringBuffer tempSql = new StringBuffer(sql);

		sql.append(K.space).append(K.values).append(K.space).append(L_PARENTHESES).append(valueSql).append(R_PARENTHESES);
		String sql0 = super.toSQL();

		sql = tempSql;

		return sql0;
	}

	private void _appendTable(String table) {
		super.appendTable(table);
	}

	private void appendValue(StringBuffer s, Object value) {
		if (value != null && value.getClass() == String.class) {
			s.append("'").append(value).append("'");
		} else {
			s.append(value);
		}
	}
}