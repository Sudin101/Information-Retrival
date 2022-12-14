
package com.annotators;

//This file is annotator file required for Apache UIMA to extract tokens

import java.text.BreakIterator;
import java.text.ParsePosition;
import java.util.Locale;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * An example annotator that annotates Tokens and Sentences.
 */
public class TokenAnnotator extends JCasAnnotator_ImplBase {

	static abstract class Maker {
		abstract Annotation newAnnotation(JCas jcas, int start, int end);
	}

	JCas jcas;

	String input;

	ParsePosition pp = new ParsePosition(0);

	static final BreakIterator wordBreak = BreakIterator.getWordInstance(Locale.US);

	static final Maker tokenAnnotationMaker = new Maker() {
		Annotation newAnnotation(JCas jcas, int start, int end) {
			return new Token(jcas, start, end);
		}
	};

	// *************************************************************
	// * process *
	// *************************************************************
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		jcas = aJCas;
		input = jcas.getDocumentText();

		// Create Annotations
		makeAnnotations(tokenAnnotationMaker, wordBreak);
	}

	// *************************************************************
	// * Helper Methods *
	// *************************************************************
	void makeAnnotations(Maker m, BreakIterator b) {
		b.setText(input);
		for (int end = b.next(), start = b.first(); end != BreakIterator.DONE; start = end, end = b
				.next()) {
			// eliminate all-whitespace tokens
			boolean isWhitespace = true;
			for (int i = start; i < end; i++) {
				if (!Character.isWhitespace(input.charAt(i))) {
					isWhitespace = false;
					break;
				}
			}
			if (!isWhitespace) {
				m.newAnnotation(jcas, start, end).addToIndexes();
			}
		}
	}
}

