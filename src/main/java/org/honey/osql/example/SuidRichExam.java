/*
 * Copyright 2016-2019 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.honey.osql.example;

import org.bee.osql.BeeException;
import org.bee.osql.SuidRich;
import org.honey.osql.core.BeeFactory;
import org.honey.osql.example.entity.User;

/**
 * @author Kingstar
 * @since  1.0
 */
public class SuidRichExam {

	public static void main(String[] args) {
		try {
			SuidRich suidRich = BeeFactory.getHoneyFactory().getSuidRich();
			suidRich.selectById(new User(), "3,4,5");
			suidRich.selectById(new User(), 3);
			suidRich.selectById(new User(), 3L);
		} catch (BeeException e) {
			e.printStackTrace();
		}
	}

}
