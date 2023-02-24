/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.util.List;

import org.teasoft.bee.osql.CommOperate;
import org.teasoft.bee.osql.Condition;
import org.teasoft.bee.osql.NameTranslate;
import org.teasoft.bee.osql.SuidType;
import org.teasoft.bee.osql.interccept.InterceptorChain;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class AbstractCommOperate implements CommOperate{
	
	//V1.11
	//全局的可以使用InterceptorChainRegistry配置;只是某个对象要使用,再使用对象配置
	protected InterceptorChain interceptorChain;
	protected String dsName;
	protected NameTranslate nameTranslate; //用于设置当前对象使用的命名转换器.使用默认的不需要设置
	
	public AbstractCommOperate() {
		System.out.println("============创建 AbstractCommOperate================"+this.toString());
	}
	
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
	public void setNameTranslate(NameTranslate nameTranslate) {
		this.nameTranslate=nameTranslate;
	}

	@Override
	public void setDataSourceName(String dsName) {
		System.err.println("================setDataSourceName=============:"+dsName+"     "+this.toString());
		this.dsName = dsName;
	}

	@Override
	public String getDataSourceName() {
		return dsName;
//		return Router.getDsName(); //不行. suid的dsName在执行时才通过拦截器设置.若提前通过线程设置,会因顺序原因,被覆盖.
	}
	
	void regCondition(Condition condition) {
		HoneyContext.setConditionLocal(condition);
	}
	
	void _doBeforePasreEntity(SuidType SuidType) {
		regSuidType(SuidType);
		if (this.dsName != null) {
			HoneyContext.setTempDS(dsName);
		}
		if(this.nameTranslate!=null) HoneyContext.setCurrentNameTranslate(nameTranslate);
	}

	void doBeforePasreEntity(Object entity, SuidType SuidType) {
		_doBeforePasreEntity(SuidType);
		getInterceptorChain().beforePasreEntity(entity, SuidType);
	}
	
	void doBeforePasreEntity(Object entityArray[], SuidType SuidType) {
		_doBeforePasreEntity(SuidType);
		getInterceptorChain().beforePasreEntity(entityArray, SuidType);
	}

	String doAfterCompleteSql(String sql) {
		//if change the sql,need update the context.
		sql = getInterceptorChain().afterCompleteSql(sql);
		return sql;
	}

	@SuppressWarnings("rawtypes")
	void doBeforeReturn(List list) {
		_doBeforeReturn();
		getInterceptorChain().beforeReturn(list);
	}
	
	void doBeforeReturn() {
		_doBeforeReturn();
		getInterceptorChain().beforeReturn();
	}
	
	private void _doBeforeReturn() {
		if (this.dsName != null) HoneyContext.removeTempDS();
		if(this.nameTranslate!=null) HoneyContext.removeCurrentNameTranslate();
	}
	
	protected void regSuidType(SuidType SuidType) {
		if (HoneyConfig.getHoneyConfig().isAndroid) HoneyContext.regSuidType(SuidType);
	}

}
