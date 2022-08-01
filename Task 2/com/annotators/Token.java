

package com.annotators;

//This file is annotator file required for Apache UIMA to extract tokens

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.tcas.Annotation;

public class Token extends Annotation {

	public final static int typeIndexID = JCasRegistry.register(Token.class);

	public final static int type = typeIndexID;

	public int getTypeIndexID() {
		return typeIndexID;
	}

	// Never called. Disable default constructor
	protected Token() {
	}

	/** Internal - Constructor used by generator */
	public Token(int addr, TOP_Type type) {
		super(addr, type);
	}

	public Token(JCas jcas) {
		super(jcas);
	}

	public Token(JCas jcas, int start, int end) {
		super(jcas, start, end);
	}
}
