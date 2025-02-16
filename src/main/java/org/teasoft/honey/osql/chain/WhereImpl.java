/*
 * Copyright 2020-2024 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.chain;

import org.teasoft.bee.osql.BeeException;
import org.teasoft.bee.osql.Op;
import org.teasoft.bee.osql.chain.Where;
import org.teasoft.bee.osql.exception.BeeErrorNameException;
import org.teasoft.honey.osql.core.AbstractToSqlForChain;
import org.teasoft.honey.osql.core.ConditionHelper;
import org.teasoft.honey.osql.core.K;
import org.teasoft.honey.osql.util.NameCheckUtil;

/**
 * @author Kingstar
 * @since  2.4.0
 */

public class WhereImpl<T> extends AbstractToSqlForChain implements Where<T> {

	// for where condition
	private static final String L_PARENTHESES = "(";
	private static final String R_PARENTHESES = ")";
	private static final String SPACE = " ";
	private static final String AND = " " + K.and + " ";

	private boolean isStartWhere = true;

	protected boolean isAddAnd = false;

	// select , update,delete also need use
	// Condition<<=============
	@Override
	public T lParentheses() {
		if (isAddAnd) sql.append(AND);
		isAddAnd = false;
		sql.append(L_PARENTHESES);
		return self();
	}

	@Override
	public T rParentheses() {
		sql.append(R_PARENTHESES);
		isAddAnd = true;
		return self();
	}

	@Override
	public T where() {
		sql.append(SPACE).append(K.where).append(SPACE);
		isStartWhere = false;

		return self();
	}

	@Override
	public T where(String expression) {
		checkExpression(expression);
		if (isStartWhere) {
			sql.append(SPACE).append(K.where).append(SPACE);
			sql.append(expression);
			isStartWhere = false;
			isAddAnd = true; // fix on 2020-01-13
		} else {
			if (isAddAnd) sql.append(AND);
			sql.append(expression);
			isAddAnd = true;
		}

		return self();
	}

	@SuppressWarnings("unchecked")
	private T self() {
		return (T) this;
	}

	@Override
	public T op(String field, Op opType, Object value) {
		checkField(field);

		if (value instanceof String) return _opWithString(field, opType, (String) value);
		if (value instanceof Number) return _opWithNumber(field, opType, (Number) value);

		if (opType == Op.in || opType == Op.notIn)
			return inOrNotInUsePlaceholder(value); // 2.x
		else {
			String msg = "";
			if (value != null) {
				msg = "the value(" + value.toString() + ") of type:" + value.getClass().getName() + " not support!";
			} else {
				msg = "the value of type not support!";
			}
			throw new BeeException(msg);
			// do nothing
//				return self();
		}
	}

	private T _opWithString(String field, Op opType, String value) {
		checkField(field);
		if (opType == Op.in) return in(field, value);
		if (opType == Op.notIn) return notIn(field, value);

		if (Op.like == opType || Op.notLike == opType || Op.likeLeft == opType || Op.likeRight == opType
				|| Op.likeLeftRight == opType)
			value = ConditionHelper.processLike(opType, value);

		if (isAddAnd) sql.append(AND);

		sql.append(field);
		sql.append(opType.getOperator());
		if (isUsePlaceholder()) {
			sql.append("?");
			addValue(value);
		} else {
			sql.append("'");
			sql.append(value);
			sql.append("'");
		}
		isAddAnd = true;
		return self();
	}

	private T _opWithNumber(String field, Op opType, Number value) {
		checkField(field);
		if (opType == Op.in) return in(field, value);
		if (opType == Op.notIn) return notIn(field, value);

		if (isAddAnd) sql.append(AND);
		sql.append(field);
		sql.append(opType.getOperator());
		if (isUsePlaceholder()) {
			sql.append("?");
			addValue(value);
		} else
			sql.append(value);
		isAddAnd = true;
		return self();
	}

	@Override
	public T opWithField(String field1, Op opType, String field2) {
		checkField(field1);
		checkField(field2);

		if (isAddAnd) sql.append(AND);
		sql.append(field1);
		sql.append(opType.getOperator());
		sql.append(field2);
		isAddAnd = true;
		return self();
	}

	@Override
	public T op(String field, String value) {
		checkField(field);
		return op(field, Op.eq, value);
	}

	@Override
	public T op(String field, Number value) {
		checkField(field);
		return op(field, Op.eq, value);
	}

	/**
	 * 默认自动加and.default will automatically add and.
	 * @return a reference to this object.
	 */
	@Override
	public T and() {
		sql.append(AND);
		isAddAnd = false;
		return self();
	}

	@Override
	public T or() {
		sql.append(SPACE).append(K.or).append(SPACE);
		isAddAnd = false;
		return self();
	}

	@Override
	public T not() {
//			sql.append(SPACE).append(K.not).append(SPACE);
		sql.append(SPACE).append("!").append(SPACE);
		return self();
	}

	@Override
	public T in(String field, Number... valueList) {
		checkField(field);
		return inOrNotIn(field, K.in, valueList);
	}

	public T notIn(String field, Number... valueList) {
		checkField(field);
		return inOrNotIn(field, K.notIn, valueList);
	}

//		如何增加list支持？？
	private T inOrNotIn(String field, String op, Number... valueList) {
		checkFieldOrExpression(field);
		if (isAddAnd) sql.append(AND);
		sql.append(field + " " + op);

		if (isUsePlaceholder()) return inOrNotInUsePlaceholder(valueList); // 2.x

		String value = "";
		for (int i = 0; i < valueList.length; i++) {
			if (i == 0)
				value += valueList[i];
			else
				value += "," + valueList[i];
		}
		sql.append(" (" + value + ")"); // eg: in (99,18)
		return self();
	}

	@Override
	public T in(String field, String valueList) {
		return inOrNotIn(field, K.in, valueList);
	}

	@Override
	public T notIn(String field, String valueList) {
		return inOrNotIn(field, K.notIn, valueList);
	}

	private T inOrNotIn(String field, String op, String valueList) {
		checkField(field);
		if (isAddAnd) sql.append(AND);
		sql.append(field + " " + op);
		if (isUsePlaceholder()) return inOrNotInUsePlaceholder(valueList); // 2.x

		valueList = valueList.replace(",", "','");
		sql.append(" ('" + valueList + "')"); // eg: in ('client01','bee')
		return self();
	}

	@Override
	public T between(String field, Number low, Number high) {
		checkField(field);
		if (isAddAnd) sql.append(AND);
		sql.append(field);
		sql.append(SPACE).append(K.between).append(SPACE);
		if (isUsePlaceholder()) {
			sql.append("?");
			addValue(low);
		} else
			sql.append(low);
		sql.append(AND);
		if (isUsePlaceholder()) {
			sql.append("?");
			addValue(high);
		} else
			sql.append(high);
		isAddAnd = true;

		return self();
	}

	@Override
	public T notBetween(String field, Number low, Number high) {
		checkField(field);
		if (isAddAnd) sql.append(AND);
		sql.append(field);
		sql.append(SPACE).append(K.notBetween).append(SPACE);
		if (isUsePlaceholder()) {
			sql.append("?");
			addValue(low);
		} else
			sql.append(low);
		sql.append(AND);
		if (isUsePlaceholder()) {
			sql.append("?");
			addValue(high);
		} else
			sql.append(high);
		isAddAnd = true;

		return self();
	}

	@Override
	public T isNull(String field) {
		checkField(field);
		if (isAddAnd) sql.append(AND);
//			sql.append(field + " is null ");
		sql.append(field);
		sql.append(SPACE).append(K.isNull).append(SPACE);
		return self();
	}

	@Override
	public T isNotNull(String field) {
		checkField(field);
		if (isAddAnd) sql.append(AND);
//			sql.append(field + " is not null ");
		sql.append(field);
		sql.append(SPACE).append(K.isNotNull).append(SPACE);
		return self();
	}

	protected void checkFieldOrExpression(String field) {
		if (NameCheckUtil.isIllegal(field)) {
			throw new BeeErrorNameException("The field: '" + field + "' is illegal!");
		}
	}

	private void checkField(String field) {
		NameCheckUtil.checkName(field);
	}

	// =============>>

	private T inOrNotInUsePlaceholder(Object value) {
		ConditionHelper.processIn(sql, getPvList(), value);
		return self();
	}

}
