package gbc;

public class Common {

    static public short unsign(byte b) {
        if (b < 0) {
            return (short) (256 + b);
        } else {
            return b;
        }
    }

    public static final void setInt(byte[] b, int i, int v) {
        b[i++] = (byte) (v >> 24);
        b[i++] = (byte) (v >> 16);
        b[i++] = (byte) (v >> 8);
        b[i++] = (byte) (v);
    }

    public static final int getInt(byte[] b, int i) {
        int r = b[i++] & 0xFF;
        r = (r << 8) + (b[i++] & 0xFF);
        r = (r << 8) + (b[i++] & 0xFF);
        return (r << 8) + (b[i++] & 0xFF);
    }

    public static void showError(String message, String number, Exception ex) {
        new Exception(number + ":" + message == null ? "" : message, ex).printStackTrace(System.err);
    }

}
