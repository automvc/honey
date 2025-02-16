/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution.ds;

import org.teasoft.bee.osql.SuidType;

/**
 * @author Kingstar
 * @since  1.8
 */
public class RouteStruct {

	private String tableNames; // 用##间隔
//	private String returnType; 
	private SuidType suidType;
	private String beanString;
	private Class entityClass; // maybe is null

	public String getTableNames() {
		return tableNames;
	}

	public void setTableNames(String tableNames) {
		this.tableNames = tableNames;
	}

	public SuidType getSuidType() {
		return suidType;
	}

	public void setSuidType(SuidType suidType) {
		this.suidType = suidType;
	}

	public String getBeanString() {
		return beanString;
	}

	public void setBeanString(String beanString) {
		this.beanString = beanString;
	}

	public Class getEntityClass() {
		return entityClass;
	}

	public void setEntityClass(Class entityClass) {
		this.entityClass = entityClass;
	}
}
