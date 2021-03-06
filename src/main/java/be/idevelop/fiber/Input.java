/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Steven Willems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package be.idevelop.fiber;

import java.nio.ByteBuffer;

import static be.idevelop.fiber.ReferenceResolver.REFERENCE_RESOLVER;

public final class Input {

    private static final byte ONE_BYTE = (byte) 0x1;

    private final SerializerConfig config;

    private final ByteBuffer byteBuffer;

    private final IntType classIntType;

    Input(SerializerConfig config, ByteBuffer byteBuffer) {
        this.config = config;
        this.byteBuffer = (ByteBuffer) byteBuffer.mark();
        this.classIntType = config.getIntTypeForClassId();
    }

    @SuppressWarnings("unchecked")
    public <T> T read() {
        return ((Serializer<T>) config.getSerializer(readClassId())).read(this);
    }

    public short readShort() {
        return byteBuffer.getShort();
    }

    public int readInteger() {
        return byteBuffer.getInt();
    }

    public byte readByte() {
        return byteBuffer.get();
    }

    public byte[] readBytes(int length) {
        byte[] buffer = new byte[length];
        byteBuffer.get(buffer, 0, length);
        return buffer;
    }

    public char readChar() {
        return byteBuffer.getChar();
    }

    public float readFloat() {
        return byteBuffer.getFloat();
    }

    public double readDouble() {
        return byteBuffer.getDouble();
    }

    public long readLong() {
        return byteBuffer.getLong();
    }

    public boolean readBoolean() {
        return (readByte() == 1);
    }

    public Class readClass() {
        return config.getClassForId(readClassId());
    }

    private short readClassId() {
        switch (classIntType) {
            case BYTE:
                return byteBuffer.get();
            default:
                return byteBuffer.getShort();
        }
    }

    public String readString() {
        short length = readShort();
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            byte b = readByte();
            if (b == ONE_BYTE) {
                chars[i] = (char) readShort();
            } else {
                chars[i] = (char) (b & 0xff);
            }
        }
        String s = new String(chars);
        REFERENCE_RESOLVER.addForDeserialize(s);
        return s;

    }

}
