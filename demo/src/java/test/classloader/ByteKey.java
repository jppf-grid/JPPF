package test.classloader;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: jandam
 * Date: Aug 30, 2011
 * Time: 8:59:52 AM
 * To change this template use File | Settings | File Templates.
 */
public class ByteKey implements Serializable {
  private static final long serialVersionUID = 3660453399774816208L;

  private final byte[] data;
  private final int hashCode;

  public ByteKey(final byte[] data) {
    if(data == null) throw new IllegalArgumentException("data is null");

    this.data = data;
    this.hashCode = Arrays.hashCode(this.data);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof ByteKey)) return false;

    ByteKey byteKey = (ByteKey) o;

    return hashCode == byteKey.hashCode && Arrays.equals(data, byteKey.data);
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  public byte[] getData() {
    return data;
  }

  public String toHex() {
    if(data == null) return null;
    StringBuilder sb = new StringBuilder();
    for (byte item : data) {
      sb.append(Integer.toHexString((item & 0xf0) >> 4));
      sb.append(Integer.toHexString(item & 0xf));
    }

    return sb.toString();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("ByteKey");
    sb.append("{data=").append(toHex());
    sb.append('}');
    return sb.toString();
  }
}

