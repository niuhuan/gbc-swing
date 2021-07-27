package gbcswing.config;

import gbcswing.language.Language;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;

// configs
public class SwingConfig {

    public static boolean showFps = false;
    public static int[] keyCodes = new int[]{'D', 'A', 'W', 'S', 'K', 'J', 'F', 'H'};
    public static String romFolder = "./";

    public static boolean enableSound = true;
    public static boolean channel1 = true;
    public static boolean channel2 = true;
    public static boolean channel3 = true;
    public static boolean channel4 = true;

    public static String color_style = "GBC";

    public static int x = 200;
    public static int y = 200;
    public static String filter = Language.NORMAL2X;

    public static String language = Language.LANGUAGE_LIST[0];

    public static int speed = 1;

    private static void loadProperties(InputStream inputStream) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();
            try {
                Field field = SwingConfig.class.getDeclaredField(key);
                Class<?> type = field.getType();
                Object valueObj = null;
                if (type == int[].class) {
                    String[] stringValue = value.split(",");
                    int[] intValue = new int[stringValue.length];
                    for (int i = 0; i < intValue.length; i++) {
                        intValue[i] = Integer.parseInt(stringValue[i]);
                    }
                    valueObj = intValue;
                } else if (type == int.class) {
                    valueObj = Integer.parseInt(value);
                } else if (type == String.class) {
                    valueObj = value.toString();
                } else if (type == boolean.class) {
                    valueObj = Boolean.parseBoolean(value.toString());
                }
                field.set(null, valueObj);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private static void saveProperties(OutputStream outputStream) throws IOException {
        Field[] fields = SwingConfig.class.getDeclaredFields();
        Properties properties = new Properties();
        for (Field field : fields) {
            Class<?> type = field.getType();
            String name = field.getName();
            String valueObj = null;
            try {
                if (type == int[].class) {
                    int[] intValue = (int[]) field.get(null);
                    String string = "";
                    for (int i = 0; i < intValue.length; i++) {
                        if (i > 0) {
                            string += ",";
                        }
                        string += intValue[i];
                    }
                    valueObj = string;
                } else if (type == int.class) {
                    valueObj = String.valueOf(field.get(null));
                } else if (type == String.class) {
                    valueObj = (String) field.get(null);
                } else if (type == boolean.class) {
                    valueObj = Boolean.toString((Boolean) field.get(null));
                }
                properties.put(name, valueObj);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        properties.store(outputStream, String.valueOf(System.currentTimeMillis()));
    }

    public static void loadProperties() {
        File file = configFile();
        if (file.exists()) {
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(configFile());
                loadProperties(fileInputStream);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void saveProperties() {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(configFile());
            saveProperties(fileOutputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static File configFile() {
        return new File("GameBoy.properties");
    }

}
