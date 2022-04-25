/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.name;

import org.teasoft.bee.osql.NameTranslate;
import org.teasoft.bee.osql.Registry;
import org.teasoft.bee.osql.annotation.customizable.ColumnHandler;
import org.teasoft.honey.osql.core.NameTranslateHandle;

/**
 * 命令转换注册器.Name Translate Registry.
 * @author Kingstar
 * @since  1.11
 */
public class NameRegistry implements Registry {
	
    /**
     * 指定命名转换实现类.Specifies the name translate implementation class.
     * 设置自定义命名转化类.for set customer naming.
     * @param nameTranslate name translate
     */
	public static void registerNameTranslate(NameTranslate nameTranslate) {
		NameTranslateHandle.setNameTranslate(nameTranslate);
	}
	
	/**
	 * 指定列名命名转换处理器(默认转换无需设置).Specifies the column naming conversion handler(no setting required for default conversion)
	 * @param columnHandler 列名命名转换处理器.column naming conversion handler
	 */
	public static void registerColumnHandler(ColumnHandler columnHandler) {
		NameTranslateHandle.setColumnHandler(columnHandler);
	}

}
