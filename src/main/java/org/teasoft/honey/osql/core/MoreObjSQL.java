/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.List;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.MoreObjToSQL;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.api.MoreTable;
import org.teasoft.bee.osql.exception.BeeIllegalParameterException;

/**
 * 多表查询,MoreTable实现类.Multi table query, moretable implementation class.
 * @author Kingstar
 * @since  1.7
 */
public class MoreObjSQL extends AbstractCommOperate implements MoreTable {

	private BeeSql beeSql;
	private MoreObjToSQL moreObjToSQL;
	
	private static final String SELECT_SQL = "select SQL: ";

	@Override
	public <T> List<T> select(T entity) {
		if (entity == null) return null;
		doBeforePasreEntity(entity);  //因要解析子表,子表下放再执行
		String sql = getMoreObjToSQL().toSelectSQL(entity);
		sql=doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SQL, sql);
		List<T> list = getBeeSql().moreTableSelect(sql, entity); 
		doBeforeReturn(list);
		return list;
	}

	@Override
	public <T> List<T> select(T entity, int start, int size) {
		if (entity == null) return null;
		if(size<=0) throw new BeeIllegalParameterException("Parameter 'size' need great than 0!");
		if(start<0) throw new BeeIllegalParameterException("Parameter 'start' need great equal 0!");
		doBeforePasreEntity(entity);  //因要解析子表,子表下放再执行
		String sql = getMoreObjToSQL().toSelectSQL(entity,start,size);
		sql=doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SQL, sql);
		List<T> list = getBeeSql().moreTableSelect(sql, entity); 
		doBeforeReturn(list);
		return list;
	}

	@Override
	public <T> List<T> select(T entity, Condition condition) {
		if (entity == null) return null;
		regCondition(condition);
		doBeforePasreEntity(entity);  //因要解析子表,子表下放再执行
		String sql = getMoreObjToSQL().toSelectSQL(entity,condition);
		sql=doAfterCompleteSql(sql);
		Logger.logSQL(SELECT_SQL, sql);
		List<T> list = getBeeSql().moreTableSelect(sql, entity); 
		doBeforeReturn(list);
		return list;
	}
	
	@Override
	public MoreObjSQL setDynamicParameter(String para, String value) {
		OneTimeParameter.setAttribute(para, value);
		return this;
	}
	
	private void doBeforePasreEntity(Object entity) {
		super.doBeforePasreEntity(entity, SuidType.SELECT);
		OneTimeParameter.setAttribute(StringConst.InterceptorChainForMoreTable, getInterceptorChain());//用于子表
	}

	public BeeSql getBeeSql() {
		if(this.beeSql==null) beeSql = BeeFactory.getHoneyFactory().getBeeSql();
		return beeSql;
	}

	public void setBeeSql(BeeSql beeSql) {
		this.beeSql = beeSql;
	}
	
	public MoreObjToSQL getMoreObjToSQL() {
		if(moreObjToSQL==null) return BeeFactory.getHoneyFactory().getMoreObjToSQL();
		return moreObjToSQL;
	}

	public void setMoreObjToSQL(MoreObjToSQL moreObjToSQL) {
		this.moreObjToSQL = moreObjToSQL;
	}

}
