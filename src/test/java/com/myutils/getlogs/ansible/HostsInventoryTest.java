/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package com.myutils.getlogs.ansible;

import com.myutils.getlogs.HostAppdir;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
public class HostsInventoryTest {
    
    public HostsInventoryTest() {
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
     * Test of load method, of class HostsInventory.
     */
    @Test
    public void testLoad() throws Exception {
        System.out.println("load");
        File file = null;
        HostsInventory expResult = null;
        HostsInventory result = HostsInventory.load(file);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of main method, of class HostsInventory.
     */
    @Test
    public void testMain() throws Exception {
        System.out.println("main");
        String[] args = null;
        HostsInventory.main(args);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getMap method, of class HostsInventory.
     */
    @Test
    public void testGetMap() {
        System.out.println("getMap");
        LinkedHashMap<String, LinkedHashMap> map = null;
        String key = "";
        int capacity = 0;
        HostsInventory instance = new HostsInventory();
        LinkedHashMap<String, LinkedHashMap> expResult = null;
        LinkedHashMap<String, LinkedHashMap> result = instance.getMap(map, key, capacity);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of addHost method, of class HostsInventory.
     */
    @Test
    public void testAddHost() {
        System.out.println("addHost");
        String s = "";
        HostAppdir appDir = null;
        HashMap<String, Object> hh = null;
        HostsInventory instance = new HostsInventory();
        instance.addHost(s, appDir, hh);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of dump method, of class HostsInventory.
     */
    @Test
    public void testDump() throws Exception {
        System.out.println("dump");
        File file = null;
        HostsInventory instance = new HostsInventory();
        instance.dump(file);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
