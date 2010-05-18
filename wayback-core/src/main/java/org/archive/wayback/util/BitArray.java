/* BitArray
 *
 * $Id$:
 *
 * Created on Apr 27, 2010.
 *
 * Copyright (C) 2006 Internet Archive.
 *
 * This file is part of Wayback.
 *
 * Wayback is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Wayback is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Wayback; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.archive.wayback.util;

/**
 * @author brad
 *
 */
public class BitArray {
	private static final int MASKS[] = {
		0x01,
		0x02,
		0x04,
		0x08,
		0x10,
		0x20,
		0x40,
		0x80
	};
	private static final int MASKSR[] = {
		~0x01,
		~0x02,
		~0x04,
		~0x08,
		~0x10,
		~0x20,
		~0x40,
		~0x80
	};
	private byte array[] = null;
	
	/**
	 * Construct a new BitArray holding at least n bits
	 * @param n number of bits to hold
	 */
	public BitArray(int n) {
		int bytes = n / 8;
		int bits = n % 8;
		if(bits > 0) {
			bytes++;
		}
		this.array = new byte[bytes];
	}
	/**
	 * Construct a new BitArray using argument as initial values.
	 * @param array byte array of initial values
	 */
	public BitArray(byte array[]) {
		this.array = array;
	}
	/**
	 * @return the byte array backing this bit array.
	 */
	public byte[] getBytes() {
		return array;
	}
	/**
	 * @param i index of bit to test
	 * @return true if the i'th bit is set, false otherwise
	 */
	public boolean get(int i) {
		int idx = i / 8;
		if(idx >= array.length) {
			throw new IndexOutOfBoundsException();
		}
		int bit = 7 - (i % 8);
		return ((array[idx] & MASKS[bit]) == MASKS[bit]);
	}
	/**
	 * set the i'th bit to 1 or 0
	 * @param i bit number to set
	 * @param value if true, the bit is set to 1, otherwise it is set to 0
	 */
	public void set(int i, boolean value) {
		int idx = i / 8;
		if(idx >= array.length) {
			throw new IndexOutOfBoundsException();
		}
		int bit = 7 - (i % 8);
		if(value) {
			array[idx] = (byte) (array[idx] | MASKS[bit]);
		} else {
			array[idx] = (byte) (array[idx] & MASKSR[bit]);
		}
	}
}
