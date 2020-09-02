package com.citi.jwt.controller;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;

import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.citi.jwt.feign.FeignRequest;
import com.citi.jwt.feign.FeignResponse;
import com.citi.jwt.service.CitiJwtReceiverService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(value = "/mock")
@Slf4j
public class ReceiverController {

	private static final String PATH_TEST_JWT = "/pdf/serialized";

	private static final String PATH_RETRIEVE_PURCHASE_ORDER_PDF = "${app.path-retrieve-purchaseorder-pdf}";
	
	@Autowired
	private CitiJwtReceiverService citiJwtService; 
	
	@PostMapping(value = PATH_TEST_JWT, produces = "application/json")
	public ResponseEntity<FeignResponse> getPurchaseOrder(
			@RequestBody FeignRequest request,
			@RequestHeader Map<String, String> headers) 
			throws NoSuchAlgorithmException, InvalidKeySpecException, JoseException, IOException {
		//log.info(":::::::::: Controller Receiver: {} **** {} ", request, headers.get("authorization"));
		return new ResponseEntity<>(citiJwtService.decodeJwt(headers.get("authorization")), HttpStatus.OK);
	}

	@PostMapping(value = "/citi/jwt/receiver/noencryption", produces = "application/json")
	public ResponseEntity<FeignResponse> getPurchaseOrderNoEncryption(
			@RequestBody FeignRequest request,
			@RequestHeader Map<String, String> headers) 
			throws NoSuchAlgorithmException, InvalidKeySpecException, JoseException, IOException {
		//log.info(":::::::::: Controller Receiver no encryption: {} ", request);
		return new ResponseEntity<>(citiJwtService.executeNoEncryption(), HttpStatus.OK);
	}

	@GetMapping(value = PATH_RETRIEVE_PURCHASE_ORDER_PDF, produces = "application/pdf")
	public ResponseEntity<byte[]> getPurchaseOrderPdf(
			@PathVariable(name = "idPurchaseOrder") Integer idPurchaseOrder) {
		log.info("::::::::::: Get Purchase Order Status Handler {} ", idPurchaseOrder);
		
		return new ResponseEntity<>(null, HttpStatus.OK);
	}

}
