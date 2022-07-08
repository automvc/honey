/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.distribution;

import org.teasoft.bee.distribution.GenId;
import org.teasoft.honey.util.IntSerialId;

/**
 * 
 * 组成:1位符号位+22位时间(分)+9位序列号
 * 每分钟可出512个int序列号,可透支两分钟(共512*3). 约可用7.98年
 * 本身只支持单点出号.
 * 注意:本类虽然是返回long型的id,但它的值是在int类型范围内.
 * 
 * @author Kingstar
 * @since  1.17
 */
public class IntSerialIdReturnLong implements GenId {

	private IntSerialId intSerialId = new IntSerialId();

	public IntSerialIdReturnLong() {}

	@Override
	public synchronized long get() {
		return (long) intSerialId.get();
	}

	/**
	 * 批量获取int类型id.sizeOfIds通常不应该大于1536
	 * @param sizeOfIds
	 * @return
	 */
	@Override
	public synchronized long[] getRangeId(int sizeOfIds) {
		int temp[] = intSerialId.getRangeId(sizeOfIds);
		long r[] = new long[2];
		r[0] = temp[0];
		r[1] = temp[1];
		return r;
	}

}
