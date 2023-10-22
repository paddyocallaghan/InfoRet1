package x;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SearchFiles {

    public void Search(String index_in, String scoring, String queries_in, String args_in)
            throws Exception {

        String queryString = null;
        int hitsPerPage = 10;
        int setScore = 0;
        float lambda = (float) 0.7;
        String nextLine = "";
        int N_query = 1;
        setScore = Integer.parseInt(scoring);
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index_in)));
        IndexSearcher searcher = new IndexSearcher(reader);

        if (setScore == 0)
            searcher.setSimilarity(new ClassicSimilarity());
        if (setScore == 1)
            searcher.setSimilarity(new BM25Similarity());
        if (setScore == 2)
            searcher.setSimilarity(new LMJelinekMercerSimilarity(lambda));

        BufferedReader in = null;
        if (queries_in != null) {
            in = Files.newBufferedReader(Paths.get(queries_in), StandardCharsets.UTF_8);
        } else {
            in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        }

        Analyzer analyzer = new EnglishAnalyzer();
        String[] fields = { "Title", "Author", "bib", "Words" };
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
        String line = in.readLine();
        PrintWriter writer = new PrintWriter(args_in + "outputs.txt", "UTF-8");

        while (true) {
            if (line.substring(0, 2).equals(".I")) {
                line = in.readLine();
                if (line.equals(".W")) {
                    line = in.readLine();
                }
                nextLine = "";
                while (!line.substring(0, 2).equals(".I")) {
                    nextLine = nextLine + " " + line;
                    line = in.readLine();
                    if (line == null)
                        break;
                }
            }
            Query query = parser.parse(QueryParser.escape(nextLine.trim()));
            doPagingSearch(in, searcher, query, hitsPerPage, writer, N_query);
            N_query++;
            if (queryString != null) {
                break;
            }
        }
        writer.close();
        reader.close();
    }

    public static void doPagingSearch(BufferedReader in, IndexSearcher searcher, Query query,
            int hitsPerPage, PrintWriter writer, int N_query) throws IOException {
        TopDocs results = searcher.search(query, 5 * hitsPerPage);
        int numTotalHits = Math.toIntExact(results.totalHits.value);
        results = searcher.search(query, numTotalHits);
        ScoreDoc[] hits = results.scoreDocs;
        for (int i = 0; i < numTotalHits; i++) {
            Document doc = searcher.doc(hits[i].doc);
            if (doc.get("path") != null) {
                writer.println(N_query + " 0 " + doc.get("path").replace(".I ", "") + " " + (i + 1) + " "
                        + hits[i].score + " Any");
            }
        }
    }
}
