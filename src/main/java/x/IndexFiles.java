package x;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class IndexFiles {

    public static void main(String[] args) {
        String indexPath = "index";
        String docsPath = null;
        String index_in = null;
        String queries_in = null;
        String scoring = null;
        String args_in = null;
        for (int i = 0; i < args.length; i++) {
            if ("-index".equals(args[i])) {
                indexPath = args[i + 1];
                index_in = indexPath;
                i++;
            } else if ("-docs".equals(args[i])) {
                docsPath = args[i + 1];
                i++;
            } else if ("-score".equals(args[i])) {
                scoring = args[i + 1];
                i++;
            } else if ("-queries".equals(args[i])) {
                queries_in = args[i + 1];
                i++;
            } else if ("-args_path".equals(args[i])) {
                args_in = args[i + 1];
                i++;
            }
        }

        final Path docDir = Paths.get(docsPath);
        try {
            System.out.println("Indexing to directory '" + indexPath + "'...");

            Directory dir = FSDirectory.open(Paths.get(indexPath));
            Analyzer analyzer = new EnglishAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(OpenMode.CREATE);
            IndexWriter writer = new IndexWriter(dir, iwc);
            indexDocs(writer, docDir);
            writer.close();
            SearchFiles search = new SearchFiles();
            try {
                search.Search(index_in, scoring, queries_in, args_in);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }

    }

    static void indexDocs(final IndexWriter writer, Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        indexDoc(writer, file);
                    } catch (IOException ignore) {
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            indexDoc(writer, path);
        }
    }

    static void indexDoc(IndexWriter writer, Path file) throws IOException {
        try (InputStream stream = Files.newInputStream(file)) {

            BufferedReader inputReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String currentLine = inputReader.readLine();
            Document doc;
            String type = "";

            while (currentLine != null) {
                if (currentLine.startsWith(".I")) {
                    doc = new Document();
                    Field pathField = new StringField("path", currentLine, Field.Store.YES);
                    doc.add(pathField);
                    currentLine = inputReader.readLine();
                    while (!(currentLine.startsWith(".I"))) {
                        if (currentLine.startsWith(".T")) {
                            type = "Title";
                            currentLine = inputReader.readLine();
                        } else if (currentLine.startsWith(".A")) {
                            type = "Author";
                            currentLine = inputReader.readLine();
                        } else if (currentLine.startsWith(".W")) {
                            type = "Words";
                            currentLine = inputReader.readLine();
                        } else if (currentLine.startsWith(".B")) {
                            type = "Bib";
                            currentLine = inputReader.readLine();
                        }
                        doc.add(new TextField(type, currentLine, Field.Store.YES));
                        currentLine = inputReader.readLine();
                        if (currentLine == null) {
                            break;
                        }
                    }
                    writer.addDocument(doc);

                }
            }
        }
    }
}
