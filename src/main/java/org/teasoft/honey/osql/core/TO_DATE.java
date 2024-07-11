/*
 * Copyright 2019-2024 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

/**
 * @author Kingstar
 * @since  2.4.0
 */
public class TO_DATE {

	private String datetimeValue;
	private String formatter;

	public TO_DATE() {}
	
	public TO_DATE(String datetimeValue, String formatter) {
		this.datetimeValue = datetimeValue;
		this.formatter = formatter;
	}

	public String getDatetimeValue() {
		return datetimeValue;
	}

	public void setDatetimeValue(String datetimeValue) {
		this.datetimeValue = datetimeValue;
	}

	public String getFormatter() {
		return formatter;
	}

	public void setFormatter(String formatter) {
		this.formatter = formatter;
	}

}
