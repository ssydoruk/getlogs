/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package getlogs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 *
 * @author stepansydoruk
 */
class Hosts extends HashMap {

    Hosts(String fileName) throws FileNotFoundException, IOException {
        File file = new File(fileName);

        BufferedReader br = new BufferedReader(new FileReader(file));

        String st;
        while ((st = br.readLine()) != null) {
            String[] split = st.split(",");
            if (split.length > 1) {
                // So expecting first field to be host name and the second to be application
                put(split[1], split[0]);

            }
        }
        br.close();
    }

}
