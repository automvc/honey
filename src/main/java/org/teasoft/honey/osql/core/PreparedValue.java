package org.teasoft.honey.osql.core;

import java.io.Serializable;
import java.lang.reflect.Field;

import org.teasoft.honey.osql.util.AnnoUtil;

/**
 * 用于预编译设置参数的结构.struct for set PreparedStatement parameter.
 * @author Kingstar
 * @since  1.0
 */
class PreparedValue implements Serializable {

	private static final long serialVersionUID = 1592803913606L;

	private String type;
	private Object value;
	// 用于识别设置PreparedStatement json参数的类型,日志输出/缓存用到的参数值.
//	private transient Field field;//V1.11    没有序列化到, 是否有影响?  已改用 jsonType    close on 2.4.0

	// 0: not json, 1:json, 2:bjson
	// 设置参数时才要区分,查询返回值处理,不需要.
	private int jsonType = 0; // 2.4.0

	public PreparedValue() {}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

//	public Field getField() { //close on 2.4.0
//		return field;
//	}

	/**
	 * set field
	 * @param field
	 * @since 1.11
	 */
	public void setField(Field field) {

		if (AnnoUtil.isJustJsonb(field))
			this.jsonType = 2;
		else
			this.jsonType = 1;

//		this.field = field; //close on 2.4.0
	}

	public int getJsonType() {
		return jsonType;
	}

}
