/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.myutils.getlogs;

import java.lang.reflect.Type;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.xbill.DNS.*;
import org.xbill.DNS.Record;
import org.xbill.DNS.lookup.LookupSession;
import org.xbill.DNS.spi.DNSJavaNameService;

/**
 *
 * @author ssydo
 */
public class NewClass {

    public static void main(String[] args) throws Exception {

        // Lookup lookup = new Lookup("rn-voicep-lapp28.corp.apple.com",
        // org.xbill.DNS.Type.ANY);
        // Resolver resolver = new SimpleResolver();
        // lookup.setResolver(resolver);
        // lookup.setCache(null);
        // Record[] records = lookup.run();
        // if (lookup.getResult() == Lookup.SUCCESSFUL) {
        // String responseMessage = null;
        // String listingType = null;
        // for (int i = 0; i < records.length; i++) {
        // if (records[i] instanceof TXTRecord) {
        // TXTRecord txt = (TXTRecord) records[i];
        // for (Iterator j = txt.getStrings().iterator(); j.hasNext();) {
        // responseMessage += (String) j.next();
        // }
        // } else if (records[i] instanceof ARecord) {
        // listingType = ((ARecord) records[i]).getAddress().getHostAddress();
        // }
        // }
        // System.err.println("Found!");
        // System.err.println("Response Message: " + responseMessage);
        // System.err.println("Listing Type: " + listingType);
        // } else if (lookup.getResult() == Lookup.HOST_NOT_FOUND) {
        // System.err.println("Not found.");
        // } else {
        // System.err.println("Error!");
        // }

        List<InetAddress> hostIPs = new ArrayList<>();
        try {
            Lookup lookup1 = new Lookup("rn-voicep-lapp28.corp.apple.com", org.xbill.DNS.Type.CNAME);
            Resolver resolver1 = new SimpleResolver();
            lookup1.setResolver(resolver1);
            Record[] records1 = lookup1.run();
            if (records1 == null) {
                System.out.println("null");
            }
            for (Record record : records1) {
                if (record instanceof CNAMERecord) {
                    CNAMERecord r = (CNAMERecord) record;
                    System.out.println(r.getName() + " " + r.getTarget());
                }
                // hostIPs.add(((ARecord) record).getAddress());
            }
        } catch (TextParseException ex) {
            System.out.println(ex);
            throw new IllegalStateException(ex);
        }

    }
}
