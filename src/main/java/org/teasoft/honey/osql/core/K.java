/*
 * Copyright 2016-2020 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import org.teasoft.bee.osql.SqlKeyWord;

/**
 * @author Kingstar
 * @since  1.8.99
 */
public class K {
	
	private static SqlKeyWord k=BeeFactory.getHoneyFactory().getSqlKeyWord();
	
	public static String 
	select=k.select(),
	from=k.from(), 
	where=k.where(),
	insert=k.insert(),
	into=k.into(),
	values=k.values(),
	and=k.and(),
	or=k.or(),
	Null=k.Null(),
	isNull=k.isNull(),
	isNotNull=k.isNotNull(),
	
	update=k.update(),
	set=k.set(),
	delete=k.delete(),
	orderBy=k.orderBy(),
	count=k.count(),
	asc=k.asc(),
	
	on=k.on(),
//	forUpdate=k.forUpdate(),
	limit=k.limit(),
	offset=k.offset(),
	top=k.top(),
	
	space=k.space();
	

}
