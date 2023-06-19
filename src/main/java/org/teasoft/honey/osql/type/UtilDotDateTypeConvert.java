///*
// * Copyright 2016-2022 the original author.All rights reserved.
// * Kingstar(honeysoft@126.com)
// * The license,see the LICENSE file.
// */
//
//package org.teasoft.honey.osql.type;
//
//import java.util.Date;
//
//import org.teasoft.bee.osql.type.SetParaTypeConvert;
//
///**
// * convert java.util.Date to java.sql.Date
// * @author Kingstar
// * @since  1.11
// */
//public class UtilDotDateTypeConvert<T> implements SetParaTypeConvert<Date> {
//
//	@Override
//	public Object convert(Date value) { //会少了时分秒
//		java.sql.Date d= new java.sql.Date(((java.util.Date)value).getTime());
//		return d;
//	}
//
//}
