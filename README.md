
## Başlarken

Gelir İdaresi Başknalığı'nın şarj operatörü firmaları için zorunlu koştuğu aylık rapor servisi entegrayonu

## Kullanım

Projeyi derledikten sonra çıkan jar dosyasını 3 farklı şekilde çağırabilirsiniz.

#### 1. Raporun Hazırlanması
ESU raporunuzun imzalı hali aşağıdaki xml örneği halinde bir path'e yazılmalıdır.

    <?xml version="1.0" encoding="utf-8"?>
    <eArsivRaporu xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:ds="http://www.w3.org/2000/09/xmldsig#" xmlns="http://earsiv.efatura.gov.tr">
      <baslik>
        <ds:SignedInfo>İmza Bilgisi</ds:SignedInfo>
        <versiyon>1.0</versiyon>
        <mukellef>
          <vkn>1234567890</vkn>
        </mukellef>
        <hazirlayan>
          <vkn>1234567890</vkn>
        </hazirlayan>
        <raporNo>d34512e8-8141-4c38-8c25-e86ef42f1bb5</raporNo>
        <donemBaslangicTarihi>2024-09-01</donemBaslangicTarihi>
        <donemBitisTarihi>2024-09-30</donemBitisTarihi>
        <bolumBaslangicTarihi>2024-09-01</bolumBaslangicTarihi>
        <bolumBitisTarihi>2024-09-30</bolumBitisTarihi>
        <bolumNo>1</bolumNo>
      </baslik>
      <esuRapor>
        <UUID>dbaacd1f-400d-4634-b628-3606ef6b91b9</UUID>
        <plakaNo>06ABC123</plakaNo>
        <hizmetMiktari unitCode="kWh">7.224</hizmetMiktari>
        <toplamTutar>57.07</toplamTutar>
        <paraBirimi>TRY</paraBirimi>
      </esuRapor>
        <esuRapor>
	    <UUID>c40af62a-508e-4ea0-86ab-9a1112f8c1c6</UUID>
	    <plakaNo>06ACD519</plakaNo>
	    <hizmetMiktari unitCode="kWh">4.373</hizmetMiktari>
	    <toplamTutar>34.55</toplamTutar>
	    <paraBirimi>TRY</paraBirimi>
	  </esuRapor>
    </eArsivRaporu>

Daha sonra bu dosyanın kayıtlı olduğu path, e-mühür bilgisayarınıza takılı iken aşağıdaki komut dizisinde 2. sırada kullanılacaktır.

    java -jar gibsigner.jar "convert" "123456" "imzaliesurapor.xml" "soap_message_to_send.xml"

#### 2. Imzalı raporu gönderme
İmzalı raporu aynı isimde bir zip içine attıktan sonra zip dosyasının base64 çıktısını bir xml'de aşağıdaki gibi ayarlanır.

    <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:web="http://earsiv.vedop3.ggm.gov.org/">
    <soap:Header>
    <wsse:Security>İmza Bilgisi</wsse:Security>
    </soap:Header>
       <soap:Body xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" wsu:Id="id-raporid">
          <web:sendDocumentFile>
             <Attachment>
                <fileName>soap_message_to_send.zip</fileName>
                <binaryData>dosya base64 çıktısı</binaryData>
             </Attachment>
          </web:sendDocumentFile>
       </soap:Body>
    </soap:Envelope>

SOAP mesajını da Gelir İdaresi Başkanlığı'na göndermek için aşağıdaki script dizi kullanılır. Cevap alınan bilgileri çıktı olarak yazılan dosyadan kontrol edebilirsiniz. Dosya kaydedildi cevabını görmeniz yeterli olacaktır.

     java -jar gibsigner.jar "send" "signed_soap_message.xml" "response_file.xml"

#### 3. GİB'den İmza durumunu sorgulama

Dosyayı kaydedikten sonra başarılı olup olmadığını da yeniden sormak germektedir. Bunun için de imzalı bir istek atmanız için aşağıdaki script dizisi kullanılmalıdır.

     java -jar gibsigner.jar "check" "123456" "report_id" "response_file.xml"

Alınan cevap sonrası Gelir İdaresi Başkanlığı'nın servisinden 30 kodunun gelmesi rapor gönderim sürecini tamamlayacaktır.

## Ön gereksinimler

1. Mali mühür
2. ÖKC Başvurusu
3. Java 8 SDK
