/*
 * Copyright 2016-2022 the original author.All rights reserved.
 * Kingstar(honeysoft@126.com)
 * The license,see the LICENSE file.
 */

package org.teasoft.honey.osql.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.teasoft.bee.osql.Serializer;

/**
 * @author Kingstar
 * @since  1.11
 */
public class JdkSerializer implements Serializer {

	@Override
	public byte[] serialize(Object obj) {
		byte b[] = null;
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ObjectOutputStream objOut = new ObjectOutputStream(out);
			objOut.writeObject(obj);
			b = out.toByteArray();
		} catch (Exception e) {
		}
		return b;
	}

	@Override
	public Object unserialize(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		Object obj = null;
		try {
			ByteArrayInputStream input = new ByteArrayInputStream(bytes);
			ObjectInputStream objInput = new ObjectInputStream(input);
			obj = objInput.readObject();
		} catch (Exception e) {
		}
		return obj;
	}

}