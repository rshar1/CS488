package common;

import com.sun.istack.internal.NotNull;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class RUDPAddress {

  private InetAddress host;
  private int port;

  public RUDPAddress (@NotNull String address, int port) throws UnknownHostException {
    this.host = InetAddress.getByName(address);
    this.port = port;
  }

  public RUDPAddress (@NotNull InetAddress address, int port) {
    this.host = address;
    this.port = port;
  }

  public InetAddress getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RUDPAddress that = (RUDPAddress) o;

    if (port != that.port) {
      return false;
    }
    return host.equals(that.host);
  }

  @Override
  public int hashCode() {
    int result = host.hashCode();
    result = 31 * result + port;
    return result;
  }

  @Override
  public String toString() {
    return "RUDPAddress{" +
        "host=" + host +
        ", port=" + port +
        '}';
  }

}
