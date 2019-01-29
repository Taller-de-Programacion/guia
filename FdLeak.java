/*
 * Compile:
 *  javac FdLeak.java
 *
 * Limit the file descriptors
 *  ulimit -Hn 2048
 *
 * Run the program with a low memory limit, this would force
 * the virtual machine to run the garbarge collecter more frequently
 * and therefor closing the open file descriptors
 *  java -Xmn512k -Xmx700m -verbose:gc FdLeak
 *
 * Run the same program again, but with a higher memory limit:
 * the virtual machine will wait longer to run the garbarge
 * collector and this will make the program ran out of file descriptors
 *  java -Xmn512m -Xmx700m -verbose:gc FdLeak
 *
 **/

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.lang.Thread;

public class FdLeak {
    public static void main(String args[]) {
        for (int i = 0; i < 2048; ++i) {
            try {
                FileInputStream istream = new FileInputStream("FdLeak.java");
            }
            catch (IOException e) {
                System.out.println(e);
            }
        }
    }
}
