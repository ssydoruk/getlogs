/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package com.myutils.getlogs;

import Utils.UnixProcess.ExtProcess;
import java.awt.Window;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author ssydoruk
 */
public class CommandExecutorTest {
    
    public CommandExecutorTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
    }
    
    @AfterEach
    public void tearDown() {
    }

    /**
     * Test of getDs method, of class CommandExecutor.
     */
    @Test
    public void testGetDs() {
        System.out.println("getDs");
        CommandExecutor instance = null;
        DownloadSettings expResult = null;
        DownloadSettings result = instance.getDs();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setDs method, of class CommandExecutor.
     */
    @Test
    public void testSetDs() {
        System.out.println("setDs");
        DownloadSettings ds = null;
        CommandExecutor instance = null;
        instance.setDs(ds);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of executeCmd method, of class CommandExecutor.
     */
    @Test
    public void testExecuteCmd_Window() throws Exception {
        System.out.println("executeCmd");
        Window parent = null;
        CommandExecutor instance = null;
        instance.executeCmd(parent);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of executeCmd method, of class CommandExecutor.
     */
    @Test
    public void testExecuteCmd_3args() throws Exception {
        System.out.println("executeCmd");
        AppProfile appProfile = null;
        App ap = null;
        boolean isLFMT = false;
        CommandExecutor instance = null;
        instance.executeCmd(appProfile, ap, isLFMT);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getStorage method, of class CommandExecutor.
     */
    @Test
    public void testGetStorage() {
        System.out.println("getStorage");
        AppProfile appProfile = null;
        App ap = null;
        HostAppdir theAppHost = null;
        String logsDir = "";
        boolean lfmt = false;
        boolean lcaLog = false;
        CommandExecutor instance = null;
        SavedSearchStorage expResult = null;
        SavedSearchStorage result = instance.getStorage(appProfile, ap, theAppHost, logsDir, lfmt, lcaLog);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of uncheckNonPrimary method, of class CommandExecutor.
     */
    @Test
    public void testUncheckNonPrimary() {
        System.out.println("uncheckNonPrimary");
        CommandExecutor instance = null;
        ExtProcess.ExecutionResult expResult = null;
        ExtProcess.ExecutionResult result = instance.uncheckNonPrimary();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setSettingsFile method, of class CommandExecutor.
     */
    @Test
    public void testSetSettingsFile() {
        System.out.println("setSettingsFile");
        String sGUIProfile = "";
        CommandExecutor instance = null;
        instance.setSettingsFile(sGUIProfile);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of pasteFiles method, of class CommandExecutor.
     */
    @Test
    public void testPasteFiles() {
        System.out.println("pasteFiles");
        Window parent = null;
        DownloadSettings ds = null;
        CommandExecutor instance = null;
        instance.pasteFiles(parent, ds);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of showRecent method, of class CommandExecutor.
     */
    @Test
    public void testShowRecent() throws Exception {
        System.out.println("showRecent");
        CommandExecutor instance = null;
        instance.showRecent();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of translateCommandline1 method, of class CommandExecutor.
     */
    @Test
    public void testTranslateCommandline1() {
        System.out.println("translateCommandline1");
        String toProcess = "";
        String[] expResult = null;
        String[] result = CommandExecutor.translateCommandline1(toProcess);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of translateCommandline method, of class CommandExecutor.
     */
    @Test
    public void testTranslateCommandline() {
        System.out.println("translateCommandline");
        String toProcess = "";
        String[] expResult = null;
        String[] result = CommandExecutor.translateCommandline(toProcess);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
