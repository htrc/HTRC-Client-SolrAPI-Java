package edu.indiana.d2i.htrc.clients.solr;

import static org.junit.Assert.fail;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class HTRCSolrClientTest {
    HTRCSolrClient client = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
//		client = new HTRCSolrClient("http://coffeetree.cs.indiana.edu:9994/solr");
        client = new HTRCSolrClient("http://chinkapin.pti.indiana.edu:9994/solr/meta");
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetVolumeIDsString() {
//		String queryurl = "title:Abraham Lincoln. AND publishDate:1920";
        String queryurl = "title:abraham lincoln&rows=0";
        try {
            List<String> volumeIDs = client.getVolumeIDs(queryurl);
            System.out.println("The query returned " + volumeIDs.size() + " volumes");
//			for (String volumeId : volumeIDs) System.out.println(volumeId);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }


    private static class ValueComparator<K, V extends Comparable<V>> implements Comparator<K> {
        private final Map<K, V> data;
        public ValueComparator(Map<K, V> data) {this.data = data;}
        @Override
        public int compare(K o1, K o2) {
            V count1 = data.get(o1);
            V count2 = data.get(o2);
            if (count1.compareTo(count2) == 1) return -1;
            else if (count1.compareTo(count2) == -1) return 1;
            else return 0;
        }
    }


//    @Test
//    public void testGetWordCound() {
//		String queryurl = "title:Abraham Lincoln. AND publishDate:1920";
////		String queryurl = "author:Raymond AND publishDate:1884&tv=on&tv.tf=true&qt=tvrh&start=0&rows=2&fl=id";
//		try {
//			Map<String, Integer> wordCound = client.getWordCount(queryurl);
//			SortedMap<String, Integer> sortedData = new TreeMap<String, Integer>(new ValueComparator<String, Integer>(wordCound));
//			sortedData.putAll(wordCound);
//
//			System.out.println(wordCound.size());
//			FileWriter writer = new FileWriter("wordcount.txt");
//			Iterator<String> iterator = wordCound.keySet().iterator();
//			while (iterator.hasNext()) {
//				String word = iterator.next();
//				int count = wordCound.get(word);
//				writer.write(word + "\t" + count + "\n");
//			}
//			writer.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//			System.out.println("bug!!!");
//		} catch (XMLStreamException e) {
//			e.printStackTrace();
//		}

//		String id = "loc.ark+=13960=t2t444175";

//		List<String> list = new ArrayList<String>();
//		list.add("loc.ark+=13960=t4wh3b18q");
//		list.add("nnc2.ark+=13960=t75t4d419");
//		list.add("uc2.ark+=13960=t7pn99h29");
//		try {
//			Map<String, Integer> wordCount = client.getWordCountInCountOrderFromSolrFilter(list, false);
//			System.out.println(wordCount.size());
//			FileWriter writer = new FileWriter("wordcount.txt");
//			Iterator<String> iterator = wordCount.keySet().iterator();
//			while (iterator.hasNext()) {
//				String word = iterator.next();
//				int count = wordCount.get(word);
//				writer.write(word + "\t" + count + "\n");
//			}
//			writer.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (XMLStreamException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//    }
}
