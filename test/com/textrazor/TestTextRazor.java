package com.textrazor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.textrazor.annotations.Custom;
import com.textrazor.annotations.Entity;
import com.textrazor.annotations.AnalyzedText;
import com.textrazor.annotations.Sentence;
import com.textrazor.annotations.Word;
import com.textrazor.annotations.Word.Sense;
import com.textrazor.dictionary.DictionaryManager;
import com.textrazor.dictionary.model.Dictionary;
import com.textrazor.dictionary.model.DictionaryEntry;
import com.textrazor.dictionary.model.PagedAllEntries;

public class TestTextRazor {

	public static void testDictionary(String apiKey) throws NetworkException, AnalysisException {
		DictionaryManager dictionaryManager = new DictionaryManager(apiKey);
	
		// Delete all dictionaries in our account.
		{
			for (Dictionary dict : dictionaryManager.allDictionaries()) {
				System.out.println("Deleting current dictionary: " + dict.getId());
				dictionaryManager.deleteDictionary(dict);
			}
		}

		// Create a new dictionary
		
		Dictionary newDict = Dictionary.builder().setId("developers").build();

		dictionaryManager.createDictionary(newDict);

		{
			List<DictionaryEntry> newEntries = new ArrayList<DictionaryEntry>();

			List<String> types = Arrays.asList("cpp_developer", "writer");

			newEntries.add(DictionaryEntry.builder().setText("Bjarne Stroustrup").setId("DEV2").addData("types", types).build());

			dictionaryManager.addEntries(newDict.getId(), newEntries);
		}
		
		// Loop over all entries in a specific dictionary
		PagedAllEntries allEntries = dictionaryManager.allEntries(newDict.getId());

		System.out.println("Dictionary contains : " + allEntries.getTotal() + " total entries.");
		
		for (DictionaryEntry entry : allEntries.getEntries()) {
			System.out.println("Dictionary entry in 'developers':");
			System.out.println(entry.getId() + " " + entry.getText());
		}
		
		// Retrieve them directly by ID
		{
			DictionaryEntry entry = dictionaryManager.getEntry(newDict.getId(), "DEV2");
			System.out.println("Entry text: " + entry.getText());
		}
		
		// Try extracting the new entry from some sample text.
		TextRazor client = new TextRazor(apiKey);
		
		client.setEntityDictionaries(Arrays.asList(newDict.getId()));
		client.addExtractor("entities");
		
		AnalyzedText response = client.analyze("Although it is very early in the process, higher-level parallelism is slated to be a key theme of the next version of C++, says Bjarne Stroustrup");
		
		for (Entity entity : response.getResponse().getEntities()) {
			String customEntityId = entity.getCustomEntityId();
			if (null != customEntityId) {
				System.out.println("Found custom entity: " + customEntityId);
				for (String type : entity.getData().get("types")) {
					System.out.println("Type: " + type);
				}
			}
		}
	}

	public static void testAnalysis(String apiKey) throws NetworkException, AnalysisException {
		// Sample request, showcasing a couple of TextRazor features
		
		TextRazor client = new TextRazor(apiKey);
		
		client.addExtractor("words");
		client.addExtractor("entities");
		client.addExtractor("entailments");
		client.addExtractor("senses");
		client.addExtractor("phrases");
		client.addExtractor("dependency-trees");
		client.addExtractor("relations");
		client.addExtractor("entity_companies");
		
		client.setEnrichmentQueries(Arrays.asList("fbase:/location/location/geolocation>/location/geocode/latitude", "fbase:/location/location/geolocation>/location/geocode/longitude"));

		String rules = "entity_companies(CompanyEntity) :- entity_type(CompanyEntity, 'Company').";
		client.setRules(rules);

		AnalyzedText response = client.analyze("LONDON - Barclays misled shareholders and the public RBS about one of the biggest investments in the bank's history, a BBC Panorama investigation has found.");

		System.out.println(response.isOk());
		
		for (Sentence sentence : response.getResponse().getSentences()) {
			for (Word word : sentence.getWords()) {

				System.out.println("----------------");
				System.out.println("Word: " + word.getToken());

				for (Word child : word.getChildren()) {
					System.out.println("Child: " + child.getToken());
				}


				for (Entity entity : word.getEntities()) {
					System.out.println("Matched Entity: " + entity.getEntityEnglishId());

					Map<String, List<String>> entityData = entity.getData();
					List<String> latitudeValues = entityData.get("fbase:/location/location/geolocation>/location/geocode/latitude");
					List<String> longitudeValues = entityData.get("fbase:/location/location/geolocation>/location/geocode/longitude");

					if (null != latitudeValues) {
						System.out.println("Entity latitude: " + latitudeValues.get(0));
					}

					if (null != longitudeValues) {
						System.out.println("Entity longitude: " + longitudeValues.get(0));
					}
				}

				for (Sense sense: word.getSenses()) {
					System.out.println("Word sense: " + sense.getSynset() + " has score: " + sense.getScore());
				}
			}
		}

		// Use a custom rule to match 'Company' type entities
		
		for (Custom custom : response.getResponse().getCustomAnnotations()) {

			for (Custom.BoundVariable variable : custom.getContents()) {

				if (null != variable.getEntityValue()) {
					for (Entity entity : variable.getEntityValue()) {
						System.out.println("Variable: " + variable.getKey() + " Value:" + entity.getEntityId());
					}
				}
			}
		}
		 
	}
	
	/**
	 * @param args
	 * @throws NetworkException 
	 */
	public static void main(String[] args) throws NetworkException, AnalysisException {
		String API_KEY = "YOUR_API_KEY_HERE";

		testDictionary(API_KEY);
		testAnalysis(API_KEY);
	}

}
