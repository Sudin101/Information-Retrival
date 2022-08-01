

package com.annotators;

//This file is annotator file required for Apache UIMA to extract tokens

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.tcas.Annotation_Type;

public class Token_Type extends Annotation_Type {
	protected FSGenerator getFSGenerator() {
		return fsGenerator;
	}

	private final FSGenerator fsGenerator = new FSGenerator() {
		public FeatureStructure createFS(int addr, CASImpl cas) {
			if (instanceOf_Type.useExistingInstance) {
				// Return eq fs instance if already created
				FeatureStructure fs = instanceOf_Type.jcas.getJfsFromCaddr(addr);
				if (null == fs) {
					fs = new Token(addr, instanceOf_Type);
					instanceOf_Type.jcas.putJfsFromCaddr(addr, fs);
					return fs;
				}
				return fs;
			} else
				return new Token(addr, instanceOf_Type);
		}
	};

	public final static int typeIndexID = Token.typeIndexID;

	public final static boolean featOkTst = JCasRegistry.getFeatOkTst("org.apache.uima_examples.tokenizer.Token");

	// * initialize variables to correspond with Cas Type and Features
	public Token_Type(JCas jcas, Type casType) {
		super(jcas, casType);
		casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

	}

	protected Token_Type() { // block default new operator
		throw new RuntimeException("Internal Error-this constructor should never be called.");
	}

}
