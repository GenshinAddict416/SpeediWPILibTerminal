package com.sotabots;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Scanner; 

public class App {

    // ANSI Escape Codes for Colors
    public static final String RESET = "\u001b[0m";
    public static final String GREEN = "\u001b[32m";
    public static final String CYAN = "\u001b[36m";
    public static final String RED = "\u001b[31m";

    @SuppressWarnings("ConvertToTryWithResources")
    public static void main(String[] args) throws IOException {

        // Track state for the current working directory
        File currentDir = new File(System.getProperty("user.dir"));

        // Clear the console
        System.out.print("\u001b[2J\u001b[H");
        System.out.flush(); 

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        printstr(GREEN + "Welcome to the SpeedyTerminal!" + RESET);
        
        try (Scanner scanner = new Scanner(System.in)) {
            OUTER:
            while (true) {
                // Prompt showing the active directory folder name
                System.out.print(CYAN + "(" + currentDir.getName() + ") speedy> " + RESET);
                System.out.flush();

                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) continue;

                String command = input.split(" ")[0];

                switch (command) {
                    case "!exit" -> {
                        printstr(GREEN + "Goodbye!" + RESET);
                        break OUTER;
                    }

                    case "!hello" -> printstr(GREEN + "Hello, user!" + RESET);

                    case "!help" -> {
                        printstr(GREEN + "Available commands:");
                        printstr("!help            - Show this help message");
                        printstr("!hello           - Greet the user");
                        printstr("!echo [text]     - Repeat back the provided text");
                        printstr("!clear           - Clear the console");
                        printstr("!pwd             - Print the current working directory path");
                        printstr("!cd [directory]  - Change the current directory");
                        printstr("!ls              - List files and folders in the current directory");
                        printstr("!run [filename]  - Compile and execute a source file (.java, .cpp, .py, .js, .jar, .bat, .sh)");
                        printstr("!time            - Display the current time");
                        printstr("!date            - Display the current date");
                        printstr("!exit            - Exit the application");
                        printstr("");
                        printstr("You can also run files directly without !run:  main.exe, script.py, etc.");
                        printstr("Unknown commands are passed through to the system shell." + RESET);
                    }

                    case "!echo" -> {
                        String[] cmdArgs = getArgs(input);
                        String message = String.join(" ", cmdArgs);
                        printstr(GREEN + message + RESET);
                    }

                    case "!clear" -> {
                        System.out.print("\u001b[2J\u001b[H");
                        System.out.flush();
                    }

                    case "!pwd" -> {
                        printstr(GREEN + currentDir.getAbsolutePath() + RESET);
                    }

                    case "!time" -> {
                        LocalTime time = LocalTime.now();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                        printstr(GREEN + "Current Time: " + time.format(formatter) + RESET);
                    }

                    case "!date" -> {
                        LocalDate date = LocalDate.now();
                        printstr(GREEN + "Current Date: " + date + RESET);
                    }

                    case "!cd" -> {
                        String[] cmdArgs = getArgs(input);
                        if (cmdArgs.length == 0) {
                            printstr(GREEN + "Current directory: " + currentDir.getAbsolutePath() + RESET);
                        } else {
                            // 1. Grab the path string the user typed
                            String pathInput = cmdArgs[0];
                            File nextDir = new File(pathInput);

                            // 2. If it's NOT an absolute path (like typing '..' or 'src'), resolve it against currentDir
                            if (!nextDir.isAbsolute()) {
                                nextDir = new File(currentDir, pathInput);
                            }

                            // 3. Check if it exists and move
                            if (nextDir.exists() && nextDir.isDirectory()) {
                                currentDir = nextDir.getCanonicalFile();
                            } else {
                                System.err.println(RED + "Directory does not exist: " + pathInput + RESET);
                            }
                        }
                    }

                    case "!run" -> {
                        String[] cmdArgs = getArgs(input);
                        if (cmdArgs.length == 0) {
                            System.err.println(RED + "Usage: !run [filename] [optional_args...]" + RESET);
                        } else {
                            String filename = cmdArgs[0];
                            File fileToRun = new File(currentDir, filename);

                            if (!fileToRun.exists()) {
                                System.err.println(RED + "File not found: " + filename + RESET);
                            } else {
                                handleAdvancedRun(fileToRun, cmdArgs, currentDir);
                            }
                        }
                    }

                    case "!ls" -> {
                        File[] entries = currentDir.listFiles();
                        if (entries == null || entries.length == 0) {
                            printstr(GREEN + "(empty directory)" + RESET);
                        } else {
                            Arrays.sort(entries, (a, b) -> {
                                if (a.isDirectory() != b.isDirectory())
                                    return a.isDirectory() ? -1 : 1;
                                return a.getName().compareToIgnoreCase(b.getName());
                            });
                            for (File entry : entries) {
                                printstr(formatEntry(entry));
                            }
                        }
                    }
                    
                    default -> {
                        File fileToRun = new File(currentDir, command);
                        if (fileToRun.exists()) {
                            handleAdvancedRun(fileToRun, input.split("\\s+"), currentDir);
                        } else {
                            // Fall back to system command
                            try {
                                runTargetProcess(input.split("\\s+"), currentDir);
                            } catch (IOException | InterruptedException e) {
                                System.err.println(RED + "Unknown command: " + command + RESET);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Handles compilation checking and execution mechanics for specialized source files.
     */
    public static void handleAdvancedRun(File file, String[] originalArgs, File currentDir) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            System.err.println(RED + "No file extension found!" + RESET);
            return;
        }

        String extension = name.substring(lastIndexOf).toLowerCase();
        String baseName = name.substring(0, lastIndexOf);
        String[] extraArgs = originalArgs.length > 1 ? Arrays.copyOfRange(originalArgs, 1, originalArgs.length) : new String[0];

        try {
            if (extension.equals(".cpp")) {
                printstr(GREEN + "Compiling C++ file via MSVC (cl)..." + RESET);
                Process compile = new ProcessBuilder("cl", name, "/EHsc", "/Fe:" + baseName + ".exe")
                        .directory(currentDir).inheritIO().start();
                
                if (compile.waitFor() == 0) {
                    printstr(GREEN + "Compilation successful. Running..." + RESET);
                    runTargetProcess(combineArgs(new String[]{new File(currentDir, baseName + ".exe").getAbsolutePath()}, extraArgs), currentDir);
                } else {
                    System.err.println(RED + "C++ Compilation failed!" + RESET);
                }
            } 
            else if (extension.equals(".java")) {
                printstr(GREEN + "Compiling Java file (javac)..." + RESET);
                Process compile = new ProcessBuilder("javac", name)
                        .directory(currentDir).inheritIO().start();

                if (compile.waitFor() == 0) {
                    printstr(GREEN + "Compilation successful. Running JVM..." + RESET);
                    runTargetProcess(combineArgs(new String[]{"java", baseName}, extraArgs), currentDir);
                } else {
                    System.err.println(RED + "Java Compilation failed!" + RESET);
                }
            } 
            else {
                String[] fallbackCmd = buildScriptCommand(extension, file.getAbsolutePath(), extraArgs);
                if (fallbackCmd != null) {
                    runTargetProcess(fallbackCmd, currentDir);
                } else {
                    System.err.println(RED + "Unsupported file type: " + extension + RESET);
                }
            }
        } catch (IOException | InterruptedException e) {
            System.err.println(RED + "Execution error: " + e.getMessage() + RESET);
        }
    }

    /**
     * Maps non-compiled scripts to their proper interpreter syntax.
     */
    private static String[] buildScriptCommand(String extension, String filePath, String[] extraArgs) {
        String[] base;
        switch (extension) {
            case ".py"  -> base = new String[]{"python", filePath};
            case ".js"  -> base = new String[]{"node", filePath};
            case ".jar" -> base = new String[]{"java", "-jar", filePath};
            case ".bat" -> base = new String[]{"cmd", "/c", filePath};
            case ".sh"  -> base = new String[]{"bash", filePath};
            default     -> { return null; }
        }
        return combineArgs(base, extraArgs);
    }

    private static String[] combineArgs(String[] base, String[] extra) {
        String[] combined = new String[base.length + extra.length];
        System.arraycopy(base, 0, combined, 0, base.length);
        System.arraycopy(extra, 0, combined, base.length, extra.length);
        return combined;
    }

    private static void runTargetProcess(String[] cmd, File currentDir) throws IOException, InterruptedException {
        new ProcessBuilder(cmd).directory(currentDir).inheritIO().start().waitFor();
    }

    public static String[] getArgs(String input) {
        String[] tokens = input.split("\\s+");
        if (tokens.length <= 1) {
            return new String[0];
        }
        return Arrays.copyOfRange(tokens, 1, tokens.length);
    }

    public static void printstr(String str) {
        System.out.println(str);
    }

    private static String formatEntry(File entry) {
        String name = entry.isDirectory() ? entry.getName() + "/" : entry.getName();
        String color = entry.isDirectory() ? CYAN : GREEN;
        String size = entry.isDirectory() ? "      <DIR>" : String.format("%10d B", entry.length());
        return color + size + "  " + name + RESET;
    }
}