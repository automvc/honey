/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.sharding.engine.decorate;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.teasoft.bee.sharding.ShardingSortStruct;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.util.ObjectUtils;

/**
 * @author AiTeaSoft
 * @since  2.0
 */
public class CompareResult implements Comparable<CompareResult> {

	private Object orderValues[]; // 利用排序结构,解析排序字段,放入
	private final ResultSet resultSet;
	private ShardingSortStruct struct;
	private boolean has = false; 

	public CompareResult(ResultSet resultSet, ShardingSortStruct struct) {
		this.resultSet = resultSet;
		this.struct = struct;
		initOrderValues();
	}
	
	public boolean hasNext() { //may be  .可能有下一元素,则当前一定有元素.
		return has;
	}

	private void initOrderValues() {
		try {
			if (this.resultSet.next()) {  //不能是while
				has = true;
				if (struct != null && struct.getOrderFields() != null) {
					this.orderValues = new Object[struct.getOrderFields().length];
					for (int k = 0; k < struct.getOrderFields().length; k++) {
//						if(k==0) this.orderValues = new Object[struct.getOrderFields().length];
						this.orderValues[k] = this.resultSet.getObject(struct.getOrderFields()[k]); // TODO 转字段
					   System.err.println(this.orderValues[k]);
					} // end for
				}
			} // end if next
		} catch (SQLException e) {
			Logger.debug(e.getMessage(), e);
		}
	}

//	这里要定义比较器.   可以将比较结构传入.
	@Override
	public int compareTo(final CompareResult other) {
		if (struct == null) return 0;
		if(!this.hasNext()) return -1;
		if(!other.hasNext()) return -1;  //放在队头, 会先处理,再次取出时,不符合,则可 减少排列元素数量.
//		if(!other.hasNext() && !this.hasNext()) return 0;
		
		for (int i = 0; i < orderValues.length; i++) {
			int result = CompareUtil.compareTo(ObjectUtils.string(this.orderValues[i]), ObjectUtils.string(other.orderValues[i]), struct, i);
			if (0 != result) {
				return result;
			}
		}

		return 0;
	}
	
	public ResultSet getResultSet() {
		return resultSet;
	}

	public Object[] getOrderValues() {
		return orderValues;
	}

	public void setOrderValues(Object[] orderValues) {
		this.orderValues = orderValues;
	}

	public ShardingSortStruct getStruct() {
		return struct;
	}
	
}
