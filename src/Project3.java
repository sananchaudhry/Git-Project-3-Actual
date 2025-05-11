public class Project3 {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: project3 <command> <args>");
            return;
        }

        String command = args[0];

        try {
            switch (command) {
                case "create":
                    IndexFile.create(args[1]);
                    break;
                case "insert":
                    if (args.length != 4) {
                        System.err.println("Usage: project3 insert <file> <key> <value>");
                        return;
                    }
                    IndexFile.insert(args[1], Long.parseLong(args[2]), Long.parseLong(args[3]));
                    break;
                case "search":
                    if (args.length != 3) {
                        System.err.println("Usage: project3 search <file> <key>");
                        return;
                    }
                    IndexFile.search(args[1], Long.parseLong(args[2]));
                    break;
                case "load":
                    if (args.length != 3) {
                        System.err.println("Usage: project3 load <file> <csv>");
                        return;
                    }
                    IndexFile.load(args[1], args[2]);
                    break;
                case "print":
                    IndexFile.print(args[1]);
                    break;
                case "extract":
                    if (args.length != 3) {
                        System.err.println("Usage: project3 extract <file> <output.csv>");
                        return;
                    }
                    IndexFile.extract(args[1], args[2]);
                    break;
                default:
                    System.err.println("Unknown command: " + command);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
