/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2014 Jeff Nelson, Cinchapi Software Collective
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.cinchapi.stalk;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.cinchapi.stalk.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Stalk} utility is used to continuously recursively watch one or
 * more directories for file changes (e.g. creation, deletion, modification) and
 * subsequently execute a shell command.
 * 
 * @author jnelson
 */
public class Stalk {

    // NOTE: This program will run until killed...

    /**
     * The logger...
     */
    private static final Logger log = LoggerFactory.getLogger(Stalk.class);

    /**
     * The usage message...
     */
    private static final String usage = MessageFormat.format(
            "USAGE: {0} dir1 [dir2...dirN] command", Stalk.class
                    .getSimpleName().toLowerCase());

    /**
     * Run the program....
     * 
     * @param args
     */
    public static void main(String... args) {

        // Preconditions check...
        if(args.length <= 1) {
            System.err.println("Please specify at least "
                    + "one file or directory to watch "
                    + "followed by a command to execute");
            System.out.println(usage);
            System.exit(1);
        }
        else {
            String command = args[args.length - 1];
            String[] commands = { "bash", "-c", command };
            try (WatchService watcher = FileSystems.getDefault()
                    .newWatchService()) {

                // Find all the recursive sub directories
                List<String> paths = new ArrayList<String>();
                for (int i = 0; i < args.length - 1; i++) {
                    paths.addAll(Files.getAllRecursiveSubDirectories(args[i]));
                }

                for (String p : paths) {
                    Path path = Paths.get(p);
                    log.info("Listening for changes to " + path + "...");
                    path.register(watcher,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY);
                }
                log.info("Configured to execute '" + command
                        + "' whenever a change occurs");

                for (;;) {
                    WatchKey key = watcher.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        log.info(
                                "Noticed the following change: " + "{} {}",
                                event.kind().toString().replace("ENTRY_", ""),
                                key.watchable() + File.separator
                                        + event.context());
                        Runtime.getRuntime().exec(commands);
                        if(key.reset()) {
                            break;
                        }
                    }
                }
            }
            catch (Exception e) {
                System.err.println(e);
                System.exit(1);
            }

        }

    }

}
