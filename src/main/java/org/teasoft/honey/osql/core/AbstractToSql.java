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
public abstract class AbstractToSql implements ToSql{
	
	protected StringBuffer sql = new StringBuffer();
	
	public String toSQL() {
////		return toSQL(false);
//		return toSQL(true); //oracle用jdbc不允许有分号
		
		String sql0=toSQL(true);
		if(isUsePlaceholder()) setContext(sql0);  //但是使用pre的时候，会把它冲了; V2.4.0 使用pre无参数时，已不会。
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
	
	//2.4.0
	private List<PreparedValue>  pvList = new ArrayList<>();
	private boolean isUsePlaceholder=true;
	private String table;
	
	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}
	
	public void appendTable(String table) {
		if(StringUtils.isBlank(this.table)) this.table=table;
		else this.table+="##"+table;
	}
	

	public boolean isUsePlaceholder() {
		return isUsePlaceholder;
	}
	
	public void setUsePlaceholder(boolean isUsePlaceholder) {
		this.isUsePlaceholder = isUsePlaceholder;
	}

	protected void addValue(Object v) { //TODO  NULL要另外处理
		PreparedValue preparedValue = new PreparedValue();
		preparedValue.setType(v.getClass().getName());
//		System.out.println(v.getClass().getName());
		preparedValue.setValue(v);
		pvList.add(preparedValue);
	}
	
//	protected static void setContext(String sql,List<PreparedValue> list,String tableName){
//		HoneyContext.setContext(sql, list, tableName);
//	}
	
	protected void setContext(String sql){
		HoneyContext.setContext(sql, pvList, table);
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
