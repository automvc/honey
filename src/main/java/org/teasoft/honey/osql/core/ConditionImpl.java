/*
 * Copyright 2016-2025 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.IncludeType;
import org.teasoft.bee.osql.Op;
import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.osql.Serializer;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;
import org.teasoft.bee.osql.exception.BeeErrorNameException;
import org.teasoft.bee.osql.exception.BeeIllegalParameterException;
import org.teasoft.honey.logging.Logger;
import org.teasoft.honey.osql.util.NameCheckUtil;
import org.teasoft.honey.util.StringUtils;

/**
 * 为面向对象方式操作数据库提供封装的条件.Condition for operate DB with Object Oriented Programming way.
 * @author Kingstar
 * @since  1.6
 * @since  2.5.2
 */
public class ConditionImpl implements Condition {

	private static final long serialVersionUID = 1596710362288L;

	private SuidType suidType;
	private List<Expression> list = new ArrayList<>();
	private Set<String> whereField = new HashSet<>(); // 条件表达式用到的字段
	private IncludeType includeType;

	private String[] selectField;
	private Boolean isForUpdate;

	private List<Expression> updateSetList = new ArrayList<>();
	private Set<String> updatefields = new HashSet<>();// update set 部分用到的字段

	private List<FunExpress> funExpList = new ArrayList<>();

	private List<Expression> onExpList = new ArrayList<>();

	private Map<String, String> orderByMap = new LinkedHashMap<>();// V1.17 用于sql server分页 ; 2.0 用于 分片后排序

	private boolean isStartGroupBy = true;
	private boolean isStartHaving = true;
	private boolean isStartOrderBy = true;

	private static final String COMMA = ",";

	private Integer start;
	private Integer size;
	private static final String START_GREAT_EQ_0 = StringConst.START_GREAT_EQ_0;
	private static final String SIZE_GREAT_0 = StringConst.SIZE_GREAT_0;

	private Boolean hasGroupBy;
	List<String> groupByFields;

	@Override
	public Condition start(Integer start) {
		if (start == null || (start < 0 && start != -1)) throw new BeeIllegalParameterException(START_GREAT_EQ_0);
		this.start = start;
		return this;
	}

	@Override
	public Condition size(Integer size) {
		if (size == null || size <= 0) throw new BeeIllegalParameterException(SIZE_GREAT_0);
		this.size = size;
		return this;
	}

	@Override
	public IncludeType getIncludeType() {
		return includeType;
	}

	@Override
	public Condition setIncludeType(IncludeType includeType) {
		this.includeType = includeType;
		return this;
	}

	@Override
	public Condition op(String field, Op op, Object value) {
		checkField(field);
		list.add(new Expression(field, op, value, getOpRealType(op)));
		this.whereField.add(field);
		return this;
	}
	
	private OpType getOpRealType(Op op) {
		OpType opType;
		if (op == Op.like || op == Op.likeLeft || op == Op.likeRight || op == Op.likeLeftRight || op == Op.notLike) {
			opType = OpType.LIKE;
		} else if (op == Op.in || op == Op.notIn) {
			opType = OpType.IN;
		} else {
			opType = OpType.OP2;
		}
		return opType;
	}

	// v1.9.8
	@Override
	public Condition opOn(String field, Op op, String value) {

		checkField(field);
		onExpList.add(new Expression(field, op, value, getOpRealType(op)));
		return this;
	}

	@Override
	public Condition opOn(String field, Op op, Number value) {

		checkField(field);
		onExpList.add(new Expression(field, op, value, getOpRealType(op)));
		return this;
	}

	@Override
	public Condition opWithField(String field1, Op op, String field2) {
		checkField(field1);
		checkField(field2);

		Expression exp = new Expression(field1, op, field2, OpType.OP_WITH_FIELD);
//		exp.setOpNum(-3); // eg:field1=field2

		list.add(exp);
		this.whereField.add(field1);
//		this.fieldSet.add(field2);
		return this;
	}

	@Override
	public Set<String> getWhereFields() {
//		return whereField;
		final Set<String> set = new HashSet<>(whereField);
		return set;
	}

	@Override
	public Condition and() {
		Expression exp = new Expression();
		exp.setValue(K.and);
		exp.setOpType(OpType.ONE);
		list.add(exp);

		return this;
	}

	@Override
	public Condition or() {
		Expression exp = new Expression();
		exp.setValue(K.or);
		exp.setOpType(OpType.ONE);
		list.add(exp);

		return this;
	}

	@Override
	public Condition not() {
		Expression exp = new Expression();
		exp.setValue(K.not);
		exp.setOpType(OpType.ONE);
		list.add(exp);

		return this;
	}

	@Override
	public Condition lParentheses() {
		Expression exp = new Expression();
//		exp.setOpNum(-2);
		exp.setValue("(");
		exp.setOpType(OpType.L_PARENTHESES);
		list.add(exp);

		return this;
	}

	@Override
	public Condition rParentheses() {
		Expression exp = new Expression();
//		exp.setOpNum(-1);
		exp.setOpType(OpType.R_PARENTHESES);
		exp.setValue(")");
		list.add(exp);

		return this;
	}

	@Override
	public Condition groupBy(String field) {
		checkField(field);
		Expression exp = new Expression();
		exp.setFieldName(field);
		exp.setOpType(OpType.GROUP_BY);

		hasGroupBy = true; // for mongodb

		if (isStartGroupBy) {
			isStartGroupBy = false;
			exp.setValue(" " + K.groupBy + " ");
			groupByFields = new ArrayList<>();
		} else {
			// exp.fieldName=","+field; //不能这样写,field需要转换
			exp.setValue(COMMA);
		}
		String strArray[] = field.split(",");
		for (String f : strArray) {
			groupByFields.add(f);
		}
		list.add(exp);
		return this;
	}

//	@Override
//	public Condition having(String expressionStr) {
//		checkHavingException(expressionStr);
//		Expression exp = new Expression();
//		exp.opType = "having";
//		//exp.value
//		exp.opNum = 2;
//		exp.value2=expressionStr;
//		
//		if (isStartHaving) {
//			if(isStartGroupBy) throw new BeeErrorGrammarException("The 'having' must be after 'group by' !");
//			isStartHaving = false;
////			exp.value = " having ";
//			exp.value = " "+K.having+" ";
//		} else {
////			exp.value = " and ";
//			exp.value = " "+K.and+" ";
//		}
//				
//		list.add(exp);
//		return this;
//	}
	// closed. because can use:
//	 .having(FunctionType.COUNT, "*", Op.ge, 1)
//	 .having(FunctionType.COUNT, "distinct(userid)", Op.ge, 1)

	@Override
	public Condition having(FunctionType functionType, String field, Op op, Number value) {
//		checkField(field);
		checkFieldOrExpression(field);
		Expression exp = new Expression();
		exp.setOpType(OpType.HAVING);
		exp.setFieldName(field);
		exp.setValue2(value);
		exp.setValue3(functionType.getName());
//		exp.opNum = 5;
		exp.setValue4(op.getOperator());

		if (isStartHaving) {
			if (isStartGroupBy) throw new BeeErrorGrammarException("The 'having' must be after 'group by' !");
			isStartHaving = false;
//			exp.value = " having ";
			exp.setValue(" " + K.having + " ");
		} else {
			exp.setValue(" " + K.and + " ");
		}

		list.add(exp);
		return this;
	}

	@Override
	public Condition orderBy(String field) {
//		checkField(field);
		checkFieldOrExpression(field);
		orderByMap.put(field, "asc");// V1.17
		Expression exp = new Expression();
		exp.setOpType(OpType.ORDER_BY2);
		exp.setFieldName(field);
//		exp.opNum = 2;

		if (isStartOrderBy) {
			isStartOrderBy = false;
			exp.setValue(" " + K.orderBy + " ");
		} else {
			exp.setValue(COMMA);
		}
		list.add(exp);
		return this;
	}

	@Override
	public Condition orderBy(String field, OrderType orderType) {
		checkField(field);
		orderByMap.put(field, orderType.getName());// V1.17
		Expression exp = new Expression();
		exp.setOpType(OpType.ORDER_BY3);
		exp.setFieldName(field);
		exp.setValue2(orderType.getName());
//		exp.opNum = 3;

		if (isStartOrderBy) {
			isStartOrderBy = false;
			exp.setValue( " " + K.orderBy + " ");
		} else {
			exp.setValue( COMMA);
		}
		list.add(exp);
		return this;
	}

	@Override
	public Condition orderBy(FunctionType functionType, String field, OrderType orderType) {
		checkField(field);
		orderByMap.put(functionType.getName() + "(" + field + ")", orderType.getName());// V1.17
		Expression exp = new Expression();
		exp.setOpType(OpType.ORDER_BY4);
		exp.setFieldName( field);
		exp.setValue2(orderType.getName());
		exp.setValue3(functionType.getName());
//		exp.opNum = 4;

		if (isStartOrderBy) {
			isStartOrderBy = false;
			exp.setValue( " " + K.orderBy + " ");
		} else {
			exp.setValue( COMMA);
		}
		list.add(exp);
		return this;
	}

	private void setForBetween(String field, Object low, Object high, OpType opType) {
		checkField(field);
		Expression exp = new Expression();
		exp.setFieldName(field);
		exp.setOpType(opType);
		exp.setValue(low);
		exp.setValue2(high);
//		exp.opNum = 3; // 即使不用也不能省,因为默认值是0会以为是其它的

		this.whereField.add(field);

		list.add(exp);
	}

	@Override
	public Condition between(String field, Number low, Number high) {
		setForBetween(field, low, high, OpType.BETWEEN);
		return this;
	}

	@Override
	public Condition notBetween(String field, Number low, Number high) {
		setForBetween(field, low, high, OpType.NOT_BETWEEN);
		return this;
	}

	@Override
	public Condition between(String field, String low, String high) {
		setForBetween(field, low, high, OpType.BETWEEN);
		return this;
	}

	@Override
	public Condition notBetween(String field, String low, String high) {
		setForBetween(field, low, high, OpType.NOT_BETWEEN);
		return this;
	}

	@Override
	public void setSuidType(SuidType suidType) {
		this.suidType = suidType;
	}

	public SuidType getSuidType() {
		return suidType;
	}

	public List<Expression> getExpList() {
		// todo 若要自动调整顺序,可以在这改. group by,having, order by另外定义,在这才添加到list.
//		return list;

		final List<Expression> list0 = new ArrayList<>(this.list);
		return list0;
	}

	public List<Expression> getOnExpList() {
//		return onExpList;

		final List<Expression> onExpList0 = new ArrayList<>(this.onExpList);
		return onExpList0;
	}

	public Integer getStart() {
		return start;
	}

	public Integer getSize() {
		return size;
	}

    ///===================== update set  =============================
	@Override
	public Condition setAdd(String field, Number num) { // for field self
		return forUpdateSet(field, num, OpType.SET_ADD);
	}

	@Override
	public Condition setMultiply(String field, Number num) { // for field self
		return forUpdateSet(field, num, OpType.SET_MULTIPLY);
	}

	@Override
	public Condition setAdd(String field, String otherFieldName) {
		return forUpdateSet(field, otherFieldName, OpType.SET_ADD_FIELD);
	}

	@Override
	public Condition setMultiply(String field, String otherFieldName) {
		return forUpdateSet(field, otherFieldName, OpType.SET_MULTIPLY_FIELD);
	}

	@Override
	public Condition setWithField(String field1, String field2) {
		return forUpdateSet(field1, field2, OpType.SET_WITH_FIELD);
	}

	@Override
	public Condition set(String fieldNmae, Number num) {
		return _forUpdateSet2(fieldNmae, num);
	}

	@Override
	public Condition set(String fieldNmae, String value) {
		return _forUpdateSet2(fieldNmae, value);
	}

	@Override
	public Condition setNull(String fieldNmae) {
		return _forUpdateSet2(fieldNmae, null);
	}

	@Override
	public Condition selectField(String... fieldList) {
		if (fieldList != null && fieldList.length == 1) checkField(fieldList[0]);
		else checkField(StringUtils.toCommasString(fieldList));

		this.selectField = fieldList;
		return this;
	}

	@Override
	public Condition selectDistinctField(String fieldName) {
		funExpList.add(new FunExpress("distinct", fieldName, null));
		return this;
	}

	@Override
	public Condition selectDistinctField(String fieldName, String alias) {
		checkField(alias);
		alias = _toColumnName(alias);
		funExpList.add(new FunExpress("distinct", fieldName, alias));
		return this;
	}

	@Override
	public String[] getSelectField() {
//		return this.selectField;

		final String[] selectField0 = this.selectField;
		return selectField0;
	}

	public List<Expression> getUpdateExpList() {
//		return updateSetList;

		final List<Expression> updateSetList0 = new ArrayList<>(this.updateSetList);
		return updateSetList0;
	}

	public List<FunExpress> getFunExpList() {
//		return funExpList;

		final List<FunExpress> funExpList0 = new ArrayList<>(this.funExpList);
		return funExpList0;
	}

	private Condition forUpdateSet(String field, String otherFieldName, OpType opType) {
		checkField(otherFieldName);
		return _forUpdateSet(field, otherFieldName, opType);
	}

	private Condition forUpdateSet(String field, Number num, OpType opType) {
		return _forUpdateSet(field, num, opType);
	}

	private Condition _forUpdateSet(String field, Object obj, OpType opType) {
		checkField(field);
		Expression exp = new Expression();
		exp.setFieldName(field);
		exp.setOpType(opType); // "setAdd" or "setMultiply"; setAddField; setMultiplyField; setWithField
		exp.setValue(obj);

		this.updatefields.add(field);
		updateSetList.add(exp);

		return this;
	}

	// set field=value
	private Condition _forUpdateSet2(String field, Object obj) {
		checkField(field);
		Expression exp = new Expression();
		exp.setFieldName(field);
		exp.setValue(obj);
		exp.setOpType(OpType.SET);

		this.updatefields.add(field);
		updateSetList.add(exp);

		return this;
	}

	@Override
	public Set<String> getUpdatefields() {
		final Set<String> updatefields0 = new HashSet<>(this.updatefields);
		return updatefields0;
	}

	@Override
	public Condition forUpdate() {
		isForUpdate = true;
		return this;
	}

	@Override
	public Boolean getForUpdate() {
//		return isForUpdate;
		final Boolean f = isForUpdate;
		return f;
	}

	@Override
	public Boolean hasGroupBy() {
		return hasGroupBy;
	}

	// v1.9
	@Override
	public Condition selectFun(FunctionType functionType, String fieldForFun) {
		funExpList.add(new FunExpress(functionType, fieldForFun, null));
		return this;
	}

	// v1.9
	@Override
	public Condition selectFun(FunctionType functionType, String fieldForFun, String alias) {
		checkField(alias);
		alias = _toColumnName(alias);
		funExpList.add(new FunExpress(functionType, fieldForFun, alias));
		return this;
	}

	@Override
	public List<String> getGroupByFields() {
//		return groupByFields;

		final List<String> list;
		if (groupByFields == null) list = null;
		else list = new ArrayList<>(groupByFields);
		return list;
	}

	private void checkField(String fields) {
		NameCheckUtil.checkName(fields);
	}

	private void checkFieldOrExpression(String field) {
		if (NameCheckUtil.isIllegal(field)) {
			throw new BeeErrorNameException("The field: '" + field + "' is illegal!");
		}
	}

	// 1.17
	@Override // 2.0
	public Map<String, String> getOrderBy() {
		return orderByMap;
	}

	private String _toColumnName(String fieldName) {
		return NameTranslateHandle.toColumnName(fieldName);
	}

	@Override
	public Condition clone() {
		try {
			Serializer jdks = new JdkSerializer();
			Object cloned = jdks.unserialize(jdks.serialize(this));
			return (ConditionImpl) cloned;
		} catch (Exception e) {
			Logger.debug("Clone Condition error. " + e.getMessage());
			return this;
		}
	}

	public final class FunExpress implements Serializable {
		private static final long serialVersionUID = 1596710362289L;

		private String functionType;
		private String field;
		private String alias;

		public FunExpress(FunctionType functionType, String field, String alias) {
			checkField(field);
			this.functionType = functionType.getName();
			this.field = field;
			this.alias = alias;
		}

		public FunExpress(String functionType, String field, String alias) {
			checkField(field);
			this.functionType = functionType;
			this.field = field;
			this.alias = alias;
		}

		FunExpress() {}

		public String getFunctionType() {
			return functionType;
		}

		public void setFunctionType(String functionType) {
			this.functionType = functionType;
		}

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}

		public String getAlias() {
			return alias;
		}

		public void setAlias(String alias) {
			this.alias = alias;
		}
	}

}
