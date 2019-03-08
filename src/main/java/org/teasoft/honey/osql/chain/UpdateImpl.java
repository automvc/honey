/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.chain;

import org.teasoft.bee.osql.Op;
import org.teasoft.bee.osql.chain.Update;

/**
 * @author Kingstar
 * @since  1.3
 */
public class UpdateImpl extends AbstractToSql implements Update {

	private static String COMMA = ",";
	
	private boolean isStartWhere = true;
	private boolean isAddAnd = false;
	private boolean isStartTable = true;
	private boolean isStartSet = true;

	public UpdateImpl() {
		sql.append("update ");
	}

	@Override
	public Update update(String table) {
		if (isStartTable) {
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

		if (!isStartSet) sql.append(" , ");
		if (isStartSet)  {
			sql.append(" set ");
			isStartSet=false;
		}

		sql.append(field);
		sql.append("=");
		sql.append("'");
		sql.append(value);
		sql.append("'");
		return this;
	}
	
	@Override
	public Update set(String field, Number value) {

		if (!isStartSet) sql.append(" , ");
		if (isStartSet)  {
			sql.append(" set ");
			isStartSet=false;
		}

		sql.append(field);
		sql.append("=");
		sql.append(value);
		return this;
	}
	
	//Condition<<=============
	public Update where() {
		sql.append(" where ");
		isStartWhere = false;

		return this;
	}

	public Update where(String expression) {
		if (isStartWhere) {
			sql.append(" where ");
			sql.append(expression);
			isStartWhere = false;
		} else {
			if (isAddAnd) sql.append(" and ");
			sql.append(expression);
			isAddAnd = true;
		}

		return this;
	}

	public Update op(String field, Op opType, String value) {

		if (isAddAnd) sql.append(" and ");

		sql.append(field);
		sql.append(opType.getOperator());
		sql.append("'");
		sql.append(value);
		sql.append("'");
		isAddAnd = true;
		return this;
	}

	public Update op(String field, Op opType, Number value) {

		if (isAddAnd) sql.append(" and ");
		sql.append(field);
		sql.append(opType.getOperator());
		sql.append(value);
		isAddAnd = true;
		return this;
	}

	/**
	 * 默认自动加 and default will automatically add and
	 * 
	 * @return
	 */
	public Update and() {
		sql.append(" and ");
		isAddAnd = false;
		return this;
	}

	public Update or() {
		sql.append(" or ");
		isAddAnd = false;
		return this;
	}
	//=============>>
}
