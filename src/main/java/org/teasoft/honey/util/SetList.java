/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * 没有重复元素的ArrayList
 * @author Kingstar
 * @since  2.0
 */
public class SetList<E> extends ArrayList<E>{

	private static final long serialVersionUID = 1L;

	private Set<E> set;
	
	public SetList() {
		super();
		set=new HashSet<>();
	}
	
	public boolean add(E e) {
		
		boolean f = set.add(e);
		
		if (f)
			return super.add(e);
		else
			return false;
	}
	
}
