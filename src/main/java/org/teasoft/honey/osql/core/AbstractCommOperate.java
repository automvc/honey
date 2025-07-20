/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.List;

import org.teasoft.bee.osql.CommOperate;
import org.teasoft.bee.osql.NameTranslate;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.api.Condition;
import org.teasoft.bee.osql.interccept.InterceptorChain;
import org.teasoft.honey.logging.Logger;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class AbstractCommOperate implements CommOperate {

	// V1.11
	// 全局的可以使用InterceptorChainRegistry配置;只是某个对象要使用,才使用对象配置
	protected InterceptorChain interceptorChain;
	protected String dsName;
	protected NameTranslate nameTranslate; // 用于设置当前对象使用的命名转换器(每次设置只使用一次即失效).使用默认的不需要设置

	public AbstractCommOperate() {}

	@Override
	public InterceptorChain getInterceptorChain() {
		if (interceptorChain == null) return BeeFactory.getHoneyFactory().getInterceptorChain();
		return HoneyUtil.copy(interceptorChain);
	}

	/**
	 * 全局的可以使用InterceptorChainRegistry配置;只是某个对象要使用,再使用对象配置
	 * @param interceptorChain
	 */
	public void setInterceptorChain(InterceptorChain interceptorChain) {
		this.interceptorChain = interceptorChain;
	}

	@Override
	public void setNameTranslateOneTime(NameTranslate nameTranslate) {
		this.nameTranslate = nameTranslate;
//		V2.1.5.1 bug.   设置了,就放上下文,即使只用一次,但设置了不用, 别的对象,即使不是Suid同种类型,也会拿到上下文中的NameTranslate
//		if (this.nameTranslate != null) HoneyContext.setCurrentNameTranslate(nameTranslate); 
	}

	@Override
	public void setDataSourceName(String dsName) {
		this.dsName = dsName;
	}

	@Override
	public String getDataSourceName() {
		return dsName;
//		return Router.getDsName(); //不行. suid的dsName在执行时才通过拦截器设置.若提前通过线程设置,会因顺序原因,被覆盖.
	}

	protected void regCondition(Condition condition) {
		if (condition != null) condition = condition.clone();
		HoneyContext.setConditionLocal(condition);
	}

	protected void doBeforePasreEntity(Object entity, SuidType SuidType) {
		_doBeforePasreEntity(SuidType);
		initParseDefineColumn(entity); // 2.4.0
//		about entity: if necessary, can test whether it is Class type.
		getInterceptorChain().beforePasreEntity(entity, SuidType);
		timerStart();
	}

	void doBeforePasreEntity(Object entityArray[], SuidType SuidType) {
		_doBeforePasreEntity(SuidType);
		if (entityArray != null && entityArray.length > 0) initParseDefineColumn(entityArray[0]); // 2.4.0
		getInterceptorChain().beforePasreEntity(entityArray, SuidType);
		timerStart();
	}

	private void _doBeforePasreEntity(SuidType suidType) {
		regSuidType(suidType);
		if (this.nameTranslate != null) HoneyContext.setCurrentNameTranslateOneTime(nameTranslate); // enhance V2.1
		if (this.dsName != null) HoneyContext.setTempDS(dsName);
	}

	String doAfterCompleteSql(String sql) {
		// if change the sql, need update the context.
		sql = getInterceptorChain().afterCompleteSql(sql);
		return sql;
	}

	@SuppressWarnings("rawtypes")
	protected void doBeforeReturn(List list) {
		_doBeforeReturn();
		getInterceptorChain().beforeReturn(list);
	}

	protected void doBeforeReturn() {
		_doBeforeReturn();
		getInterceptorChain().beforeReturn();
	}

	private void _doBeforeReturn() {
		timerEnd();
		if (this.dsName != null) HoneyContext.removeTempDS();
		if (this.nameTranslate != null) HoneyContext.removeCurrentNameTranslate();
		this.nameTranslate = null; // 2.1 仅允许一次有效. 因整个应用周期内,只有一个bean时,会影响到其它情况的使用(如spring整合)
	}

	protected void regSuidType(SuidType suidType) {
		if (suidType == null) return;
		if (HoneyConfig.getHoneyConfig().isAndroid) HoneyContext.regSuidType(suidType);
	}

	private final String field2Column = StringConst.PREFIX + "Field2Column";

	// 2.4.0 as sharding support column name for tabField/dsField, parse first.
	private void initParseDefineColumn(Object entity) {
		if (entity == null) return;
		Class entityClass;
		// 2.4.0.8
		if (entity instanceof Class)
			entityClass = (Class) entity;
		else
			entityClass = entity.getClass();

		String entityFullName = entityClass.getName();
		Boolean flag = HoneyContext.getCustomFlagMap(field2Column + entityFullName);
		if (flag == null) {// 还没检测的
			HoneyContext.initParseDefineColumn(entityClass);
			flag = HoneyContext.getCustomFlagMap(field2Column + entityFullName);
		}
	}
	
	private boolean showSqlExecuteTime() {
		return HoneyConfig.getHoneyConfig().showSqlExecuteTime;
	}

	private void timerStart() {
		if (!showSqlExecuteTime()) return;
		long t = System.currentTimeMillis();
		OneTimeParameter.setAttribute(StringConst.TIMER_START, t);
	}

	private void timerEnd() {
		if (!showSqlExecuteTime()) return;
		// sharding时,还要加一个随机码?  没有在这分线程,是在SqlLib
		Long t = (Long) OneTimeParameter.getAttribute(StringConst.TIMER_START);
		if (t!=null && t > 0) {
			long spentTime = System.currentTimeMillis() - t;
			int min = HoneyConfig.getHoneyConfig().minSqlExecuteTime;
			if (spentTime >= min) Logger.logSQL("------ spent time(ms): " + spentTime);
		}
	}

}
