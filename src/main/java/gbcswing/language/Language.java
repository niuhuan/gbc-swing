package gbcswing.language;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class Language {

    public static String ENABLE;
    public static String CODE;
    public static String DESCRIPTION;
    public static String CANCEL;
    public static String TITLE;
    public static String CHOOSER;
    public static String FILE;
    public static String OPEN;
    public static String CLOSE;
    public static String PAUSE;
    public static String RESET;
    public static String QUICK_SAVE;
    public static String QUICK_LOAD;
    public static String EXIT;
    public static String SETTINGS;
    public static String SHOW_FPS;
    public static String LANGUAGE;
    public static String LANGUAGE_RESTART;
    public static String ON;
    public static String OFF;
    public static String FILTER;
    public static String NORMAL;
    public static String NORMAL2X;
    public static String NORMAL3X;
    public static String HQ2X;
    public static String HQ3X;
    public static String HQ4X;
    public static String NEAREST2X;
    public static String NEAREST3X;
    public static String NEAREST4X;
    public static String SCALE2X;
    public static String SCALE3X;
    public static String SPEAKER;
    public static String CHANNEL;
    public static String CONTROLLER;
    public static String COLOR_STYLE;
    public static String NEED_RESET_TITLE;
    public static String NEED_RESET_SAVE_NOT_COMPATIBLE;
    public static String SAVE_NOT_COMPATIBLE;
    public static String OTHERS;
    public static String ABOUT;
    public static String ABOUT_CONTENT;
    public static String KEY_CODES;
    public static String OK;
    public static String CURRENT_GAME;
    public static String CHEATS;
    public static String PLEASE_RUN_A_GAME;
    public static String SPEED;
    public static String ADD;
    public static String REMOVE_SELECTED;
    public static String EDIT_SELECTED;
    public static String SWITCH_SELECTED;

    public static String[] LANGUAGE_LIST = new String[]{
            "DEFAULT", "en", "zh",
    };

    static {
        internationalization(LANGUAGE_LIST[1]);
        internationalization(null);
    }

    public static void internationalization(String language) {
        if (language == null || language.equals(LANGUAGE_LIST[0])) {
            language = Locale.getDefault().getLanguage();
        }
        InputStream is = null;
        try {
            is = resource(languagePath(language));
            if (is == null)
                is = resource(languagePath(LANGUAGE_LIST[1]));
            Properties properties = new Properties();
            properties.load(is);
            for (Map.Entry entry : properties.entrySet()) {
                String fieldName = String.valueOf(entry.getKey());
                try {
                    Field field = Language.class.getDeclaredField(fieldName);
                    if (field != null) {
                        field.set(null, String.valueOf(entry.getValue()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private static String languagePath(String name) {
        return "/gbcswing/language/properties/" + name + ".properties";
    }

    private static InputStream resource(String path) {
        return Language.class.getResourceAsStream(path);
    }

}
