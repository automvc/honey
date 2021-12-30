/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.List;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.MoreObjToSQL;
import org.teasoft.bee.osql.MoreTable;
import org.teasoft.bee.osql.exception.BeeIllegalParameterException;

/**
 * @author Kingstar
 * @since  1.7
 */
public class MoreObjSQL implements MoreTable{

	private BeeSql beeSql;
	private MoreObjToSQL moreObjToSQL;

	public MoreObjSQL() {}

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

	@Override
	public <T> List<T> select(T entity) {
		if (entity == null) return null;

		String sql = getMoreObjToSQL().toSelectSQL(entity);
		Logger.logSQL("select SQL: ", sql);
		return getBeeSql().moreTableSelect(sql, entity); 
	}

	@Override
	public <T> List<T> select(T entity, int start, int size) {
		if (entity == null) return null;
		if(size<=0) throw new BeeIllegalParameterException("Parameter 'size' need great than 0!");
		if(start<0) throw new BeeIllegalParameterException("Parameter 'start' need great equal 0!");

		String sql = getMoreObjToSQL().toSelectSQL(entity,start,size);
		Logger.logSQL("select SQL: ", sql);
		return getBeeSql().moreTableSelect(sql, entity); 
	}

	@Override
	public <T> List<T> select(T entity, Condition condition) {
		if (entity == null) return null;

		String sql = getMoreObjToSQL().toSelectSQL(entity,condition);
		Logger.logSQL("select SQL: ", sql);
		return getBeeSql().moreTableSelect(sql, entity); 
	}
	
	@Override
	public MoreObjSQL setDynamicParameter(String para, String value) {
		OneTimeParameter.setAttribute(para, value);
		return this;
	}

}
