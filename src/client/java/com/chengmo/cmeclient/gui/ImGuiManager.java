package com.chengmo.cmeclient.gui;

import imgui.ImGui;
import imgui.ImFontConfig;
import imgui.ImGuiIO;
import imgui.flag.ImGuiCol;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ImGuiManager {
    private static boolean initialized = false;
    public static boolean isInitialized() { return initialized; }

    public static void init() {
        if (initialized) return;
        try {
            InputStream in = ImGuiManager.class.getResourceAsStream("/natives/linux-arm64/libimgui-java64.so");
            if (in == null) return;
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "cmeclient_native");
            tempDir.mkdirs();
            File tempLib = new File(tempDir, "libimgui-java64.so");
            Files.copy(in, tempLib.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.load(tempLib.getAbsolutePath());
        } catch (Throwable e) {
            e.printStackTrace();
            return;
        }

        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();

        try (InputStream is = ImGuiManager.class.getResourceAsStream("/font/jianchenyuanti.ttf")) {
            if (is != null) {
                byte[] fontData = is.readAllBytes();
                ImFontConfig fontConfig = new ImFontConfig();
                // 还原全字库，前提是你重新编译的 .so 把 MAX_TEXTURE_SIZE 或者 ImFontAtlas 图集上限调大了
                io.getFonts().addFontFromMemoryTTF(fontData, 24.0f, fontConfig, io.getFonts().getGlyphRangesChineseFull());
                fontConfig.destroy();
            } else {
                io.getFonts().addFontDefault();
            }
        } catch (Exception e) {
            io.getFonts().addFontDefault();
        }
        io.getFonts().build();

        var style = ImGui.getStyle();
        style.setWindowRounding(12.0f);
        style.setChildRounding(10.0f);
        style.setFrameRounding(8.0f);
        style.setWindowPadding(0.0f, 0.0f);

        style.setColor(ImGuiCol.WindowBg, 0.15f, 0.08f, 0.25f, 0.95f);
        style.setColor(ImGuiCol.ChildBg, 0.15f, 0.08f, 0.25f, 0.50f);
        style.setColor(ImGuiCol.Text, 0.95f, 0.95f, 0.95f, 1.00f);
        style.setColor(ImGuiCol.FrameBg, 0.25f, 0.15f, 0.35f, 1.00f);
        style.setColor(ImGuiCol.Header, 0.85f, 0.65f, 0.13f, 0.80f);
        style.setColor(ImGuiCol.HeaderHovered, 0.95f, 0.75f, 0.23f, 0.80f);
        style.setColor(ImGuiCol.HeaderActive, 0.95f, 0.75f, 0.23f, 0.80f);

        initialized = true;
    }
}
