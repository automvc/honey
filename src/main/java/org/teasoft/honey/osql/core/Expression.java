/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.io.Serializable;

import org.teasoft.bee.osql.Op;

/**
 * Expression
 * @author Kingstar
 * @since  1.6
 * @since  2.5.2
 */
public final class Expression implements Serializable {

	private static final long serialVersionUID = 1592803913607L;

	private OpType opType; //2.5.2
	private String fieldName;
	private Op op;
	private Object value;
	private Object value2;
	private Object value3;
	private Object value4; // for having

	public Expression() {}

	public Expression(String field, Op op, Object value,OpType opType) {
		this.fieldName = field;
		this.opType = opType;
		this.op = op;
		this.value = value;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
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

	public OpType getOpType() {
		return opType;
	}

	public void setOpType(OpType opType) {
		this.opType = opType;
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
