/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.FunctionType;
import org.teasoft.bee.osql.IncludeType;
import org.teasoft.bee.osql.Op;
import org.teasoft.bee.osql.OrderType;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.exception.BeeErrorGrammarException;

/**
 * @author Kingstar
 * @since  1.6
 */
public class ConditionImpl implements Condition {

	private SuidType suidType;
	public List<Expression> list = new ArrayList<>();
	private Set<String> fieldSet = new HashSet<>();
	private IncludeType includeType;
	
	private String selectField;
	private Boolean isForUpdate;
	
	private List<Expression> updateSetList = new ArrayList<>();
	private Set<String> updatefieldSet = new HashSet<>();
	
	private boolean isStartGroupBy = true;
	private boolean isStartHaving = true;
	private boolean isStartOrderBy = true;

	private static String COMMA = ",";

	private Integer start;
	private Integer size;

	@Override
	public Condition start(Integer start) {
		this.start = start;
		return this;
	}

	@Override
	public Condition size(Integer size) {
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
	public Condition op(String field, Op Op, Object value) {
		list.add(new Expression(field, Op, value));
		this.fieldSet.add(field);
		return this;
	}

	@Override
	public Set<String> getFieldSet() {
		return fieldSet;
	}

	@Override
	public Condition and() {
		Expression exp = new Expression();
		exp.setOpNum(1);
		exp.value = "and";
		list.add(exp);

		return this;
	}

	@Override
	public Condition or() {
		Expression exp = new Expression();
		exp.setOpNum(1);
		exp.value = "or";
		list.add(exp);

		return this;
	}

	@Override
	public Condition lParentheses() {
		Expression exp = new Expression();
		exp.setOpNum(-2);
		exp.value = "(";
		list.add(exp);

		return this;
	}

	@Override
	public Condition rParentheses() {
		Expression exp = new Expression();
		exp.setOpNum(-1);
		exp.value = ")";
		list.add(exp);

		return this;
	}

	@Override
	public Condition groupBy(String field) {
		Expression exp = new Expression();
		exp.fieldName = field;
		exp.opType = "groupBy";
		
		if (isStartGroupBy) {
			isStartGroupBy = false;
			exp.value =" group by ";
		} else {
			//exp.fieldName=","+field; //不能这样写,field需要转换
			exp.value = COMMA;
		}
		list.add(exp);
		return this;
	}

	@Override
	public Condition having(String expressionStr) {
		Expression exp = new Expression();
		exp.opType = "having";
		//exp.value
		exp.opNum = 2;
		exp.value2=expressionStr;
		
		if (isStartHaving) {
			if(isStartGroupBy) throw new BeeErrorGrammarException("The 'having' must be after 'group by' !");
			isStartHaving = false;
			exp.value = " having ";
		} else {
			exp.value = " and ";
		}
				
		list.add(exp);
		return this;
	}

	@Override
	public Condition having(FunctionType functionType, String field, Op Op, Number value) {
		Expression exp = new Expression();
		exp.opType = "having";
		//exp.value
		exp.fieldName=field;
		exp.value2=value;
		exp.value3=functionType.getName();
		exp.opNum = 5;
		exp.value4=Op.getOperator();
		
		if (isStartHaving) {
			if(isStartGroupBy) throw new BeeErrorGrammarException("The 'having' must be after 'group by' !");
			isStartHaving = false;
			exp.value = " having ";
		} else {
			exp.value = " and ";
		}
				
		list.add(exp);
		return this;
	}

	@Override
	public Condition orderBy(String field) {

		Expression exp = new Expression();
		exp.opType = "orderBy";
		//		exp.value
		exp.fieldName = field;
		exp.opNum = 2;

		if (isStartOrderBy) {
			isStartOrderBy = false;
			exp.value = " order by ";
		} else {
			exp.value = COMMA;
		}
		list.add(exp);
		return this;
	}

	@Override
	public Condition orderBy(String field, OrderType orderType) {

		Expression exp = new Expression();
		exp.opType = "orderBy";
		//		exp.value
		exp.fieldName = field;
		exp.value2 = orderType.getName();
		exp.opNum = 3;

		if (isStartOrderBy) {
			isStartOrderBy = false;
			exp.value = " order by ";
		} else {
			exp.value = COMMA;
		}
		list.add(exp);
		return this;
	}
	
	@Override
	public Condition orderBy(FunctionType functionType, String field, OrderType orderType) {
		Expression exp = new Expression();
		exp.opType = "orderBy";
		//		exp.value
		exp.fieldName = field;
		exp.value2 = orderType.getName();
		exp.value3=functionType.getName();
		exp.opNum = 4;

		if (isStartOrderBy) {
			isStartOrderBy = false;
			exp.value = " order by ";
		} else {
			exp.value = COMMA;
		}
		list.add(exp);
		return this;
	}
	
	private void setForBetween(String field, Object low, Object high,String type){
		Expression exp = new Expression();
		exp.fieldName = field;
//		exp.opType = "between";
		exp.opType =type;
		exp.value=low;
		exp.value2=high;
		exp.opNum=3;  //即使不用也不能省,因为默认值是0会以为是其它的
		
		this.fieldSet.add(field);
		
		list.add(exp);
	}
	
	@Override
	public Condition between(String field, Number low, Number high) {
		
		setForBetween(field, low, high, " between ");
		
		return this;
	}

	@Override
	public Condition notBetween(String field, Number low, Number high) {
		setForBetween(field, low, high, " not between ");
		
		return this;
	}

	@Override
	public Condition between(String field, String low, String high) {
		setForBetween(field, low, high, " between ");
		
		return this;
	}

	@Override
	public Condition notBetween(String field, String low, String high) {
		setForBetween(field, low, high, " not between ");
		
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
		return list;
	}

	public Integer getStart() {
		return start;
	}

	public Integer getSize() {
		return size;
	}
	
	private static String setAdd="setAdd";
	private static String setMultiply="setMultiply";
	
	private static String setAddField = "setAddField";
	private static String setMultiplyField = "setMultiplyField";

	@Override
	public Condition setAdd(String field, Number num) {  //for field self
        return forUpdateSet(field, num, setAdd);
	}

	@Override
	public Condition setMultiply(String field, Number num) { //for field self
		 return forUpdateSet(field, num, setMultiply);
	}
	
	@Override
	public Condition setAdd(String field, String fieldName) {
		return forUpdateSet(field, fieldName, setAddField);
	}

	@Override
	public Condition setMultiply(String field, String fieldName) {
		return forUpdateSet(field, fieldName, setMultiplyField);
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
	public Condition selectField(String fieldList) {
		
		this.selectField=fieldList;
		
		return this;
	}
	
	@Override
	public String getSelectField(){
		return this.selectField;
	}

	public List<Expression> getUpdateExpList() {
		return updateSetList;
	}
	
	private Condition forUpdateSet(String field, String fieldName,String opType){
		return _forUpdateSet(field, fieldName, opType);
	}
	
	private Condition forUpdateSet(String field, Number num,String opType){
		return _forUpdateSet(field, num, opType);
	}
	
	private Condition _forUpdateSet(String field, Object ojb,String opType){
		Expression exp = new Expression();
		exp.fieldName = field;
		exp.opType =opType; //"setAdd" or "setMultiply";  setAddField; setMultiplyField
		exp.value=ojb;
		exp.opNum=1;  
		
		this.updatefieldSet.add(field);
		updateSetList.add(exp);
		
		return this;
	}
	
	//set field=value
	private Condition _forUpdateSet2(String field, Object ojb) {
		Expression exp = new Expression();
		exp.fieldName = field;
	  //exp.opType =opType; 
		exp.value = ojb;
		exp.opNum = 1;

		this.updatefieldSet.add(field);
		updateSetList.add(exp);

		return this;
	}
	
	@Override
	public Set<String> getUpdatefieldSet() {
		return updatefieldSet;
	}

	@Override
	public Condition forUpdate() {
		isForUpdate = true;
		return this;
	}

	@Override
	public Boolean getForUpdate() {
		return isForUpdate;
	}
	
}
