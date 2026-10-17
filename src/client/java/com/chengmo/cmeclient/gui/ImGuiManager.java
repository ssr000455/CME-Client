package com.chengmo.cmeclient.gui;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImFontConfig;
import imgui.flag.ImGuiCol;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ImGuiManager {
    private static boolean initialized = false;

    public static boolean isInitialized() {
        return initialized;
    }

    public static void init() {
        if (initialized) return;
        
        // 1. 加载并提取 ARM64 原生库
        loadNative();

        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.setConfigFlags(imgui.flag.ImGuiConfigFlags.NavEnableKeyboard);

        // 2. 从 resources 根目录的 font 加载字体
        try (InputStream is = ImGuiManager.class.getResourceAsStream("/font/jianchenyuanti.ttf")) {
            if (is != null) {
                byte[] fontData = is.readAllBytes();
                ImFontConfig fontConfig = new ImFontConfig();
                io.getFonts().addFontFromMemoryTTF(fontData, 16.0f, fontConfig, io.getFonts().getGlyphRangesChineseFull());
                fontConfig.destroy();
            } else {
                io.getFonts().addFontDefault();
            }
        } catch (Exception e) {
            e.printStackTrace();
            io.getFonts().addFontDefault();
        }
        io.getFonts().build();

        setupTheme();
        initialized = true;
    }

    private static void loadNative() {
        try {
            // 从 jar 包内提取 .so 到临时文件
            InputStream in = ImGuiManager.class.getResourceAsStream("/natives/linux-arm64/libimgui-java64.so");
            if (in == null) {
                throw new RuntimeException("找不到原生库 /natives/linux-arm64/libimgui-java64.so");
            }
            File tempLib = File.createTempFile("libimgui-java64", ".so");
            tempLib.deleteOnExit();
            Files.copy(in, tempLib.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            System.load(tempLib.getAbsolutePath());
            System.out.println("[CME Client] 原生库加载成功");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("原生库加载失败", e);
        }
    }

    private static void setupTheme() {
        var style = ImGui.getStyle();
        
        style.setWindowRounding(12.0f);
        style.setChildRounding(8.0f);
        style.setFrameRounding(6.0f);
        style.setPopupRounding(8.0f);
        style.setScrollbarRounding(10.0f);
        style.setGrabRounding(6.0f);
        style.setTabRounding(6.0f);

        style.setWindowPadding(12.0f, 12.0f);
        style.setFramePadding(10.0f, 6.0f);
        style.setItemSpacing(10.0f, 10.0f);

        // 皇家紫主题
        style.setColor(ImGuiCol.WindowBg, 0.08f, 0.06f, 0.10f, 0.98f); 
        style.setColor(ImGuiCol.ChildBg, 0.10f, 0.08f, 0.12f, 1.00f);
        style.setColor(ImGuiCol.Text, 0.92f, 0.90f, 0.96f, 1.00f);
        style.setColor(ImGuiCol.FrameBg, 0.15f, 0.12f, 0.18f, 1.00f);
        style.setColor(ImGuiCol.FrameBgHovered, 0.22f, 0.18f, 0.26f, 1.00f);
        style.setColor(ImGuiCol.FrameBgActive, 0.28f, 0.22f, 0.35f, 1.00f);
        style.setColor(ImGuiCol.CheckMark, 0.52f, 0.34f, 0.70f, 1.00f); 
        style.setColor(ImGuiCol.SliderGrab, 0.52f, 0.34f, 0.70f, 1.00f);
        style.setColor(ImGuiCol.SliderGrabActive, 0.62f, 0.44f, 0.80f, 1.00f);
        style.setColor(ImGuiCol.Header, 0.32f, 0.22f, 0.45f, 0.80f);
        style.setColor(ImGuiCol.HeaderHovered, 0.42f, 0.32f, 0.55f, 0.80f);
        style.setColor(ImGuiCol.HeaderActive, 0.52f, 0.34f, 0.70f, 0.80f);
    }
}
