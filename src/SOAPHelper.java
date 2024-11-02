import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.apache.ws.security.WSEncryptionPart;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.components.crypto.Merlin;
import org.apache.ws.security.message.WSSecHeader;
import org.apache.ws.security.message.WSSecSignature;
import org.apache.ws.security.message.WSSecTimestamp;
import org.w3c.dom.Document;

import sun.security.pkcs11.SunPKCS11;

public class SOAPHelper {

    public static void convertToSoapMessage(String pin, String filePath, String outputFilePath) throws Exception {
        String soapXmlContent = new String(Files.readAllBytes(Paths.get(filePath)), Charset.forName("UTF-8"));

        SOAPMessage soapmsg = getSOAPMessageFromString(soapXmlContent);
        SOAPMessage signed = getSignedSOAPMessage(pin, soapmsg);
        saveSOAPMessageToFile(signed, outputFilePath);
    }

    public static void sendSignedESUReport(String xmlFilePath, String reportResponsePath) {
        String url = "https://okctest.gib.gov.tr/okcesu/services/EArsivWsPort/earsiv";
        String soapAction = "urn:sendDocumentFile";

        try {
            String soapMessage = new String(Files.readAllBytes(Paths.get(xmlFilePath)), "utf-8");

            disableSSLVerification();

            HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");
            connection.setRequestProperty("SOAPAction", soapAction);
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = soapMessage.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream(),
                            "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            saveServerResponseToFile(response.toString(), reportResponsePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void getBatchStatus(String pin, String id, String reportResponsePath) {
        String url = "https://okctest.gib.gov.tr/okcesu/services/EArsivWsPort/earsiv";
        String soapAction = "urn:getBatchStatus";

        try {
            String soapMessage = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:ear=\"http://earsiv.vedop3.ggm.gov.org/\"><soap:Header/><soap:Body><ear:getBatchStatus><paketId>"
                    + id + "</paketId></ear:getBatchStatus></soap:Body></soap:Envelope>";

            SOAPMessage soapmsg = getSOAPMessageFromString(soapMessage);
            SOAPMessage signed = getSignedSOAPMessage(pin, soapmsg);
            String soapMessageString = SoapMessageUtil.soapMessageToString(signed);
            disableSSLVerification();

            HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");
            connection.setRequestProperty("SOAPAction", soapAction);
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = soapMessageString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream(),
                            "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
            saveServerResponseToFile(response.toString(), reportResponsePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveSOAPMessageToFile(SOAPMessage soapMessage, String filePath) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(filePath);
            soapMessage.writeTo(fileOutputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void saveServerResponseToFile(String response, String filePath) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(filePath);
            fileOutputStream.write(response.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static SOAPMessage getSOAPMessageFromString(String xml) throws SOAPException, IOException {
        MessageFactory factory = MessageFactory.newInstance("SOAP 1.2 Protocol");
        SOAPMessage message = factory.createMessage(new MimeHeaders(),
                new ByteArrayInputStream(xml.getBytes(Charset.forName("UTF-8"))));
        return message;
    }

    private static Document toDocument(SOAPMessage soapMsg)
            throws TransformerConfigurationException, TransformerException, SOAPException, IOException {
        Source src = soapMsg.getSOAPPart().getContent();
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        DOMResult result = new DOMResult();
        transformer.transform(src, result);
        return (Document) result.getNode();
    }

    private static SOAPMessage updateSOAPMessage(Document doc, SOAPMessage message) throws Exception {
        DOMSource domSource = new DOMSource(doc);
        message.getSOAPPart().setContent(domSource);
        return message;
    }

    private static SOAPMessage getSignedSOAPMessage(String pin, SOAPMessage soap_message)
            throws SOAPException, Exception {
        SunPKCS11 pkcsProvider = null;
        FileInputStream fis = new FileInputStream("pkcs.properties");
        pkcsProvider = new SunPKCS11(fis);
        char[] passwordChar = pin.toCharArray();
        Security.addProvider(pkcsProvider);
        KeyStore ks = KeyStore.getInstance("PKCS11", pkcsProvider);
        ks.load((InputStream) null, passwordChar);
        X509Certificate signCert = null;
        Key signPrivKey = null;
        Enumeration<String> aliases = ks.aliases();
        String alias = null;

        while (aliases.hasMoreElements()) {
            alias = (String) aliases.nextElement();
            signCert = (X509Certificate) ks.getCertificate(alias);
            signPrivKey = ks.getKey(alias, (char[]) null);
            if (signCert.getKeyUsage()[0]) {
                break;
            }
        }

        WSSConfig config = new WSSConfig();
        config.setWsiBSPCompliant(false);
        WSSecSignature builder = new WSSecSignature();
        builder.setX509Certificate(signCert);
        builder.setUserInfo(alias, new String(passwordChar));
        builder.setUseSingleCertificate(true);
        builder.setKeyIdentifierType(1);
        Document document = toDocument(soap_message);
        WSSecHeader secHeader = new WSSecHeader();
        secHeader.setMustUnderstand(false);
        secHeader.insertSecurityHeader(document);
        WSSecTimestamp timestamp = new WSSecTimestamp();
        timestamp.setTimeToLive(3600);
        document = timestamp.build(document, secHeader);
        Vector<WSEncryptionPart> parts = new Vector<WSEncryptionPart>();
        WSEncryptionPart timestampPart = new WSEncryptionPart("Timestamp",
                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", "");
        WSEncryptionPart bodyPart = new WSEncryptionPart("Body", "http://www.w3.org/2003/05/soap-envelope", "");
        parts.add(timestampPart);
        parts.add(bodyPart);
        builder.setParts(parts);
        builder.setSignatureAlgorithm("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
        Properties properties = new Properties();
        properties.setProperty("org.apache.ws.security.crypto.provider",
                "org.apache.ws.security.components.crypto.Merlin");
        Crypto crypto = CryptoFactory.getInstance(properties);
        KeyStore keystore = KeyStore.getInstance("PKCS11");
        keystore.load((InputStream) null, passwordChar);
        keystore.setKeyEntry(alias, (PrivateKey) signPrivKey, passwordChar, new Certificate[] { signCert });
        ((Merlin) crypto).setKeyStore(keystore);
        crypto.loadCertificate(new ByteArrayInputStream(signCert.getEncoded()));
        document = builder.build(document, crypto, secHeader);
        soap_message = updateSOAPMessage(document, soap_message);

        return soap_message;
    }

    private static void disableSSLVerification() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    }

}