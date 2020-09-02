package com.citi.jwt.service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.jose4j.lang.JoseException;

import com.citi.jwt.feign.FeignResponse;

public interface CitiJwtReceiverService {
	
	FeignResponse decodeJwt(String message) 
			throws NoSuchAlgorithmException, InvalidKeySpecException, JoseException, IOException;
		
	FeignResponse executeNoEncryption();
}
