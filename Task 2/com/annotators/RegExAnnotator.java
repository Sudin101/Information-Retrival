
package com.annotators;

//This file is annotator file required for Apache UIMA to extract features based on regex

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.analysis_engine.annotator.TextAnnotator;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FSTypeConstraint;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.ResourceAccessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

public class RegExAnnotator extends CasAnnotator_ImplBase {
	public static final String MESSAGE_DIGEST = "org.apache.uima.examples.cas.RegExAnnotator_Messages";

	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		try {
			// Retrieve configuration parameters
			String[] patternStrings = (String[]) getContext().getConfigParameterValue("Patterns");
			String[] typeNames = (String[]) getContext().getConfigParameterValue("TypeNames");
			mContainingAnnotationTypeNames = (String[]) getContext().getConfigParameterValue(
					"ContainingAnnotationTypes");
			if (mContainingAnnotationTypeNames != null && mContainingAnnotationTypeNames.length > 0) {
				mAnnotateEntireContainingAnnotation = (Boolean) getContext().getConfigParameterValue(
						"AnnotateEntireContainingAnnotation");
			} else {
				mAnnotateEntireContainingAnnotation = Boolean.FALSE;
			}

			// create an ArrayList of type names and an ArrayList of pattern arrays,
			// where the indexes of the two lists corespond so that the patterns
			// at patternArray[i] correspond to the annotation type at
			// mTypeNames[i].
			mTypeNames = new ArrayList();
			ArrayList patternArray = new ArrayList();
			if (patternStrings != null) {
				if (typeNames == null || typeNames.length != patternStrings.length) {
					// throw exception - error message in external message digest
					throw new ResourceInitializationException(MESSAGE_DIGEST,
							"type_pattern_array_length_mismatch", new Object[0]);
				}
				mTypeNames.addAll(Arrays.asList(typeNames));

				for (int i = 0; i < patternStrings.length; i++) {
					patternArray.add(new String[] { patternStrings[i] });
				}
			}

			// if PatternFile resource exists, parse it and add to patternArray
			InputStream in = getContext().getResourceAsStream("PatternFile");
			if (in != null) {
				try {
					ArrayList patternsForCurrentType = new ArrayList();
					boolean foundFirstType = false;
					// get buffered reader
					BufferedReader reader = new BufferedReader(new InputStreamReader(in));

					// read lines from file
					String line = reader.readLine();
					while (line != null) {
						if (!line.startsWith("#") && line.length() > 0
								&& !Character.isWhitespace(line.charAt(0))) {
							// line is not a comment
							if (line.startsWith("%")) // annotation type name
							{
								// add pattern array for previous type (if any) to list
								if (foundFirstType) {
									String[] pats = new String[patternsForCurrentType.size()];
									patternsForCurrentType.toArray(pats);
									patternArray.add(pats);
									patternsForCurrentType.clear();
								}
								// add new type name to mTypeNames list
								mTypeNames.add(line.substring(1));
								foundFirstType = true;
							} else // treat as regular expression
							{
								patternsForCurrentType.add(line);
							}
						}
						line = reader.readLine();
					}
					// add last group of pattersn to patternArray
					String[] pats = new String[patternsForCurrentType.size()];
					patternsForCurrentType.toArray(pats);
					patternArray.add(pats);
				} finally {
					if (in != null) {
						in.close();
					}
				}
			}

			// make sure there is at least one pattern
			if (patternArray.isEmpty()) {
				throw new ResourceInitializationException(
						AnnotatorConfigurationException.ONE_PARAM_REQUIRED,
						new Object[] { "Patterns, Pattern File" });
			}

			// compile regular expression patterns
			mPatterns = new Pattern[patternArray.size()][];
			for (int i = 0; i < patternArray.size(); i++) {
				String[] pats = (String[]) patternArray.get(i);
				mPatterns[i] = new Pattern[pats.length];
				for (int j = 0; j < mPatterns[i].length; j++) {
					try {
						mPatterns[i][j] = Pattern.compile(pats[j]);
						// make sure no pattern matches the empty string - as this
						// would lead to infinite loops during processing
						if (mPatterns[i][j].matcher("").matches()) {
							throw new ResourceInitializationException(MESSAGE_DIGEST,
									"regex_matches_empty_string", new Object[] { pats[j] });
						}
					} catch (PatternSyntaxException e) {
						throw new ResourceInitializationException(MESSAGE_DIGEST, "regex_syntax_error",
								new Object[] { pats[j] }, e);
					}
				}
			}
		} catch (ResourceAccessException e) {
			throw new ResourceInitializationException(e);
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}
	}

	public void typeSystemInit(TypeSystem aTypeSystem) throws AnalysisEngineProcessException {
		// get references to annotation types we will create
		mCASTypes = new Type[mTypeNames.size()];
		for (int i = 0; i < mTypeNames.size(); i++) {
			String curTypeName = (String) mTypeNames.get(i);
			mCASTypes[i] = aTypeSystem.getType(curTypeName);
			if (mCASTypes[i] == null) {
				throw new AnalysisEngineProcessException(AnnotatorInitializationException.TYPE_NOT_FOUND,
						new Object[] { this.getClass().getName(), curTypeName });
			}
		}

		// get references to Containing Annotation Types
		if (mContainingAnnotationTypeNames == null) {
			mContainingAnnotationTypes = null;
		} else {
			mContainingAnnotationTypes = new Type[mContainingAnnotationTypeNames.length];
			for (int i = 0; i < mContainingAnnotationTypes.length; i++) {
				mContainingAnnotationTypes[i] = aTypeSystem.getType(mContainingAnnotationTypeNames[i]);
				if (mContainingAnnotationTypes[i] == null) {
					throw new AnalysisEngineProcessException(AnnotatorInitializationException.TYPE_NOT_FOUND,
							new Object[] { getClass().getName(), mContainingAnnotationTypeNames[i] });
				}
			}
		}
	}

	
	public void process(CAS aCAS) throws AnalysisEngineProcessException {
		try {
			String docText = aCAS.getDocumentText();
			// Determine which regions of the document we are going to annotate
			int[] rangesToAnnotate = getRangesToAnnotate(aCAS);

			// We treat the rangesToAnnotate array as a list of (start,end) offset
			// pairs. Iterate through all of these pairs.
			for (int i = 0; i < rangesToAnnotate.length; i += 2) {
				int startPos = rangesToAnnotate[i];
				int endPos = rangesToAnnotate[i + 1];
				// get the substring of text to be annotated
				String subText = docText.substring(startPos, endPos);

				// iterate over all annotation types for which we have patterns
				for (int j = 0; j < mCASTypes.length; j++) {
					// see if the ResultSpec contains this type
					if (getResultSpecification().containsType(mCASTypes[j].getName(),aCAS.getDocumentLanguage()) || getResultSpecification().containsType(mCASTypes[j].getName())) {
						// try to match each pattern that we have for this annotation type
						for (int k = 0; k < mPatterns[j].length; k++) {
							int pos = 0;
							Matcher matcher = mPatterns[j][k].matcher(subText);
							while (pos < subText.length() && matcher.find(pos)) {
								getContext().getLogger().log(Level.FINER,
										"RegEx match found: [" + matcher.group() + "]");
								// match found; extract locations of start and end of match
								// (or of entire containing annotation, if that option is on)
								int annotStart, annotEnd;
								if (mAnnotateEntireContainingAnnotation.booleanValue()) {
									annotStart = startPos;
									annotEnd = endPos;
								} else {
									annotStart = startPos + matcher.start();
									annotEnd = startPos + matcher.end();
								}
								// create Annotation in CAS
								FeatureStructure fs = aCAS.createAnnotation(mCASTypes[j], annotStart, annotEnd);
								aCAS.getIndexRepository().addFS(fs);
								pos = annotEnd - startPos;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

	
	protected int[] getRangesToAnnotate(CAS aCAS) {
		if (mContainingAnnotationTypes == null || mContainingAnnotationTypes.length == 0) {
			// ContainingAnnotationTypes is not set - the whole document is eligible
			return new int[] { 0, aCAS.getDocumentText().length() };
		} else {
			// get iterator over all annotations in the CAS
			FSIterator iterator = aCAS.getAnnotationIndex().iterator();

			// filter the iterator so that only instances of Types in the
			// mContainingAnnotationTypes array are returned
			FSTypeConstraint constraint = aCAS.getConstraintFactory().createTypeConstraint();
			for (int i = 0; i < mContainingAnnotationTypes.length; i++) {
				constraint.add(mContainingAnnotationTypes[i]);
			}
			iterator = aCAS.createFilteredIterator(iterator, constraint);

			// iterate over annotations and add them to an ArrayList
			List annotationList = new ArrayList();
			while (iterator.isValid()) {
				annotationList.add(iterator.get());
				iterator.moveToNext();
			}

			// For each Annotation in the list, add its start and end
			// positions to the result array.
			int numRanges = annotationList.size();
			int[] result = new int[numRanges * 2];
			for (int j = 0; j < numRanges; j++) {
				AnnotationFS curFS = (AnnotationFS) annotationList.get(j);
				result[j * 2] = curFS.getBegin();
				result[j * 2 + 1] = curFS.getEnd();
			}
			return result;
		}
	}

	/**
	 * The regular expression Patterns to be matched.
	 */
	private Pattern[][] mPatterns;

	/**
	 * The names of the CAS types that this annotator produces from the patterns in {@link #mPatterns}.
	 */
	private ArrayList mTypeNames;

	/**
	 * The names of the CAS types within which this annotator will search for new annotations. This
	 * may be null, indicating that the entire document will be searched.
	 */
	private String[] mContainingAnnotationTypeNames;

	/**
	 * The CAS types corresponding to {@link #mTypeNames}.
	 */
	private Type[] mCASTypes;

	/**
	 * The CAS types corresponding to {@link #mContainingAnnotationTypeNames}.
	 */
	private Type[] mContainingAnnotationTypes;

	/**
	 * Whether to annotate the entire span of the containing annotation when a match is found.
	 */
	private Boolean mAnnotateEntireContainingAnnotation;

}
