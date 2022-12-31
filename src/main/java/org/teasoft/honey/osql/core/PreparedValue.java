package org.teasoft.honey.osql.core;

import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * 用于预编译设置参数的结构.struct for set PreparedStatement parameter.
 * @author Kingstar
 * @since  1.0
 */
class PreparedValue implements Serializable {
//	class PreparedValue {

//	private static final long serialVersionUID = 1592803913604L;
	
	private String type;
	private Object value;
	private transient Field field;//V1.11    没有序列化到, 是否有影响???
	
	public PreparedValue(){}
	
//	public PreparedValue(String type, Object value) {
//		this.type = type;
//		this.value = value;
//	}

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

	/**
	 * get field
	 * @return
	 * @since 1.11
	 */
	public Field getField() {
		return field;
	}

	/**
	 * set field
	 * @param field
	 * @since 1.11
	 */
	public void setField(Field field) {
		this.field = field;
	}
	
}
