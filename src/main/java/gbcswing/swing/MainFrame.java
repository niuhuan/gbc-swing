package gbcswing.swing;

import gbc.Controller;
import gbc.GameBoy;
import gbc.ScreenListener;
import gbc.Palette;
import gbcswing.config.CheatItem;
import gbcswing.config.CheatConfig;
import gbcswing.config.SwingConfig;
import gbcswing.language.Language;
import gbcswing.tools.filter.*;
import gbcswing.tools.FileUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainFrame extends JFrame {

    // gameBoy screen size
    private static final int GB_WIDTH = 160;
    private static final int GB_HEIGHT = 144;

    // panel size + swing border size = window size

    // panel size
    private static int panelWidth;
    private static int panelHeight;

    // window border size
    private int borderWidth;
    private int borderHeight;

    // filters
    private _ImageFilter filter;
    private Map<String, _ImageFilter> filterMap;

    // virtual machine instance，Buffer frame information
    private GameBoy gameBoy;
    private final Controller controller = new Controller();
    private final ScreenListener screenListener = new ScreenListener() {
        @Override
        public void onFrameReady(BufferedImage image, int skipFrame) {
            gameBoyImage = image;
            gameBoySkipFrame = skipFrame;
            panel.repaint();
        }
    };
    private BufferedImage gameBoyImage;
    private int gameBoySkipFrame;

    // main panel
    private final Container panel;

    /* fps*/
    // font of fps
    private Font fpsFont;
    // fps counter
    private int[] previousTime = new int[16];
    private int previousTimeIx;

    // does machine need to continue to run?
    private boolean isRunning = false;

    // controller setting dialog
    private ControllerDialog controllerDialog;
    private CheatListDialog cheatListDialog;
    private JFileChooser jfc;
    private String lastLoadFile;

    public MainFrame() {
        super();
        // properties
        SwingConfig.loadProperties();
        SwingConfig.saveProperties();
        Language.internationalization(SwingConfig.language);
        // components
        controllerDialog = new ControllerDialog(this);
        cheatListDialog = new CheatListDialog(this) {
            @Override
            void onChange(List<CheatItem> cheatCodes) {
                if (gameBoy != null) {
                    CheatConfig.saveItems(lastLoadFile + ".cheats.xml", cheatCodes);
                    resetCheats(cheatCodes);
                }
            }
        };
        jfc = new JFileChooser();
        jfc.setFileFilter(new FileFilter() {
            public String getDescription() {
                return Language.CHOOSER;
            }

            public boolean accept(File arg0) {
                String name = arg0.getName().toLowerCase();
                return (arg0.isDirectory() || name.endsWith(".gbc")
                        || name.endsWith(".gb") || name.endsWith(".cgb") || name
                        .endsWith(".sgb"));
            }
        });
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.setCurrentDirectory(new File(SwingConfig.romFolder));

        // software title
        this.setTitle(Language.TITLE);

        // user can't resize window free
        this.setResizable(false);

        // menu bar
        JMenuBar menuBar = new JMenuBar();
        this.setJMenuBar(menuBar);

        // file menu
        JMenu fileMenu = new JMenu(Language.FILE);
        menuBar.add(fileMenu);

        // machine settings
        // open & close
        final JMenuItem openMenuItem = new JMenuItem(Language.OPEN);
        final JMenuItem closeMenuItem = new JMenuItem(Language.CLOSE);
        fileMenu.add(openMenuItem);
        fileMenu.add(closeMenuItem);
        fileMenu.addSeparator();
        // pause,resume,reset
        final JMenuItem pauseMenuItem = new JMenuItem(Language.PAUSE);
        fileMenu.add(pauseMenuItem);
        final JMenuItem resetMenuItem = new JMenuItem(Language.RESET);
        fileMenu.add(resetMenuItem);
        fileMenu.addSeparator();

        // quick save, quick load
        final JMenu quickSaveMenu = new JMenu(Language.QUICK_SAVE);
        final JMenuItem qs1 = new JMenuItem("1");
        final JMenuItem qs2 = new JMenuItem("2");
        final JMenuItem qs3 = new JMenuItem("3");
        final JMenuItem qs4 = new JMenuItem("4");
        final JMenuItem qs5 = new JMenuItem("5");
        fileMenu.add(quickSaveMenu);
        quickSaveMenu.add(qs1);
        quickSaveMenu.add(qs2);
        quickSaveMenu.add(qs3);
        quickSaveMenu.add(qs4);
        quickSaveMenu.add(qs5);
        final JMenu quickLoadMenu = new JMenu(Language.QUICK_LOAD);
        final JMenuItem ql1 = new JMenuItem("1");
        final JMenuItem ql2 = new JMenuItem("2");
        final JMenuItem ql3 = new JMenuItem("3");
        final JMenuItem ql4 = new JMenuItem("4");
        final JMenuItem ql5 = new JMenuItem("5");
        fileMenu.add(quickLoadMenu);
        quickLoadMenu.add(ql1);
        quickLoadMenu.add(ql2);
        quickLoadMenu.add(ql3);
        quickLoadMenu.add(ql4);
        quickLoadMenu.add(ql5);
        fileMenu.addSeparator();

        // exit
        final JMenuItem exitMenuItem = new JMenuItem(Language.EXIT);
        fileMenu.add(exitMenuItem);

        // settings
        JMenu settingMenu = new JMenu(Language.SETTINGS);
        menuBar.add(settingMenu);

        // language
        if (!Arrays.asList(Language.LANGUAGE_LIST).contains(SwingConfig.language)) {
            SwingConfig.language = Language.LANGUAGE_LIST[0];
        }
        JMenu languageMenu = new JMenu(Language.LANGUAGE);
        ButtonGroup languageMenuItemGroup = new ButtonGroup();
        final Runnable languageRunnable = new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(MainFrame.this, Language.LANGUAGE_RESTART, Language.LANGUAGE, JOptionPane.INFORMATION_MESSAGE);
            }
        };
        ActionListener languageMenuAc = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JRadioButtonMenuItem item = (JRadioButtonMenuItem) e.getSource();
                SwingConfig.language = item.getText();
                new Thread(languageRunnable).start();
            }
        };
        for (String country : Language.LANGUAGE_LIST) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(country);
            languageMenuItemGroup.add(item);
            languageMenu.add(item);
            if (country.equals(SwingConfig.language)) {
                item.setSelected(true);
            }
            item.addActionListener(languageMenuAc);
        }
        settingMenu.add(languageMenu);
        settingMenu.addSeparator();

        // speed
        final JMenu speedMenu = new JMenu(Language.SPEED);
        settingMenu.add(speedMenu);
        final JRadioButtonMenuItem speed1x = new JRadioButtonMenuItem("X1");
        final JRadioButtonMenuItem speed2x = new JRadioButtonMenuItem("X2");
        final JRadioButtonMenuItem speed4x = new JRadioButtonMenuItem("X4");
        final JRadioButtonMenuItem speed8x = new JRadioButtonMenuItem("X8");
        speedMenu.add(speed1x);
        speedMenu.add(speed2x);
        speedMenu.add(speed4x);
        speedMenu.add(speed8x);
        ButtonGroup speedGroup = new ButtonGroup();
        speedGroup.add(speed1x);
        speedGroup.add(speed2x);
        speedGroup.add(speed4x);
        speedGroup.add(speed8x);
        switch (SwingConfig.speed) {
            case 1:
                speed1x.setSelected(true);
                break;
            case 2:
                speed2x.setSelected(true);
                break;
            case 4:
                speed4x.setSelected(true);
                break;
            case 8:
                speed8x.setSelected(true);
                break;
        }
        settingMenu.addSeparator();

        // color style
        final JMenu colorStyleMenu = new JMenu(Language.COLOR_STYLE);
        settingMenu.add(colorStyleMenu);
        final JRadioButtonMenuItem style_menu_item_gbc = new JRadioButtonMenuItem("GBC");
        final JRadioButtonMenuItem style_menu_item_gbp = new JRadioButtonMenuItem("GBP");
        final JRadioButtonMenuItem style_menu_item_gb = new JRadioButtonMenuItem("GB");
        colorStyleMenu.add(style_menu_item_gbc);
        colorStyleMenu.add(style_menu_item_gbp);
        colorStyleMenu.add(style_menu_item_gb);
        ButtonGroup gbTypeGroup = new ButtonGroup();
        gbTypeGroup.add(style_menu_item_gbc);
        gbTypeGroup.add(style_menu_item_gbp);
        gbTypeGroup.add(style_menu_item_gb);
        if (SwingConfig.color_style.equals("GBP")) {
            style_menu_item_gbp.setSelected(true);
        } else if (SwingConfig.color_style.equals("GB")) {
            style_menu_item_gb.setSelected(true);
        } else {
            style_menu_item_gbc.setSelected(true);
        }

        // init filters
        filterMap = new LinkedHashMap<String, _ImageFilter>();
        filterMap.put(Language.NORMAL, new NormalFilter());
        filterMap.put(Language.NORMAL2X, new Normal2xFilter());
        filterMap.put(Language.SCALE2X, new Scale2xFilter());
        filterMap.put(Language.NEAREST2X, new Nearest2xFilter());
        filterMap.put(Language.HQ2X, new Hq2xFilter());
        filterMap.put(Language.NORMAL3X, new Normal3xFilter());
        filterMap.put(Language.SCALE3X, new Scale3xFilter());
        filterMap.put(Language.NEAREST3X, new Nearest3xFilter());
        filterMap.put(Language.HQ3X, new Hq3xFilter());
        filterMap.put(Language.NEAREST4X, new Nearest4xFilter());
        filterMap.put(Language.HQ4X, new Hq4xFilter());
        filter = filterMap.get(SwingConfig.filter);
        if (filter == null) { // use default filter if settings' filter is invalid.
            filter = filterMap.get(Language.NORMAL);
            SwingConfig.filter = Language.NORMAL;
        }
        final JMenu filterMenu = new JMenu(Language.FILTER);
        settingMenu.add(filterMenu);
        ButtonGroup filterGroup = new ButtonGroup();
        ActionListener filterActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String filterName = ((JRadioButtonMenuItem) e.getSource()).getText();
                filter = filterMap.get(filterName);
                SwingConfig.filter = filterName;
                resize();
            }
        };
        String f2 = null;
        for (String key : filterMap.keySet()) {
            if (f2 == null) {
                f2 = key.substring(0, 2);
            } else {
                String c2 = key.substring(0, 2);
                if (!f2.equals(c2)) {
                    f2 = c2;
                    filterMenu.addSeparator();
                }
            }
            final JRadioButtonMenuItem filterMenuItem = new JRadioButtonMenuItem(key);
            filterMenu.add(filterMenuItem);
            filterGroup.add(filterMenuItem);
            if (SwingConfig.filter.equals(key)) {
                filterMenuItem.setSelected(true);
            }
            filterMenuItem.addActionListener(filterActionListener);
        }

        // show fps
        JMenu showFpsMenu = new JMenu(Language.SHOW_FPS);
        settingMenu.add(showFpsMenu);
        final JMenuItem showFpsOnMenuItem = new JRadioButtonMenuItem(Language.ON);
        final JMenuItem showFpsOffMenuItem = new JRadioButtonMenuItem(Language.OFF);
        showFpsMenu.add(showFpsOnMenuItem);
        showFpsMenu.add(showFpsOffMenuItem);
        ButtonGroup showSwitchGroup = new ButtonGroup();
        showSwitchGroup.add(showFpsOnMenuItem);
        showSwitchGroup.add(showFpsOffMenuItem);
        if (SwingConfig.showFps) {
            showFpsOnMenuItem.setSelected(true);
        } else {
            showFpsOffMenuItem.setSelected(true);
        }

        settingMenu.addSeparator();

        // audio options
        final JMenu speakerMenu = new JMenu(Language.SPEAKER);
        settingMenu.add(speakerMenu);
        final JMenuItem speakerOnMenu = new JRadioButtonMenuItem(Language.ON);
        final JMenuItem speakerOffMenu = new JRadioButtonMenuItem(Language.OFF);
        speakerMenu.add(speakerOnMenu);
        speakerMenu.add(speakerOffMenu);
        ButtonGroup speakerSwitchGroup = new ButtonGroup();
        speakerSwitchGroup.add(speakerOnMenu);
        speakerSwitchGroup.add(speakerOffMenu);
        if (SwingConfig.enableSound) {
            speakerOnMenu.setSelected(true);
        } else {
            speakerOffMenu.setSelected(true);
        }
        final JMenu channelMenu = new JMenu(Language.CHANNEL);
        final JCheckBoxMenuItem channel1MenuItem = new JCheckBoxMenuItem("1");
        final JCheckBoxMenuItem channel2MenuItem = new JCheckBoxMenuItem("2");
        final JCheckBoxMenuItem channel3MenuItem = new JCheckBoxMenuItem("3");
        final JCheckBoxMenuItem channel4MenuItem = new JCheckBoxMenuItem("4");
        channel1MenuItem.setSelected(SwingConfig.channel1);
        channel2MenuItem.setSelected(SwingConfig.channel2);
        channel3MenuItem.setSelected(SwingConfig.channel3);
        channel4MenuItem.setSelected(SwingConfig.channel4);
        channelMenu.add(channel1MenuItem);
        channelMenu.add(channel2MenuItem);
        channelMenu.add(channel3MenuItem);
        channelMenu.add(channel4MenuItem);
        settingMenu.add(channelMenu);
        settingMenu.addSeparator();

        // controller type
        final JMenuItem controllerSetting = new JMenuItem(Language.CONTROLLER);
        settingMenu.add(controllerSetting);

        // The Game
        settingMenu.addSeparator();
        JMenu emulatorMenu = new JMenu(Language.CURRENT_GAME);
        settingMenu.add(emulatorMenu);
        final JMenuItem cheatsMenuItem = new JMenuItem(Language.CHEATS);
        emulatorMenu.add(cheatsMenuItem);

        // other menu
        JMenu otherMenu = new JMenu(Language.OTHERS);
        menuBar.add(otherMenu);
        final JMenuItem aboutMenuItem = new JMenuItem(Language.ABOUT);
        otherMenu.add(aboutMenuItem);

        // shortcuts
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke('O', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        // other listener

        // a thread of show about
        final Runnable aboutThread = new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(MainFrame.this, Language.ABOUT_CONTENT, Language.ABOUT, JOptionPane.INFORMATION_MESSAGE);
            }
        };

        // a thread of set controller
        final Runnable controllerThread = new Runnable() {
            @Override
            public void run() {
                controllerDialog.setVisible(true);
            }
        };

        // action listener
        ActionListener ac = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == openMenuItem) {
                    openFile();
                } else if (e.getSource() == closeMenuItem) {
                    closeGBC();
                } else if (e.getSource() == pauseMenuItem) {
                    isRunning = !isRunning;
                } else if (e.getSource() == resetMenuItem) {
                    if (gameBoy != null) {
                        startGBC();
                    }
                } else if (e.getSource() == qs1) {
                    quickSave(1);
                } else if (e.getSource() == qs2) {
                    quickSave(2);
                } else if (e.getSource() == qs3) {
                    quickSave(3);
                } else if (e.getSource() == qs4) {
                    quickSave(4);
                } else if (e.getSource() == qs5) {
                    quickSave(5);
                } else if (e.getSource() == ql1) {
                    quickLoad(1);
                } else if (e.getSource() == ql2) {
                    quickLoad(2);
                } else if (e.getSource() == ql3) {
                    quickLoad(3);
                } else if (e.getSource() == ql4) {
                    quickLoad(4);
                } else if (e.getSource() == ql5) {
                    quickLoad(5);
                } else if (e.getSource() == exitMenuItem) {
                    closeFrame();
                } else if (e.getSource() == speed1x) {
                    changeSpeed(1);
                } else if (e.getSource() == speed2x) {
                    changeSpeed(2);
                } else if (e.getSource() == speed4x) {
                    changeSpeed(4);
                } else if (e.getSource() == speed8x) {
                    changeSpeed(8);
                } else if (e.getSource() == style_menu_item_gbc) {
                    changeMode("GBC");
                } else if (e.getSource() == style_menu_item_gbp) {
                    changeMode("GBP");
                } else if (e.getSource() == style_menu_item_gb) {
                    changeMode("GB");
                } else if (e.getSource() == showFpsOnMenuItem) {
                    SwingConfig.showFps = true;
                } else if (e.getSource() == showFpsOffMenuItem) {
                    SwingConfig.showFps = false;
                } else if (e.getSource() == speakerOnMenu) {
                    SwingConfig.enableSound = true;
                    if (gameBoy != null) gameBoy.setSoundEnable(true);
                } else if (e.getSource() == speakerOffMenu) {
                    SwingConfig.enableSound = false;
                    if (gameBoy != null) gameBoy.setSoundEnable(false);
                } else if (e.getSource() == channel1MenuItem) {
                    SwingConfig.channel1 = channel1MenuItem.getState();
                    if (gameBoy != null) gameBoy.setChannelEnable(1, SwingConfig.channel1);
                } else if (e.getSource() == channel2MenuItem) {
                    SwingConfig.channel2 = channel2MenuItem.getState();
                    if (gameBoy != null) gameBoy.setChannelEnable(2, SwingConfig.channel2);
                } else if (e.getSource() == channel3MenuItem) {
                    SwingConfig.channel3 = channel3MenuItem.getState();
                    if (gameBoy != null) gameBoy.setChannelEnable(3, SwingConfig.channel3);
                } else if (e.getSource() == channel4MenuItem) {
                    SwingConfig.channel4 = channel4MenuItem.getState();
                    if (gameBoy != null) gameBoy.setChannelEnable(4, SwingConfig.channel4);
                } else if (e.getSource() == controllerSetting) {
                    new Thread(controllerThread).start();
                } else if (e.getSource() == aboutMenuItem) {
                    new Thread(aboutThread).start();
                } else if (e.getSource() == cheatsMenuItem) {
                    if (gameBoy != null) {
                        cheatListDialog.setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(MainFrame.this, Language.PLEASE_RUN_A_GAME);
                    }
                }
            }
        };

        openMenuItem.addActionListener(ac);
        closeMenuItem.addActionListener(ac);
        pauseMenuItem.addActionListener(ac);
        resetMenuItem.addActionListener(ac);
        qs1.addActionListener(ac);
        qs2.addActionListener(ac);
        qs3.addActionListener(ac);
        qs4.addActionListener(ac);
        qs5.addActionListener(ac);
        ql1.addActionListener(ac);
        ql2.addActionListener(ac);
        ql3.addActionListener(ac);
        ql4.addActionListener(ac);
        ql5.addActionListener(ac);
        exitMenuItem.addActionListener(ac);
        speed1x.addActionListener(ac);
        speed2x.addActionListener(ac);
        speed4x.addActionListener(ac);
        speed8x.addActionListener(ac);
        style_menu_item_gbc.addActionListener(ac);
        style_menu_item_gbp.addActionListener(ac);
        style_menu_item_gb.addActionListener(ac);
        showFpsOnMenuItem.addActionListener(ac);
        showFpsOffMenuItem.addActionListener(ac);
        speakerOnMenu.addActionListener(ac);
        speakerOffMenu.addActionListener(ac);
        channel1MenuItem.addActionListener(ac);
        channel2MenuItem.addActionListener(ac);
        channel3MenuItem.addActionListener(ac);
        channel4MenuItem.addActionListener(ac);
        controllerSetting.addActionListener(ac);
        cheatsMenuItem.addActionListener(ac);
        aboutMenuItem.addActionListener(ac);

        // save "sram" file when window closing
        WindowAdapter wa = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                closeFrame();
            }
        };
        this.setDefaultCloseOperation(HIDE_ON_CLOSE);
        this.addWindowListener(wa);

        // fps font
        fpsFont = new Font("", Font.BOLD, 16);

        // main panel
        panel = new JPanel() {
            public void paint(Graphics graphics) {
                super.paint(graphics);
                if (gameBoy != null && gameBoyImage != null) {
                    graphics.drawImage(filter == null ? gameBoyImage : filter.scale(gameBoyImage), 0, 0, panelWidth, panelHeight, null);
                    if (SwingConfig.showFps) {
                        int now = (int) System.currentTimeMillis();
                        graphics.setFont(fpsFont);
                        int eFps = ((32640 + now - previousTime[previousTimeIx]) / (now - previousTime[previousTimeIx])) >> 1;
                        previousTime[previousTimeIx] = now;
                        previousTimeIx = (previousTimeIx + 1) & 0x0F;
                        graphics.setColor(Color.RED);
                        String fps = eFps + " fps * " + (gameBoySkipFrame + 1);
                        graphics.drawString(fps, 20, 20);
                    }
                } else {
                    graphics.fillRect(0, 0, panelWidth, panelHeight);
                }
            }
        };
        panel.setBackground(Color.BLACK);

        // key listener
        panel.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (gameBoy != null && gameBoy.running()) {
                    for (int i = 0; i < SwingConfig.keyCodes.length; i++) {
                        if (SwingConfig.keyCodes[i] == e.getKeyCode())
                            controller.buttonDown(i);
                    }
                }
            }

            public void keyReleased(KeyEvent e) {
                if (gameBoy != null && gameBoy.running()) {
                    for (int i = 0; i < SwingConfig.keyCodes.length; i++) {
                        if (SwingConfig.keyCodes[i] == e.getKeyCode())
                            controller.buttonUp(i);
                    }
                }
            }
        });

        // auto pause or resume when focus changed
        panel.setFocusable(true);
        panel.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                if (isRunning) {
                    resumeGBC();
                }
            }

            public void focusLost(FocusEvent e) {
                if (isRunning) {
                    pauseGBC();
                }
            }
        });

        // swing frame location
        {
            Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
            int width = (int) screensize.getWidth();
            int height = (int) screensize.getHeight();
            if (SwingConfig.x > width - 100 || SwingConfig.y > height - 100) {
                SwingConfig.x = 300;
                SwingConfig.y = 300;
            }
        }

        // calculate swing borders size
        this.getContentPane().add(panel);
        this.setBounds(SwingConfig.x, SwingConfig.y, 300, 300);
        this.setVisible(true);
        borderWidth = 300 - (panel.getWidth());
        borderHeight = 300 - (panel.getHeight());

        // resize the window according to the size of the border
        resize();
        this.setVisible(false);
    }

    private void resize() {
        int scaleFactor = filter == null ? 1 : filter.getScaleFactor();
        panelWidth = GB_WIDTH * scaleFactor;
        panelHeight = GB_HEIGHT * scaleFactor;
        this.setSize(panelWidth + borderWidth, panelHeight + borderHeight);
    }

    private void openFile() {
        jfc.showOpenDialog(MainFrame.this);
        File loadFile = jfc.getSelectedFile();
        if (loadFile == null) {
            return;
        } else {
            SwingConfig.romFolder = loadFile.getParentFile().getAbsolutePath();
            SwingConfig.saveProperties();
            jfc.setSelectedFile(null);
            lastLoadFile = loadFile.getAbsolutePath();
            startGBC();
        }
    }

    private void closeFrame() {
        this.setVisible(false);
        this.dispose();
        closeGBC();
        SwingConfig.x = this.getX();
        SwingConfig.y = this.getY();
        SwingConfig.saveProperties();
        System.exit(0);
    }


    private void quickSave(int i) {
        if (gameBoy != null && lastLoadFile != null) {
            File saveFile = new File(lastLoadFile + "." + i + ".sav");
            FileUtils.writeFile(saveFile.getAbsolutePath(), gameBoy.flatten());
        }
    }

    private void quickLoad(int i) {
        if (gameBoy != null && lastLoadFile != null) {
            File saveFile = new File(lastLoadFile + "." + i + ".sav");
            if (saveFile.exists()) {
                byte[] bs = FileUtils.readFile(saveFile.getAbsolutePath());
                if (bs == null) return;
                gameBoy.unflatten(bs);
            }
        }
    }

    private void changeSpeed(final int speed) {
        new Thread() {
            @Override
            public void run() {
                SwingConfig.speed = speed;
                SwingConfig.saveProperties();
                if (gameBoy != null) {
                    gameBoy.setSpeed(speed);
                }
            }
        }.start();
    }

    private void changeMode(final String mode) {
        if (!mode.equals(SwingConfig.color_style)) {
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    SwingConfig.color_style = mode;
                    if (gameBoy != null) {
                        int option = JOptionPane.showConfirmDialog(MainFrame.this, Language.NEED_RESET_SAVE_NOT_COMPATIBLE, Language.NEED_RESET_TITLE, JOptionPane.OK_CANCEL_OPTION);
                        if (option == JOptionPane.OK_OPTION) {
                            if (gameBoy != null && lastLoadFile != null) {
                                closeGBC();
                                startGBC();
                            }
                        }
                    } else {
                        JOptionPane.showMessageDialog(MainFrame.this, Language.SAVE_NOT_COMPATIBLE);
                    }
                }
            }.start();
        }
    }

    private void resetCheats(List<CheatItem> cheats) {
        gameBoy.remoteAllCheat();
        for (int i = 0; i < cheats.size(); i++) {
            CheatItem cheatItem = cheats.get(i);
            if (cheats.get(i).enabled) {
                for (String code : cheatItem.codes) {
                    gameBoy.setCheat(code);
                }
            }
        }
    }

    private void startGBC() {
        closeGBC();

        byte[] cartridgeBin = FileUtils.readFile(lastLoadFile);
        if (cartridgeBin == null) return;

        String sRamFile = lastLoadFile + ".sram.sav";
        byte[] sRamBin = null;
        if (new File(sRamFile).exists())
            sRamBin = FileUtils.readFile(sRamFile);

        List<CheatItem> cheatCodes = CheatConfig.loadItems(lastLoadFile + ".cheats.xml");
        cheatListDialog.resetModel(cheatCodes);

        boolean gbc;
        int[] colors;
        if (SwingConfig.color_style.equals("GB")) {
            gbc = false;
            colors = Palette.GB;
        } else if (SwingConfig.color_style.equals("GBP")) {
            gbc = false;
            colors = Palette.GBP;
        } else {
            gbc = true;
            colors = Palette.GB;
        }

        gameBoy = new GameBoy(gbc, colors, cartridgeBin, controller, screenListener);
        gameBoy.setSoundEnable(SwingConfig.enableSound);
        gameBoy.setChannelEnable(1, SwingConfig.channel1);
        gameBoy.setChannelEnable(2, SwingConfig.channel2);
        gameBoy.setChannelEnable(3, SwingConfig.channel3);
        gameBoy.setChannelEnable(4, SwingConfig.channel4);
        if (sRamBin != null) {
            gameBoy.setSarm(sRamBin);
        }
        resetCheats(cheatCodes);
        gameBoy.setSpeed(SwingConfig.speed);
        isRunning = true;
    }

    private void closeGBC() {
        if (gameBoy != null) {
            pauseGBC();
            isRunning = false;
            gameBoy = null;
        }
        panel.repaint();
    }

    private void pauseGBC() {
        if (gameBoy != null && gameBoy.running()) {
            gameBoy.shutdown();
            byte[] sram = gameBoy.sarm();
            if (sram != null) {
                FileUtils.writeFile(lastLoadFile + ".sram.sav", sram);
            }
        }
    }

    private void resumeGBC() {
        if (gameBoy != null && !gameBoy.running()) {
            gameBoy.startup();
        }
    }

}
