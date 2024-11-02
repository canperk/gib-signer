import java.io.ByteArrayOutputStream;
import javax.xml.soap.SOAPMessage;

public class SoapMessageUtil {
    public static String soapMessageToString(SOAPMessage soapMessage) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            soapMessage.writeTo(outputStream);
            return outputStream.toString("UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}