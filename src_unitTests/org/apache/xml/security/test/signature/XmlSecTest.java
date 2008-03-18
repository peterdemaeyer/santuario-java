/*
 * Copyright 2008 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.xml.security.test.signature;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.apache.xml.security.Init;
import org.apache.xml.security.algorithms.MessageDigestAlgorithm;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.transforms.params.XPathContainer;
import org.apache.xml.security.utils.Constants;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import junit.framework.TestCase;

/**
 * Tests creating and validating an XML Signature with an XPath Transform.
 * Tests bug #44617.
 *
 * @author Frank Cornelis
 */
public class XmlSecTest extends TestCase {

    static org.apache.commons.logging.Log log =
        org.apache.commons.logging.LogFactory.getLog
            (XmlSecTest.class.getName());

    public void testCheckXmlSignatureSoftwareStack() throws Exception {
	Init.init();
	DocumentBuilderFactory documentBuilderFactory = 
	    DocumentBuilderFactory.newInstance();
	documentBuilderFactory.setNamespaceAware(true);
	DocumentBuilder documentBuilder = 
	    documentBuilderFactory.newDocumentBuilder();
	Document testDocument = documentBuilder.newDocument();

	Element rootElement = 
	    testDocument.createElementNS("urn:namespace", "tns:document");
	rootElement.setAttributeNS
	    (Constants.NamespaceSpecNS, "xmlns:tns", "urn:namespace");
	testDocument.appendChild(rootElement);
	Element childElement = 
	    testDocument.createElementNS("urn:childnamespace", "t:child");
	childElement.setAttributeNS
	    (Constants.NamespaceSpecNS, "xmlns:t", "urn:childnamespace");
	childElement.appendChild(testDocument.createTextNode("hello world"));
	rootElement.appendChild(childElement);

	KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
	PublicKey publicKey = keyPair.getPublic();
	PrivateKey privateKey = keyPair.getPrivate();

	XMLSignature signature = new XMLSignature(testDocument, "",
				XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256,
				Canonicalizer.ALGO_ID_C14N_WITH_COMMENTS);

	Element signatureElement = signature.getElement();
	rootElement.appendChild(signatureElement);

	Transforms transforms = new Transforms(testDocument);
	XPathContainer xpath = new XPathContainer(testDocument);
	xpath.setXPathNamespaceContext("ds", Constants.SignatureSpecNS);
	xpath.setXPath("not(ancestor-or-self::ds:Signature)");
	transforms.addTransform(Transforms.TRANSFORM_XPATH, xpath
				.getElementPlusReturns());
	transforms.addTransform(Transforms.TRANSFORM_C14N_WITH_COMMENTS);
	signature.addDocument("", transforms,
				MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA1);

	signature.addKeyInfo(publicKey);

	Element nsElement = testDocument.createElementNS(null, "nsElement");
	nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:ds",
				Constants.SignatureSpecNS);

	signature.sign(privateKey);

	TransformerFactory tf = TransformerFactory.newInstance();
	Transformer t = tf.newTransformer();
	t.transform(new DOMSource(testDocument), new StreamResult(System.out));

	NodeList signatureElems = XPathAPI.selectNodeList(testDocument,
				"//ds:Signature", nsElement);
	signatureElement = (Element) signatureElems.item(0);
	XMLSignature signatureToVerify = new XMLSignature(signatureElement, "");

	boolean signResult = signatureToVerify.checkSignatureValue(publicKey);

	assertTrue(signResult);
    }
}
