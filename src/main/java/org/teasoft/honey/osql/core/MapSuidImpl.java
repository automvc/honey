/*
 * Copyright 2016-2021 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.MapSql;
import org.teasoft.bee.osql.api.MapSuid;
import org.teasoft.honey.util.ObjectUtils;

/**
 * 操作数据库不依赖javabean结构的类.The class that operation database does not depend on Javabean.
 * @author Kingstar
 * @since  1.9
 */
public class MapSuidImpl extends AbstractCommOperate implements MapSuid {

	private BeeSql beeSql;

//	//V1.11
//	private InterceptorChain interceptorChain;
//	private String dsName;
//	private NameTranslate nameTranslate; //用于设置当前对象使用的命名转换器.使用默认的不需要设置

	public BeeSql getBeeSql() {
		if (beeSql == null) beeSql = BeeFactory.getHoneyFactory().getBeeSql();
		return beeSql;
	}

	public void setBeeSql(BeeSql beeSql) {
		this.beeSql = beeSql;
	}

//	@Override
//	public InterceptorChain getInterceptorChain() {
//		if (interceptorChain == null) return BeeFactory.getHoneyFactory().getInterceptorChain();
//		return HoneyUtil.copy(interceptorChain);
//	}
//
//	public void setInterceptorChain(InterceptorChain interceptorChain) {
//		this.interceptorChain = interceptorChain;
//	}
//
//	@Override
//	public void setDataSourceName(String dsName) {
//		this.dsName = dsName;
//	}
//
//	@Override
//	public String getDataSourceName() {
//		return dsName;
//	}
//	
//	@Override
//	public void setNameTranslate(NameTranslate nameTranslate) {
//		this.nameTranslate=nameTranslate;
//	}

	@Override
	public List<String[]> selectString(MapSql mapSql) {
		doBeforePasreEntity(SuidType.SELECT);

		String sql = MapSqlProcessor.toSelectSqlByMap(mapSql);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("In MapSuid, select List<String[]> SQL: ", sql);
		List<String[]> list = getBeeSql().select(sql);
		doBeforeReturn();
		return list;
	}

	@Override
	public String selectJson(MapSql mapSql) {
		doBeforePasreEntity(SuidType.SELECT);
		String sql = MapSqlProcessor.toSelectSqlByMap(mapSql);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("In MapSuid, selectJson SQL: ", sql);
		String json = getBeeSql().selectJson(sql);
		doBeforeReturn();
		return json;
	}

	@Override
	public List<Map<String, Object>> select(MapSql mapSql) {
		doBeforePasreEntity(SuidType.SELECT);
		String sql = MapSqlProcessor.toSelectSqlByMap(mapSql);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("In MapSuid, select List<Map> SQL: ", sql);
		List<Map<String, Object>> list = getBeeSql().selectMapList(sql);
		doBeforeReturn();
		return list;
	}

	@Override
	public int count(MapSql mapSql) {
		doBeforePasreEntity(SuidType.SELECT);
		String sql = MapSqlProcessor.toCountSqlByMap(mapSql);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("In MapSuid, count SQL: ", sql);
		String total = getBeeSql().selectFun(sql);
		doBeforeReturn();
		return total == null ? 0 : Integer.parseInt(total);
	}

	@Override
	public Map<String, Object> selectOne(MapSql mapSql) {
		doBeforePasreEntity(SuidType.SELECT);
		String sql = MapSqlProcessor.toSelectSqlByMap(mapSql);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("In MapSuid, selectOne Map SQL: ", sql);
		List<Map<String, Object>> list = getBeeSql().selectMapList(sql);
		doBeforeReturn();
		if (ObjectUtils.isNotEmpty(list)) {
			return list.get(0);
		} else {
			return Collections.emptyMap();
		}
	}

	@Override
	public int insert(MapSql mapSql) {
		if (mapSql == null) return -1;
		doBeforePasreEntity(SuidType.INSERT);

		String sql = MapSqlProcessor.toInsertSqlByMap(mapSql);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("In MapSuid, insert SQL: ", sql);

		int insertNum = getBeeSql().modify(sql);
		doBeforeReturn();

		return insertNum;
	}

	@Override
	public long insertAndReturnId(MapSql mapSql) {

		if (mapSql == null) return -1;
		doBeforePasreEntity(SuidType.INSERT);
		String sql = MapSqlProcessor.toInsertSqlByMap(mapSql, true); // will get pkName and set into OneTimeParameter
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("In MapSuid, insertAndReturnId SQL: ", sql);

		Object obj = OneTimeParameter.getAttribute(StringConst.MapSuid_Insert_Has_ID);
		long newId;
		if (obj != null) {
			newId = Long.parseLong(obj.toString());
			if (newId > 1) { //设置有大于1的值,使用设置的
				OneTimeParameter.getAttribute(StringConst.PK_Name_For_ReturnId); //不使用insertAndReturnId,提前消费一次性变量
				int insertNum = getBeeSql().modify(sql);
				if (insertNum == 1) {
					return newId;
				} else {
					return insertNum;
				}
			} else {
				if (HoneyUtil.isOracle()) {
					Logger.debug("Need create Sequence and Trigger for auto increment id. "
							+ "By the way,maybe use distribute id is better!");
				}
			}
		}
//		假如处理后id为空,则用db生成.
		//id will gen by db
		newId = getBeeSql().insertAndReturnId(sql);
		doBeforeReturn();

		return newId;

	}

	@Override
	public int delete(MapSql mapSql) {
		doBeforePasreEntity(SuidType.DELETE);
		String sql = MapSqlProcessor.toDeleteSqlByMap(mapSql);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("In MapSuid, delete SQL: ", sql);
		int a = getBeeSql().modify(sql);
		doBeforeReturn();
		return a;
	}

	@Override
	public int update(MapSql mapSql) {
		doBeforePasreEntity(SuidType.UPDATE);
		String sql = MapSqlProcessor.toUpdateSqlByMap(mapSql);
		sql = doAfterCompleteSql(sql);
		Logger.logSQL("In MapSuid, update SQL: ", sql);
		int a = getBeeSql().modify(sql);
		doBeforeReturn();
		return a;
	}
	
	private void doBeforePasreEntity(SuidType suidType) {
		Object entity=null;
		super.doBeforePasreEntity(entity, suidType);
	}

//	private void doBeforePasreEntity(SuidType suidType) {
//		regSuidType(suidType);
//		if (this.dsName != null) HoneyContext.setTempDS(dsName);
//		if(this.nameTranslate!=null) HoneyContext.setCurrentNameTranslate(nameTranslate);
//		getInterceptorChain().beforePasreEntity(null, suidType);
//	}
//
//	private String doAfterCompleteSql(String sql) {
//		//if change the sql,need update the context.
//		sql = getInterceptorChain().afterCompleteSql(sql);
//		return sql;
//	}
//
//	private void doBeforeReturn() {
//		if (this.dsName != null) HoneyContext.removeTempDS();
//		if(this.nameTranslate!=null) HoneyContext.removeCurrentNameTranslate();
//		getInterceptorChain().beforeReturn();
//	}
//	
//	protected void regSuidType(SuidType SuidType) {
//		if (HoneyConfig.getHoneyConfig().isAndroid) HoneyContext.regSuidType(SuidType);
//	}

}
