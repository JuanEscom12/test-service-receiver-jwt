package com.citi.jwt.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.BooleanUtils;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.citi.jwt.feign.FeignResponse;
import com.citi.jwt.service.CitiJwtReceiverService;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.codec.Base64;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CitiJwtReceiverServiceImpl implements CitiJwtReceiverService {

	@SuppressWarnings("deprecation")
	private static final AlgorithmConstraints CONTENT_ENCRYPTION_ALGORITHM_CONSTRAINTS = new AlgorithmConstraints(
			ConstraintType.WHITELIST, ContentEncryptionAlgorithmIdentifiers.AES_256_GCM);
	 
	@SuppressWarnings("deprecation")
	private static final AlgorithmConstraints KEY_ENCRYPTION_ALGORITHM_CONSTRAINTS = new AlgorithmConstraints(
			ConstraintType.WHITELIST, KeyManagementAlgorithmIdentifiers.RSA_OAEP_256);

	private static final StringBuilder TEXT = new StringBuilder();
	
	@Autowired
	private ResourceLoader resourceLoader;
	
	private static final String PATH_KEY_FILE = "jwe_key_file";

	private static final String PATH_JWS_KEY_FILE = "jws_key_file";
	
	private static final String PRIVATE_EXTENSION = ".key";
	
	private static final String PUBLIC_EXTENSION = ".pub";
	
	
	@Override
	public FeignResponse decodeJwt(String message) 
			throws NoSuchAlgorithmException, InvalidKeySpecException, JoseException, IOException {
		log.info("****************** SERVICE RECEIVER *********************");
		
		long millisStar = Calendar.getInstance().getTimeInMillis();
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	    RSAPrivateKey privateKey = (RSAPrivateKey)keyFactory.generatePrivate(
	    		new PKCS8EncodedKeySpec(getKey(PATH_KEY_FILE, PRIVATE_EXTENSION)));  		
	    JsonWebEncryption jweDecod = new JsonWebEncryption();
	    jweDecod.setAlgorithmConstraints(KEY_ENCRYPTION_ALGORITHM_CONSTRAINTS);
	    jweDecod.setContentEncryptionAlgorithmConstraints(CONTENT_ENCRYPTION_ALGORITHM_CONSTRAINTS);
	    jweDecod.setKey(privateKey);
	    jweDecod.setCompactSerialization(message);
	    long iat = jweDecod.getHeaders().getLongHeaderValue("iat");
	    log.info(":::::::::::::::: Claim iat private :" + iat);
	    String payload = jweDecod.getPayload();
	    payload = getStringJwsDeserealized(payload);
	    log.info(":::::::::::::::: Payload Request - Message {} ", payload);
		//////////////////////////////////////////////// ENCODED ////////////////////////////////////////////////////////////
		RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(
				new X509EncodedKeySpec(getKey(PATH_KEY_FILE, PUBLIC_EXTENSION)));
		JsonWebEncryption jwe = new JsonWebEncryption();
		jwe.setPayload(getStringJWs());		
		jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.RSA_OAEP_256);
		jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_256_GCM);
		jwe.setKey(publicKey);
	    iat = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
	    jwe.getHeaders().setObjectHeaderValue("iat", iat);
		String jweCompact = jwe.getCompactSerialization();
		log.info(":::::::::::::::::: JWE Compact {} ", jweCompact);
		FeignResponse response = new FeignResponse();
		response.setParameter(jweCompact);
		log.info("::::::::::: Result Receiver {} ", response);
		log.info("****** Tiempo total del proceso (Receiver) : {} ", Calendar.getInstance().getTimeInMillis() - millisStar);
	    return response;
	}
			
	private byte[] getKey(String pathDirectory, String keyExtension) throws IOException {
		//Resource resource = resourceLoader.getResource("classpath:" + pathDirectory + keyExtension);
		//Path path = Paths.get(pathDirectory + keyExtension);
		//return Files.readAllBytes(resource.getFile().toPath());
		Resource resource = resourceLoader.getResource("classpath:" + pathDirectory + keyExtension);		
		log.info("************************* Resource-length {} ", resource.getInputStream().readAllBytes().length);
	    return resource.getInputStream().readAllBytes();
	}
			
	@SuppressWarnings("unused")
	private String getPdf() {
		final Document document = new Document();
		document.addTitle("pdf - test");
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try {
			PdfWriter.getInstance(document, byteArrayOutputStream);
		} catch (DocumentException e) {
			log.error(":: Error pdf  ", e);
		}
		document.open();
		final Font font = FontFactory.getFont(FontFactory.TIMES, 12, BaseColor.BLACK);
		try {
			Paragraph paraghaph = new Paragraph();
			StringBuilder text = new StringBuilder();
//			Resource resource = resourceLoader.getResource("classpath:PDF.txt");
//			//Path path = Paths.get("PDF.txt");
//			List<String> lines = Files.readAllLines(resource.getFile().toPath());
//			for (String line : lines) {
//				text.append(line);
//			}
			text.append(getTextFromInputStream());
			Chunk chunk = new Chunk(text.toString(), font);
			paraghaph.add(chunk);
			paraghaph.setAlignment(Paragraph.ALIGN_JUSTIFIED);
			document.add(paraghaph);
		} catch (Exception e) {
			log.error(":: Error pdf  ", e);
		}
		document.close();
		return Base64.encodeBytes(byteArrayOutputStream.toByteArray());		
	}
	
	private String getText()  {
		if (BooleanUtils.negate(TEXT.toString().length() == 0)) {
			return Base64.encodeBytes(TEXT.toString().getBytes());
		}
		//Path path = Paths.get("PDF.txt");
//		Resource resource = resourceLoader.getResource("classpath:PDF.txt");
//		List<String> lines = null;
//		try {
//			lines = Files.readAllLines(resource.getFile().toPath());
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		for (String line : lines) {
//			TEXT.append(line);
//		}
		TEXT.append(getTextFromInputStream());
		return Base64.encodeBytes(TEXT.toString().getBytes());
	}
	
	private String getTextFromInputStream() {
		final StringBuilder result = new StringBuilder();
		final Resource resource = resourceLoader.getResource("classpath:PDF.txt");
		try {
			for (final byte item: resource.getInputStream().readAllBytes()) {
				result.append((char)item);
			}
		} catch (IOException e) {
			log.error(":: Error reading file ", e);
		}
		return result.toString();
	}

	private String getStringJwsDeserealized(String compactSerialization) 
			throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		// Create a new JsonWebSignature
		JsonWebSignature jwsSignature = new JsonWebSignature();
		
		// Set the algorithm constraints based on what is agreed upon or expected from
		// the sender
		jwsSignature.setAlgorithmConstraints(new AlgorithmConstraints(ConstraintType.PERMIT,
				AlgorithmIdentifiers.RSA_USING_SHA256));

		// Set the compact serialization on the JWS
		try {
			jwsSignature.setCompactSerialization(compactSerialization);
		} catch (JoseException e) {
			e.printStackTrace();
		}
		// Set the verification key
		// Note that your application will need to determine where/how to get the key
		// Here we use an example from the JWS spec
//		PublicKey publicKey = ExampleEcKeysFromJws.PUBLIC_256;
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(
				new X509EncodedKeySpec(getKey(PATH_JWS_KEY_FILE, PUBLIC_EXTENSION)));
		jwsSignature.setKey(publicKey);

		// Check the signature
		String payload = null;
		try {
			boolean signatureVerified = jwsSignature.verifySignature();
			// Do something useful with the result of signature verification
			log.info("::::::::::: JWS Signature is valid: {} ", signatureVerified);
			// Get the payload, or signed content, from the JWS
			payload = jwsSignature.getPayload();
			// Do something useful with the content
			log.info(":::::::::::: JWS payload: {} ", payload);
		} catch (JoseException e) {
			e.printStackTrace();
		}
		return payload;
	}

	private String getStringJWs() 
			throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		// The content that will be signed
		String examplePayload = getText();
		
		// Create a new JsonWebSignature
		JsonWebSignature jws = new JsonWebSignature();

		// Set the payload, or signed content, on the JWS object
		jws.setPayload(examplePayload);

		// Set the signature algorithm on the JWS that will integrity protect the
		// payload
		jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
		// Set the signing key on the JWS
		// Note that your application will need to determine where/how to get the key
		// and here we just use an example from the JWS spec
		//PrivateKey privateKey = ExampleEcKeysFromJws.PRIVATE_256;
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		RSAPrivateKey privateKey = (RSAPrivateKey)keyFactory.generatePrivate(
	    		new PKCS8EncodedKeySpec(getKey(PATH_JWS_KEY_FILE, PRIVATE_EXTENSION)));
		jws.setKey(privateKey);
			    
		// Sign the JWS and produce the compact serialization or complete JWS
		// representation, which
		// is a string consisting of three dot ('.') separated base64url-encoded
		// parts in the form Header.Payload.Signature
		String jwsCompactSerialization = null;
		try {
			jwsCompactSerialization = jws.getCompactSerialization();
			// Do something useful with your JWS
			//log.info(":::::::::::::::::: Serialization JWS {} ", jwsCompactSerialization);
		} catch (JoseException e) {
			e.printStackTrace();
		}
		return jwsCompactSerialization;
	}

	@Override
	public FeignResponse executeNoEncryption() {
		FeignResponse result = new FeignResponse();
		result.setParameter(getText());
		return result;
	}
	
}
