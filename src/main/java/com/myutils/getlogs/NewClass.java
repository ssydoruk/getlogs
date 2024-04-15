/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.myutils.getlogs;

import java.lang.reflect.Type;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.xbill.DNS.*;
import org.xbill.DNS.Record;
import org.xbill.DNS.lookup.LookupSession;

/**
 *
 * @author ssydo
 */
public class NewClass {

    public static void main(String[] args) throws Exception {

        Record queryRecord = Record.newRecord(Name.fromString("dnsjava.org."), org.xbill.DNS.Type.A, DClass.IN);
        Message queryMessage = Message.newQuery(queryRecord);
        Resolver r = new SimpleResolver("8.8.8.8");
        r.sendAsync(queryMessage)
                .whenComplete(
                        (answer, ex) -> {
                            if (ex == null) {
                                System.out.println(answer);
                            } else {
                                ex.printStackTrace();
                            }
                        })
                .toCompletableFuture()
                .get();

        List<InetAddress> hostIPs = new ArrayList<>();
        try {
            Lookup lookup = new Lookup("www.google.com");
            Resolver resolver = new SimpleResolver("www.google.com");
            lookup.setResolver(resolver);
            Record[] records = lookup.run();
            if (records == null) {
                System.out.println("null");
            }
            for (Record record : records) {
                hostIPs.add(((ARecord) record).getAddress());
            }
        } catch (TextParseException ex) {
            System.out.println(ex);
            throw new IllegalStateException(ex);
        }

    }
}
