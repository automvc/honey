/*
 * Copyright 2013-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.ArrayList;
import java.util.List;

import org.teasoft.bee.osql.chain.ToSql;
import org.teasoft.bee.osql.exception.BeeIllegalSQLException;
import org.teasoft.honey.util.StringUtils;

/**
 * @author Kingstar
 * @since  1.3
 * @since  2.4.0
 */
public abstract class AbstractToSqlForChain implements ToSql{
	
	protected StringBuffer sql = new StringBuffer();
	
	//2.4.0
	private List<PreparedValue>  pvList = new ArrayList<>();
	private boolean isUsePlaceholder=true;
	private String table;
	
	public String toSQL() {
		String sql0=toSQL(true);//oracle用jdbc不允许有分号
		if(isUsePlaceholder()) {
			setContext(sql0);  //但是使用pre的时候，会把它冲了; V2.4.0 使用pre无参数时，已不会。
//			if (StringUtils.isNotBlank(getTable()))
//				HoneyContext.addInContextForCache(sql0, getTable());   //纳入缓存管理
		}
		return sql0;
	}

	//用于输出sql;不提供缓存功能
	public String toSQL(boolean noSemicolon) {
		if (noSemicolon){
			return sql.toString();
		}else{
			return sql.toString()+";";
		}
	}
	
	@Override
	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}
	
	public void appendTable(String table) {
		if(StringUtils.isBlank(this.table)) this.table=table;
		else this.table+=StringConst.TABLE_SEPARATOR+table;
	}
	

	public boolean isUsePlaceholder() {
		return isUsePlaceholder;
	}
	
	public void setUsePlaceholder(boolean isUsePlaceholder) {
		this.isUsePlaceholder = isUsePlaceholder;
	}

	protected void addValue(Object v) {
		PreparedValue preparedValue = new PreparedValue(); //NULL要另外处理
		if (v == null)
			preparedValue.setType(Object.class.getName());
		else
			preparedValue.setType(v.getClass().getName());
		preparedValue.setValue(v);
		pvList.add(preparedValue);
	}
	
	protected void setContext(String sql){
		HoneyContext.setContext(sql, pvList, table); //已有cache
	}
	
	@Override
	public List<PreparedValue> getPvList() {
		return pvList;
	}
	
	protected void checkExpression(String expression){
		if(Check.isNotValidExpression(expression)) {
			throw new BeeIllegalSQLException("The expression: '"+expression+ "' is invalid!");
		}
	}

}
