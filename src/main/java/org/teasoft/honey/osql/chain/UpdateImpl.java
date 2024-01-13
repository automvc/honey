/*
 * Copyright 2020-2024 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.chain;

import org.teasoft.bee.osql.chain.Update;
import org.teasoft.honey.osql.core.K;

/**
 * @author Kingstar
 * @since  1.3
 * @since  2.4.0
 */
public class UpdateImpl extends WhereImpl<Update> implements Update {

	
	private static final String MUL = "*";
	private static final String ADD = "+";
	private boolean isStartTable = true;
	private boolean isStartSet = true;
	
	private static final String COMMA = ",";
	private static final String SPACE = " ";

	public UpdateImpl() {

	}

	@Override
	public Update update(String table) {
		checkExpression(table);
		_appendTable(table);
		if (isStartTable) {
			sql.append(K.update).append(SPACE);
			sql.append(table);
			isStartTable = false;
		} else {
			sql.append(COMMA);
			sql.append(table);
		}
		return this;
	}

	@Override
	public Update set(String field, String value) {

		checkExpression(field);
		
		adjustComma();
		sql.append(field);
		sql.append("=");
		
		if (isUsePlaceholder()) {
			sql.append("?");
		    addValue(value);
		} else {
			sql.append("'");
			sql.append(value);
			sql.append("'");
		}
		
		
		return this;
	}
	
	@Override
	public Update set(String field, Number value) {
		checkExpression(field);
		
		adjustComma();
		sql.append(field);
		sql.append("=");
		if (isUsePlaceholder()) {
			sql.append("?");
			addValue(value);
		} else {
			sql.append(value);
		}
		return this;
	}
	
	@Override
	public Update setAdd(String field, Number num) {
		return _set0(field, num, ADD);
	}

	@Override
	public Update setMultiply(String field, Number num) {
		return _set0(field, num, MUL);
	}
	
	
	private Update _set0(String field, Number value, String operator) {
		checkExpression(field);
		
		adjustComma();
		sql.append(field);
		sql.append("=");
		sql.append(field);
		sql.append(operator);
		
		if (isUsePlaceholder()) {
			sql.append("?");
			addValue(value);
		} else {
			sql.append(value);
		}
		return this;
	}

	/**
	 * update table_name set field=field + otherFieldName;
	 */
	@Override
	public Update setAdd(String field, String otherFieldName) {
		return _set1(field, otherFieldName, ADD);
	}

	/**
	 * eg:update table_name set field=field * otherFieldName;
	 */
	@Override
	public Update setMultiply(String field, String otherFieldName) {
		return _set1(field, otherFieldName, MUL);
	}
	
//	update table_name set f1=f1+alph;
	private Update _set1(String field, String otherFieldName, String operator) {
		checkExpression(field);
		checkExpression(otherFieldName);
		
		adjustComma();
		sql.append(field);
		sql.append("=");
		sql.append(field);
		sql.append(operator);
		sql.append(otherFieldName);
		
		return this;
	}

	/**
	 * eg:update table_name set field1=field2;
	 */
	@Override
	public Update setWithField(String field1, String field2) {
		checkExpression(field1);
		checkExpression(field2);
		
		adjustComma();
		sql.append(field1);
		sql.append("=");
		sql.append(field2);

		return this;
	}
	
	

	@Override
	public Update setNull(String fieldName) {
		checkExpression(fieldName);
		
		adjustComma();
		sql.append(fieldName);
		sql.append("=");
		sql.append(K.Null);

		return this;
	}

	private void _appendTable(String table) {
		super.appendTable(table);
	}
	
	private void adjustComma() {
		if (!isStartSet)
			sql.append(" , ");
		if (isStartSet) {
			sql.append(" set ");
			isStartSet = false;
		}
	}
}
