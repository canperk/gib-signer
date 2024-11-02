public class App {
    public static void main(String[] args) throws Exception {
        String type = args[0];
        switch (type) {
            case "convert":
                if (args.length != 4) {
                    System.err.println("Invalid Parameters for 'convert'");
                    System.exit(1);
                }
                String signPin = args[1];
                String soapPath = args[2];
                String signedSoapPath = args[3];
                SOAPHelper.convertToSoapMessage(signPin, soapPath, signedSoapPath);
                break;
            case "send":
                if (args.length != 3) {
                    System.err.println("Invalid Parameters for 'send'");
                    System.exit(1);
                }
                String reportPath = args[1];
                String reportResponsePath = args[2];
                SOAPHelper.sendSignedESUReport(reportPath, reportResponsePath);
                break;
            case "check":
                if (args.length != 4) {
                    System.err.println("Invalid Parameters for 'check'");
                    System.exit(1);
                }
                String checkPin = args[1];
                String id = args[2];
                String statusResponsePath = args[3];
                SOAPHelper.getBatchStatus(checkPin, id, statusResponsePath);
                break;
            default:
                throw new Exception("Type mismatch");
        }
    }
}
