package org.teasoft.honey.osql.core;

/**
 * @author Kingstar
 * @since  1.0
 */
class PreparedValue {

	private String type;
	private Object value;

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
}
