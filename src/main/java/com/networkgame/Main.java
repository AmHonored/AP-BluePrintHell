package com.networkgame;

import com.networkgame.controller.GameController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.StdCallLibrary;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Set JavaFX properties for optimal performance
            System.setProperty("javafx.animation.fullspeed", "true");
            System.setProperty("prism.vsync", "false");
            System.setProperty("javafx.animation.pulse", "60");
            
            // Only minimize other applications if explicitly requested via command line
            // This prevents issues with background applications
            if (getParameters().getNamed().containsKey("minimizeOthers")) {
                minimizeOtherApplications();
            }
            
            GameController gameController = new GameController();
            Scene mainMenuScene = gameController.createMainMenuScene();
            
            // Load CSS stylesheet
            String cssPath = getClass().getResource("/css/style.css").toExternalForm();
            mainMenuScene.getStylesheets().add(cssPath);
            
            primaryStage.setTitle("Blueprint Hell");
            primaryStage.setScene(mainMenuScene);
            
            // Make window non-resizable, non-minimizable, and non-closable as per requirements
            primaryStage.setResizable(false);
            primaryStage.setMaximized(false);
            primaryStage.initStyle(StageStyle.UNDECORATED);
            
            // Prevent Alt+F4 and other system close requests
            primaryStage.setOnCloseRequest(event -> {
                event.consume(); // Prevent default close action
            });
            
            primaryStage.show();
            
            // Set the game controller's primary stage for future reference
            gameController.setPrimaryStage(primaryStage);
            
            // Bring this application to front
            primaryStage.toFront();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Windows-specific interface for minimizing applications
     */
    public interface User32Extra extends StdCallLibrary {
        User32Extra INSTANCE = Native.load("user32", User32Extra.class);
        boolean EnumWindows(WinUser.WNDENUMPROC lpEnumFunc, Pointer userData);
        boolean IsWindowVisible(HWND hWnd);
        int GetWindowTextA(HWND hWnd, byte[] lpString, int nMaxCount);
        boolean ShowWindow(HWND hWnd, int nCmdShow);
        
        int SW_MINIMIZE = 6;
    }
    
    /**
     * Minimizes all other applications running on the system
     * Note: This feature should be used with caution as it may affect other applications
     */
    private void minimizeOtherApplications() {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                User32Extra.INSTANCE.EnumWindows((hWnd, data) -> {
                    try {
                        if (User32Extra.INSTANCE.IsWindowVisible(hWnd)) {
                            byte[] windowText = new byte[512];
                            User32Extra.INSTANCE.GetWindowTextA(hWnd, windowText, 512);
                            String windowTitle = Native.toString(windowText);
                            
                            // Skip windows with empty titles and our own window
                            if (!windowTitle.isEmpty() && !windowTitle.equals("Blueprint Hell")) {
                                User32Extra.INSTANCE.ShowWindow(hWnd, User32Extra.SW_MINIMIZE);
                            }
                        }
                        return true;
                    } catch (Exception e) {
                        // Gracefully handle any errors during window enumeration
                        System.err.println("Error while minimizing window: " + e.getMessage());
                        return true; // Continue enumeration despite errors
                    }
                }, null);
            }
        } catch (Exception e) {
            // Catch any exceptions from the JNA interface
            System.err.println("Failed to minimize other applications: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
} 