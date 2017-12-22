import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import utility.Tuple;

import java.util.Set;

public class ProbabilisticAnnotationV3
{
	//pre calculate stuff we need. this will make code execute about 10 times faster
	private int /*#(v, j)*/times_visual_word_is_found_per_image[][];/*search_results X visual_words*/
	private int /*#(v, T)*/total_times_visual_word_is_found[];/*visual_words*/

	private int /*#(w, j)*/times_text_word_is_found_per_image[][];/*search_results X text_words*/
	private int /*#(w, T)*/total_times_text_word_is_found[];/*text_words*/
	
	//private int /*|J|*/aggregate_count_all_text_and_visual_words_per_image[];/*search_results*/
	private int /*|J| for text*/sum_of_all_text_words_per_image[];/*search_results*/
	private int /*|J| for visual words*/sum_of_all_visual_words_per_image[];/*search_results*/
	
	private String distinct_words_list[];//we will only refer to a word by it's index during calculations
	private String images_returned_from_train[];//we will only refer to a result image by it's index
	//|T| (total images on set) = images_returned_from_train.length
	private int visual_words_number;
	
	//also, precalculate some stuff we use more often
	private double precalculated_p_w_j[][];//search_results X text_words
	private double precalculated_p_v_j[][];//search_results X visual_words
	//done
	
	private int times_visual_words_found_on_input_image[];//visual_words
	private int sum_of_all_times_visual_words_found_on_input_image;
	private double precalculated_p_v_g[];//search_results X visual_words
	
    private Map<String,Double> retrievalResults;
	
	private void init(int imgsPerQuery, String valID, RetrievalResults retResults, Map<String, List<String>> train)
	//this is generally very fast. I could actually do it in maybe 2 loops but the code will be hard to read and speed is not an issue here
	{
		retrievalResults = new HashMap<>();
		List<Tuple<String, Double>> searchResults = retResults.getSortedSeachResults(valID);
				
		if(searchResults.size() > imgsPerQuery)
			searchResults = searchResults.subList(0, imgsPerQuery);
		
		for(Tuple<String,Double> t:searchResults)
			retrievalResults.put(t.A, t.B);
		
		if(retResults.hasVwords)
		{
			this.visual_words_number = retResults.getTrainingVWordFrequency(searchResults.get(0).A).size();
			//first calculate the times visual words are found
			times_visual_word_is_found_per_image = new int[searchResults.size()][];
			//total_times_visual_word_is_found = new int[visual_words_number];

			for(int i = 0; i < searchResults.size(); i++)//for every image LIRE search returned (all from the training set)
			{
					times_visual_word_is_found_per_image[i] = new int[visual_words_number];
					List<Integer> tmp = retResults.getTrainingVWordFrequency(searchResults.get(i).A);

					for(int visual_word_index = 0; visual_word_index < visual_words_number; visual_word_index++)
					{
							times_visual_word_is_found_per_image[i][visual_word_index] = tmp.get(visual_word_index);
							//total_times_visual_word_is_found[visual_word_index] += tmp.get(visual_word_index);
					}
			}
			
			//caclaulte freuqncies for input image
			sum_of_all_times_visual_words_found_on_input_image = 0;
			List<Integer> tmp = retResults.getValidationVWordFrequency(valID);
			times_visual_words_found_on_input_image = new int[visual_words_number];
			
			for(int visual_word_index = 0; visual_word_index < visual_words_number; visual_word_index++)
			{
				times_visual_words_found_on_input_image[visual_word_index] = tmp.get(visual_word_index);
				sum_of_all_times_visual_words_found_on_input_image += tmp.get(visual_word_index);
			}
			
		}
		else
			this.visual_words_number = 0;
        
		//done
		
		//then calculate the distinct words found
                Set<String> tmp_ =new HashSet<String>();
                for(Tuple<String, Double> val : searchResults){
                    tmp_.addAll(train.get(val.A));
                }
                distinct_words_list = tmp_.toArray(new String[tmp_.size()]);
		//done
		
		//then convert the search results to String array
		images_returned_from_train = new String[searchResults.size()];
		
		for(int image_index = 0; image_index < images_returned_from_train.length; image_index++)
			images_returned_from_train[image_index] = searchResults.get(image_index).A;
		//done
		
		//now, time to calculate how many times a text word is found per image
		int text_words = distinct_words_list.length;
		
		times_text_word_is_found_per_image = new int[searchResults.size()][];
		total_times_text_word_is_found = new int[text_words];
		
		for(int image_index = 0; image_index < images_returned_from_train.length; image_index++)
		{
			times_text_word_is_found_per_image[image_index] = new int[text_words];
			List<String> tmp = train.get(searchResults.get(image_index).A);
			
			for(int text_word_index = 0; text_word_index < text_words; text_word_index++)
			{
				int does_contain = tmp.contains(distinct_words_list[text_word_index]) ? 1: 0;
				times_text_word_is_found_per_image[image_index][text_word_index] = does_contain;
				total_times_text_word_is_found[text_word_index] += does_contain;
			}
		}
		//done
		
		sum_of_all_text_words_per_image = new int[images_returned_from_train.length];
		sum_of_all_visual_words_per_image = new int[images_returned_from_train.length];
		
		for(int image_index = 0; image_index < images_returned_from_train.length; image_index++)
		{
			for(int visual_word_index = 0; visual_word_index < visual_words_number; visual_word_index++)
				sum_of_all_visual_words_per_image[image_index] += times_visual_word_is_found_per_image[image_index][visual_word_index];
			
			for(int text_word_index = 0; text_word_index < text_words; text_word_index++)
				sum_of_all_text_words_per_image[image_index] += times_text_word_is_found_per_image[image_index][text_word_index];
		}
		
		//time to pre calculate some extra stuff
		precalculated_p_v_j = new double[images_returned_from_train.length][];
		precalculated_p_w_j = new double[images_returned_from_train.length][];
		
		for(int image_index = 0; image_index < images_returned_from_train.length; image_index++)
		{
			precalculated_p_w_j[image_index] = new double[text_words];
			for(int text_word_index = 0; text_word_index < text_words; text_word_index++)
				precalculated_p_w_j[image_index][text_word_index] = calculate_p_w_j(image_index, text_word_index);
			
			precalculated_p_v_j[image_index] = new double[visual_words_number];
			for(int visual_word_index = 0; visual_word_index < visual_words_number; visual_word_index++)
				precalculated_p_v_j[image_index][visual_word_index] = calculate_p_v_j(image_index, visual_word_index);
		}
		//done
		
		precalculated_p_v_g = new double[visual_words_number];
		for(int visual_word_index = 0; visual_word_index < visual_words_number; visual_word_index++)
			precalculated_p_v_g[visual_word_index] = calculate_p_v_g(visual_word_index);
	}
	
	//
	//first, calculate p(w|j) and p(v|j) using the precalculated tables
    private double /*p(w|j)*/calculate_p_w_j(int image_index, int text_word_index)
    {
    	//we are calculating here: P(w|j) = #(w,j)/|j|
    	
    	double //declare it double so divisions bellow work as expected
    		no_w_j = times_text_word_is_found_per_image[image_index][text_word_index],
    		_j_ = sum_of_all_text_words_per_image[image_index];
    	
    	return _j_ > 0 ? no_w_j/_j_ : 0;
    }
    
    private double /*p(v|j)*/calculate_p_v_j(int image_index, int visual_word_index)
    {
    	//we are calculating here: P(v|j) = #(v,j)/|j|
    	
    	double //declare it double so divisions bellow work as expected
			no_v_j = times_visual_word_is_found_per_image[image_index][visual_word_index],
			_j_ = sum_of_all_visual_words_per_image[image_index];
    	
    	return _j_ > 0 ? no_v_j/_j_ : 0;
    }
	//done
    
    private double /*p(v|g)*/calculate_p_v_g(int visual_word_index)
    {
    	double //declare it double so divisions bellow work as expected
			no_v_j = times_visual_words_found_on_input_image[visual_word_index],
			_j_ = sum_of_all_times_visual_words_found_on_input_image;
    	
    	return _j_ > 0 ? no_v_j/_j_ : 0;
    }
	
	//
	//now we are going to do the calculations using the pre-calculated tables from above
	//we can easily pre calculate the following too, but they depend on the algorithm, so better not optimize something that can change easily
	////////////////////////////////////////////////////////////////////////////////////
    
    //now, to the final, for a given text word calculate it's score
    private double calculate_text_word_score(int text_word_index)
    {
    	double total_sum = 0;
    	
    	for(int image_index = 0; image_index < images_returned_from_train.length; image_index++)//for every image LIRE returned
    	{
    		double temp_sum = 0;
    		
    		for(int visual_word_index = 0; visual_word_index < visual_words_number; visual_word_index++)
    		{
    			if(times_visual_words_found_on_input_image[visual_word_index] > 0)
    			{
            		double tmp_1 = precalculated_p_v_j[image_index][visual_word_index];
            		
            		if(tmp_1 != 0)
            			temp_sum += precalculated_p_v_g[visual_word_index]*Math.log(tmp_1);
    			}
    		}
    		
    		total_sum += precalculated_p_w_j[image_index][text_word_index]*temp_sum;
    	}
    	
    	return -total_sum;
    }
    ///////////////////////////////////////////////////////////////////////////////////

    private ArrayList<Tuple<String, Double>> run_algorithm()
    {
    	HashMap<Integer/*word index*/, Double/*word score*/> scored_words = new HashMap<Integer, Double>();//store word index and score pairs here
    	
    	for(int text_word_index = 0; text_word_index < distinct_words_list.length; text_word_index++)
    		scored_words.put(text_word_index, calculate_text_word_score(text_word_index));
    	
    	Map<Integer, Double> sorted_scored_words = sortByValue(scored_words);//sort the result pairs
    	
    	ArrayList<Tuple<String, Double>> result = new ArrayList<Tuple<String, Double>>();
    	
    	for (Map.Entry<Integer, Double> entry : sorted_scored_words.entrySet())
    	{
    		String actual_text_word = distinct_words_list[entry.getKey()];
    		result.add(new Tuple<String, Double>(actual_text_word, entry.getValue()));
    	}
    	
    	return result;
    }
    
    public ArrayList<Tuple<String, Double>> generate_concepts(
			String validation_image_id, 
			RetrievalResults ret_results, 
			Map<String, List<String>> train, 
			double alpha,
			double beta,
			int max_number_of_search_results_to_use)
	{
    	init(max_number_of_search_results_to_use, validation_image_id, ret_results, train);
    	return run_algorithm();
	}
    
	private static <K, V> Map<K, V> sortByValue(Map<K, V> map)//desc
	{
	    List<Entry<K, V>> list = new LinkedList<>(map.entrySet());
	    Collections.sort(list, new Comparator<Object>() {
	        @SuppressWarnings("unchecked")
	        public int compare(Object o1, Object o2) {
	            return ((Comparable<V>) ((Map.Entry<K, V>) (o2)).getValue()).compareTo(((Map.Entry<K, V>) (o1)).getValue());
	        }
	    });
	    
	    Map<K, V> result = new LinkedHashMap<>();
	    for (Iterator<Entry<K, V>> it = list.iterator(); it.hasNext();) {
	        Map.Entry<K, V> entry = (Map.Entry<K, V>) it.next();
	        result.put(entry.getKey(), entry.getValue());
	    }
	    
	    return result;
	}
}