/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

/**
 * Cache Suid Struct.
 * @author Kingstar
 * @since  1.4
 */
@SuppressWarnings("rawtypes")
public class CacheSuidStruct {
	
	private String sql;  //不带值的
	private String tableNames; //用##间隔
	private String returnType;  //返回值类型用于过滤缓存的查询结果,防止同一查询sql的不同类型的结果;  但更改的操作可不需要用这个值
	private String suidType;  //操作类型
	
	private Class entityClass; //V2.0
	
	public String getSql() {
		return sql;
	}
	public void setSql(String sql) {
		this.sql = sql;
	}

	public String getTableNames() {
		return tableNames;
	}

	public void setTableNames(String tableNames) {
		if (tableNames != null) tableNames = tableNames.toLowerCase(); // V2.1.8
		this.tableNames = tableNames;
	}

	public String getSuidType() {
		return suidType;
	}

	public void setSuidType(String suidType) {
		this.suidType = suidType;
	}

	public String getReturnType() {
		return returnType;
	}
	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}
	public Class getEntityClass() {
		return entityClass;
	}
	public void setEntityClass(Class entityClass) {
		this.entityClass = entityClass;
	}

}
