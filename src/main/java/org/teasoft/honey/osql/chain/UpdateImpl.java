/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.chain;

import org.teasoft.bee.osql.Op;
import org.teasoft.bee.osql.chain.Update;
import org.teasoft.bee.osql.exception.BeeIllegalSQLException;
import org.teasoft.honey.osql.core.Check;
import org.teasoft.honey.osql.core.K;
import org.teasoft.honey.osql.util.NameCheckUtil;


/**
 * @author Kingstar
 * @since  1.3
 */
public class UpdateImpl extends AbstractToSql implements Update {

	private boolean isStartWhere = true;
	private boolean isAddAnd = false;
	private boolean isStartTable = true;
	private boolean isStartSet = true;
	
	//for where condition
	private static final String L_PARENTHESES = "(";
	private static final String R_PARENTHESES = ")";
	private static final String COMMA = ",";
	private static final String SPACE = " ";
	private static final String AND = " "+K.and+" ";

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
	

	//select , update also need use	
	 //Condition<<============= 
		@Override
		public Update lParentheses() {
			if (isAddAnd) sql.append(AND);
			isAddAnd = false;
			sql.append(L_PARENTHESES);
			return this;
		}

		@Override
		public Update rParentheses() {
			sql.append(R_PARENTHESES);
			isAddAnd = true;
			return this;
		}
		
		@Override
		public Update where() {
//			sql.append(" where ");
			sql.append(SPACE).append(K.where).append(SPACE);
			isStartWhere = false;

			return this;
		}

		@Override
		public Update where(String expression) {
			checkExpression(expression);
			if (isStartWhere) {
				sql.append(SPACE).append(K.where).append(SPACE);
				sql.append(expression);
				isStartWhere = false;
				isAddAnd = true; //fix on 2020-01-13
			} else {
				if (isAddAnd) sql.append(AND);
				sql.append(expression);
				isAddAnd = true;
			}

			return this;
		}

		@Override
		public Update op(String field, Op opType, String value) {
			checkField(field);
			if (opType == Op.in) return in(field, value);
			if (opType == Op.notIn) return notIn(field, value);

			if (isAddAnd) sql.append(AND);

			sql.append(field);
			sql.append(opType.getOperator());
			sql.append("'");
			sql.append(value);
			sql.append("'");
			isAddAnd = true;
			return this;
		}

		@Override
		public Update op(String field, Op opType, Number value) {
			checkField(field);
			if (opType == Op.in) return in(field, value);
			if (opType == Op.notIn) return notIn(field, value);

			if (isAddAnd) sql.append(AND);
			sql.append(field);
			sql.append(opType.getOperator());
			sql.append(value);
			isAddAnd = true;
			return this;
		}
		
		@Override
		public Update op(String field, String value) {
			checkField(field);
			return op(field, Op.eq, value);
		}
		
		@Override
		public Update op(String field, Number value) {
			checkField(field);
			return op(field, Op.eq, value);
		}

		/**
		 * 默认自动加and.default will automatically add and.
		 * @return a reference to this object.
		 */
		@Override
		public Update and() {
			sql.append(AND);
			isAddAnd = false;
			return this;
		}

		@Override
		public Update or() {
			sql.append(SPACE).append(K.or).append(SPACE);
			isAddAnd = false;
			return this;
		}
		
		@Override
		public Update in(String field, Number... valueList) {
			checkField(field);
			return inOrNotIn(field, K.in, valueList);
		}

		public Update notIn(String field, Number... valueList) {
			checkField(field);
			return inOrNotIn(field, K.notIn, valueList);
		}

		private Update inOrNotIn(String field, String op, Number... valueList) {
			checkField(field);
			if (isAddAnd) sql.append(AND);
			String value = "";
			for (int i = 0; i < valueList.length; i++) {
				if (i == 0)
					value += valueList[i];
				else
					value += "," + valueList[i];
			}
			sql.append(field + " " + op + " (" + value + ")"); // eg: in (99,18)
			return this;
		}

		@Override
		public Update in(String field, String valueList) {
			return inOrNotIn(field, K.in, valueList);
		}

		@Override
		public Update notIn(String field, String valueList) {
			return inOrNotIn(field, K.notIn, valueList);
		}

		private Update inOrNotIn(String field, String op, String valueList) {
			checkField(field);
			if (isAddAnd) sql.append(AND);
			valueList = valueList.replace(",", "','");
			sql.append(field + " " + op + " ('" + valueList + "')"); // in ('client01','bee')
			return this;
		}
		
		@Override
		public Update between(String field, Number low, Number high) {
			checkField(field);
			if (isAddAnd) sql.append(AND);
			sql.append(field);
			sql.append(SPACE).append(K.between).append(SPACE);
			sql.append(low);
			sql.append(AND);
			sql.append(high);
			isAddAnd = true;

			return this;
		}

		@Override
		public Update notBetween(String field, Number low, Number high) {
			checkField(field);
			if (isAddAnd) sql.append(AND);
			sql.append(field);
			sql.append(SPACE).append(K.notBetween).append(SPACE);
			sql.append(low);
			sql.append(AND);
			sql.append(high);
			isAddAnd = true;

			return this;
		}

		@Override
		public Update isNull(String field) {
			checkField(field);
			if (isAddAnd) sql.append(AND);
//			sql.append(field + " is null ");
			sql.append(field);
			sql.append(SPACE).append(K.isNull).append(SPACE);
			return this;
		}

		@Override
		public Update isNotNull(String field) {
			checkField(field);
			if (isAddAnd) sql.append(AND);
//			sql.append(field + " is not null ");
			sql.append(field);
			sql.append(SPACE).append(K.isNotNull).append(SPACE);
			return this;
		}
		
		private void checkField(String field){
			NameCheckUtil.checkName(field);
		}
		
		private void checkExpression(String expression){
			if(Check.isNotValidExpression(expression)) {
				throw new BeeIllegalSQLException("The expression: '"+expression+ "' is invalid!");
			}
		}
		
		 //=============>>
}
