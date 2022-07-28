/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import org.teasoft.bee.osql.Op;

/**
 * Expression
 * @author Kingstar
 * @since  1.6
 */
public final class Expression {

	String fieldName;
	String opType;
	Op op;  //V1.17
	Object value;
	int opNum;
	Object value2;
	Object value3;
	Object value4; //for having

	public Expression() {}

	public Expression(String field, Op opType, Object value) {
		this.fieldName = field;
		this.opType = opType.getOperator();
		this.op=opType; //V1.17  for likeLeft,likeRight,likeLeftRight
		this.value = value;
		this.opNum = 2;
	}

//	public Expression(String field, Object value) {
//		this.fieldName = field;
//		this.opType = Op.eq.getOperator();
//		this.value = value;
//		this.opNum = 2;
//	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getOpType() {
		return opType;
	}

	public void setOpType(String opType) {
		this.opType = opType;
	}
	
	/**
	 * 
	 * @return
	 * @since 1.17
	 */
	public Op getOp() {
		return op;
	}

	/**
	 * 
	 * @param op
	 * @since 1.17
	 */
	public void setOp(Op op) {
		this.op = op;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public int getOpNum() {
		return opNum;
	}

	public void setOpNum(int opNum) {
		this.opNum = opNum;
	}

	public Object getValue2() {
		return value2;
	}

	public void setValue2(Object value2) {
		this.value2 = value2;
	}

	public Object getValue3() {
		return value3;
	}

	public void setValue3(Object value3) {
		this.value3 = value3;
	}

	public Object getValue4() {
		return value4;
	}

	public void setValue4(Object value4) {
		this.value4 = value4;
	}
	
}
