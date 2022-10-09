import java.lang.Process;
import java.lang.ProcessBuilder;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.*;

/*
 * Main class for assignment
 * @author Isaac Airmet
 */
public class Terminal {
    private static ArrayList<String> history = new ArrayList<String>();
    private static int pTime = 0;
    private static boolean requiresInput = true;
    public static void main(String[] args) {
        String input = "";
        Scanner inputGetter = new Scanner(System.in);

        // Keep terminal open until user enters exit
        while (!input.equals("exit")) {
            // No input required if the ^ NUMBER call was last used
            if (requiresInput) {
                String curPWD = getPWD();
                System.out.print("[" + curPWD + "]: ");
    
                input = inputGetter.nextLine();
            }
            requiresInput = true;

            // Protects against empty input
            if (input.isEmpty() || input.isBlank()) {
                continue;
            }

            ArrayList<String> parsedInput = new ArrayList<String>(Arrays.asList(splitCommand(input)));

            history.add(input);

            Boolean shouldWait = true;

            // Handles wait symbol
            if (parsedInput.get(parsedInput.size() - 1).equals("&")) {
                parsedInput.remove(parsedInput.size() - 1);
                shouldWait = false;
            }   

            // Check first inputted "word" to try to match it up. Check for correct overall length of input
            if (parsedInput.get(0).equals("ptime")) {
                printChildTime();
            } else if (parsedInput.get(0).equals("history") && parsedInput.size() == 1) {
                printHistory();
            } else if (parsedInput.get(0).equals("^") && parsedInput.size() == 2) {
                input = handlePastInput(input, parsedInput);
            } else if (parsedInput.get(0).equals("list") && parsedInput.size() == 1) {
                printList();
            } else if (parsedInput.get(0).equals("cd") && (parsedInput.size() == 2 || parsedInput.size() == 1)) {
                String pathChange = "";
                if (parsedInput.size() != 1) {
                    pathChange = parsedInput.get(1);
                }
                changeDirectory(pathChange);
            } else if (parsedInput.get(0).equals("mdir") && parsedInput.size() == 2) {
                createSubDirectory(parsedInput.get(1));
            } else if (parsedInput.get(0).equals("rdir") && parsedInput.size() == 2) {
                removeSubDirectory(parsedInput.get(1));
            } else if (parsedInput.get(0).equals("exit")  && parsedInput.size() == 1) {
                continue;
            } else {
                if (parsedInput.contains("|")) {
                    boolean success = runPipedExternalCommands(parsedInput, shouldWait);
                    if (!success) {
                        invalidCommand(input);
                    }
                } else {
                    boolean success = runExternalCommand(parsedInput, shouldWait);
                    if (!success) {
                        invalidCommand(input);
                    } 
                }
            }
        }
        inputGetter.close();
        System.exit(0);
    }

    /*
     * Runs piping on two external commands
     * @author Isaac Airmet & Prof Mano
     */
    private static boolean runPipedExternalCommands(ArrayList<String> command, Boolean shouldWait) {
        try {
            // Get each half of the pipe as a process
            ProcessBuilder pb1 = new ProcessBuilder(command.subList(0, command.indexOf("|")));
            ProcessBuilder pb2 = new ProcessBuilder(command.subList(command.indexOf("|") + 1, command.size()));
            pb1.directory(new File(System.getProperty("user.dir")));
            pb2.directory(new File(System.getProperty("user.dir")));

            // Use the parent process's I/O channels
            pb1.redirectInput(ProcessBuilder.Redirect.INHERIT);
            pb2.redirectOutput(ProcessBuilder.Redirect.INHERIT);

            long startTime = System.currentTimeMillis();
            Process p1 = pb1.start();
            Process p2 = pb2.start();

            java.io.InputStream in = p1.getInputStream();
            java.io.OutputStream out = p2.getOutputStream();

            // Read the data from p1 and feed to p2.
            int data;
            while ((data = in.read()) != -1) {
                    out.write(data);
            }

            out.flush();
            out.close();

            // Wait and time if needed
            if (shouldWait) {
                p1.waitFor();
                p2.waitFor();
                pTime += System.currentTimeMillis() - startTime;
            }
        } catch (Exception e) {
            System.out.println("External Error!");
        }
        return true;
    }

    /*
     * Runs external command
     * @author Isaac Airmet
     */
    private static boolean runExternalCommand(ArrayList<String> parsedInput, boolean shouldWait) {
        try {
            ProcessBuilder pb1 = new ProcessBuilder(parsedInput);
            pb1.directory(new File(System.getProperty("user.dir")));
            pb1.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            long startTime = System.currentTimeMillis();
            Process p1 = pb1.start();
            if (shouldWait) {
                p1.waitFor();
                pTime += System.currentTimeMillis() - startTime;
            }
            
        } catch (Exception e) {
            System.out.println("External Error!");
        }
        return true;
    }

    /*
     * Removes subdirectory
     * @author Isaac Airmet
     */
    private static void removeSubDirectory (String folder) {
        try {
            java.nio.file.Path proposed = java.nio.file.Paths.get(getPWD(), folder);
            if (proposed.toFile().isDirectory()) {
                proposed.toFile().delete();
            } else {
                System.out.println("Does Not Exist");
            }
        } catch (Exception e) {
            System.out.println("An Unknown Error Occured");
        }
    }

    /*
     * Creates subdirectory
     * @author Isaac Airmet
     */
    private static void createSubDirectory(String newDirectory) {
        try {
            java.nio.file.Path proposed = java.nio.file.Paths.get(getPWD(), newDirectory);
            if (proposed.toFile().isDirectory() || proposed.toFile().exists()) {
                System.out.println("Directory Already Exists");
            } else {
                proposed.toFile().mkdir();
                
            }
        } catch (Exception e) {
            System.out.println("An Unknown Error Occured");
        }
    }

    /*
     * Runs piping on two external commands
     * @author Isaac Airmet
     */
    private static void changeDirectory(String pathChange) {
        try {
            if (pathChange.equals("")) {
                System.setProperty("user.dir", System.getProperty("user.home"));
            } else if (pathChange.equals("..")) {
                String parentDirectory = new File(getPWD()).getParent();
                // Protect against trying to go "up" too far
                if (parentDirectory != null) {
                    System.setProperty("user.dir", parentDirectory);
                    
                } else {
                    System.out.println("Directory Not Found");
                }
                
            } else {
                java.nio.file.Path proposed = java.nio.file.Paths.get(getPWD(), pathChange);
                if (proposed.toFile().isDirectory()) { 
                    System.setProperty("user.dir", proposed.toString());
                    System.getProperty("user.dir");
                } else {
                    System.out.println("Directory Not Found");
                }
            }
        } catch (Exception e) {
            System.out.println("An Unknown Error Occured");
        }
    }

    /*
     * Prints list of elements in current directory with added info
     * @author Isaac Airmet
     */
    private static void printList() {
        File head = new File(getPWD());
        File[] files = head.listFiles();
        String result = "";
        for (File file : files) {
            if (file.isDirectory()) {
                result += "d";
            } else {
                result += "-";
            }
            if (file.canRead()) {
                result += "r";
            } else {
                result += "-";
            }
            if (file.canWrite()) {
                result += "w";
            } else {
                result += "-";
            }
            if (file.canExecute()) {
                result += "x ";
            } else {
                result += "- ";
            }
            result += String.format("%10s ", (int)file.length());
            result += new SimpleDateFormat("MMM dd, yyyy HH:mm").format(new Date(file.lastModified())) + " ";
            result += file.getName() + "\n";
        }
        System.out.print(result);
    }

    /*
     * Used to handle the ^ NUMBER input
     * @author Isaac Airmet
     */
    private static String handlePastInput(String input, ArrayList<String> parsedInput) {
        int historyIndex = 0;
        try {
            historyIndex = Integer.parseInt(parsedInput.get(1));
            if (historyIndex == history.size()) {
                throw new Exception("Invalid Index");
            }
            String oldInput = history.get(historyIndex - 1);
            requiresInput = false;
            return oldInput;

        } catch (Exception e) {
            invalidCommand(input);
            return "";
        }
    }

    /*
     * Prints the command history
     * @author Isaac Airmet
     */
    private static void printHistory() {
        System.out.println("-- Command History --");
        String strBuilder = "";
        int counter = 1;
        for (String pastInput : history) {
            strBuilder += counter + " : " + pastInput + "\n";
            counter += 1;
        }
        System.out.print(strBuilder);
    }

    /*
     * Prints time taken of child processes
     * @author Isaac Airmet
     */
    private static void printChildTime() {
        double pTimeSeconds = pTime / 1000.0;
        DecimalFormat formater = new DecimalFormat("#.####");
        formater.setRoundingMode(RoundingMode.CEILING);
        System.out.printf("Total time in child processes: %.4f seconds\n", pTimeSeconds);
    }

    /*
     * Returns the current working directory
     * @author Isaac Airmet
     */
    private static String getPWD() {
        return System.getProperty("user.dir");
    }

    /*
     * Lets the user know that a command was invalid
     * @author Isaac Airmet
     */
    private static void invalidCommand(String input) {
        System.out.println("Invalid command: " + input);
    }

    /*
     * Split the user command by spaces, but preserving them when inside double-quotes.
     * Code Adapted from: https://stackoverflow.com/questions/366202/regex-for-splitting-a-string-using-space-when-not-surrounded-by-single-or-double
     * @author Mano
     */
    public static String[] splitCommand(String command) {
        java.util.List<String> matchList = new java.util.ArrayList<>();

        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(command);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                matchList.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                matchList.add(regexMatcher.group());
            }
        }
        return matchList.toArray(new String[matchList.size()]);
    }
}