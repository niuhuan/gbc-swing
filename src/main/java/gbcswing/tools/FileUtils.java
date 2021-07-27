package gbcswing.tools;

import java.io.*;

public class FileUtils {

    // 保存文件
    public static void writeFile(String s, byte[] buff) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(s);
            fos.write(buff);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 读取文件
    public static byte[] readFile(String path) {
        File loadFile = new File(path);
        if (loadFile.exists()) {
            byte[] bin = new byte[(int) loadFile.length()];
            int total = (int) loadFile.length();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(loadFile);
                while (total > 0) {
                    total -= fis.read(bin, (int) (loadFile.length() - total), total);
                }
                return bin;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fis!=null){
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

}
