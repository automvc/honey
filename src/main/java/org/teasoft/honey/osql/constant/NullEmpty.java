package org.teasoft.honey.osql.constant;

public final class NullEmpty {

	private NullEmpty() {}

	public static final int NULL = 0;
	public static final int EMPTY_STRING = 1;
	public static final int NULL_AND_EMPTY_STRING = 2;

	public static final int EXCLUDE = -1; // exclude null, empty. null,empty两者都排除
}
