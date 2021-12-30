/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import org.teasoft.bee.osql.LowerKey;
import org.teasoft.bee.osql.SqlKeyWord;
import org.teasoft.bee.osql.UpperKey;

/**
 * @author Kingstar
 * @since  1.8.99
 */
public class K {
	
	private static SqlKeyWord k=getSqlKeyWord();
	
	private K() {}
	
	public static final String space=k.space();
	public static final String select=k.select();
	public static final String as=k.as();
	public static final String from=k.from(); 
	public static final String where=k.where();
	public static final String insert=k.insert();
	public static final String into=k.into();
	public static final String values=k.values();
	public static final String and=k.and();
	public static final String or=k.or();
	public static final String Null=k.Null();
	public static final String isNull=k.isNull();
	public static final String isNotNull=k.isNotNull();

	public static final String update=k.update();
	public static final String set=k.set();
	public static final String delete=k.delete();
	public static final String orderBy=k.orderBy();
	public static final String count=k.count();
	public static final String asc=k.asc();
	public static final String on=k.on();

	public static final String limit=k.limit();
	public static final String offset=k.offset();
	public static final String top=k.top();

	public static final String groupBy=k.groupBy();
	public static final String having=k.having();
	public static final String between=k.between();
	public static final String notBetween=k.notBetween();

	public static final String forUpdate=k.forUpdate();
	
	public static final String distinct=k.distinct();
	public static final String join=k.join();
	public static final String innerJoin=k.innerJoin();
	public static final String leftJoin=k.leftJoin();
	public static final String rightJoin=k.rightJoin();
	
	public static final String in=k.in();
	public static final String notIn=k.notIn();
	public static final String exists=k.exists();
	public static final String notExists=k.notExists();
	
	private static SqlKeyWord getSqlKeyWord() {
		if (HoneyUtil.isSqlKeyWordUpper())
			return new UpperKey();
		else
			return new LowerKey(); //default
	}

}
