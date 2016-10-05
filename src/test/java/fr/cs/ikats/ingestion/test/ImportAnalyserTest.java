package fr.cs.ikats.ingestion.test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import javax.ejb.embeddable.EJBContainer;
import javax.naming.NamingException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cs.ikats.ingestion.api.ImportSessionDto;
import fr.cs.ikats.ingestion.model.ImportSession;
import fr.cs.ikats.ingestion.model.ImportStatus;
import fr.cs.ikats.ingestion.process.ImportAnalyser;
import fr.cs.ikats.ingestion.process.IngestionProcess;

public class ImportAnalyserTest {

	/** Example of Regex for the "pathPattern" in the case of EDF Dataset */
	public final static String EDF_PATH_PATTERN = "\\/DAR\\/(?<equipement>\\w*)\\/(?<metric>.*?)(?:_(?<good>bad|good))?\\.csv";
	
	/***
     * Méthode d'initialisation appelée une seule fois lors de l'exécution
     * des tests de HelloServiceTest.
     * C'est l'endroit idéal pour démarrer l'EJBContainer et récupérer
     * les EJB à tester.
     * @throws NamingException
     */
    @BeforeClass
    public static void init() throws NamingException {
        EJBContainer.createEJBContainer();
    }
	
	@Test
	public void testAnalyserNominal() throws URISyntaxException, InterruptedException, IOException {
		
		ImportSessionDto simple = new ImportSessionDto();
		simple.dataset = "DS_FAKE_EDF";
		simple.pathPattern = EDF_PATH_PATTERN;
		Path rootPath = Paths.get(getClass().getResource("/" + simple.dataset).toURI());
		simple.rootPath = rootPath.toAbsolutePath().toString();
		ImportSession importSession = new ImportSession(simple);
		
		// -- Get number of files
		int nbMatchingFiles = countFilesMatching(rootPath, EDF_PATH_PATTERN);
		
		// -- Creates the ingestion process that do not run, only to pass it as an argument
		// to the analyser !
		IngestionProcess ingestionProcess = new IngestionProcess(importSession, null, null);
		ImportAnalyser importAnalyser = new ImportAnalyser(ingestionProcess, importSession);
		
		// Start the analyser thread and wait until it to finish
		Thread analyser = new Thread(importAnalyser);
		analyser.start();
		analyser.join();
		
		// Result 1: the session had to be analysed
		Assert.assertTrue(importSession.getStatus() == ImportStatus.ANALYSED);
		Assert.assertTrue(importSession.getItemsToImport().size() == nbMatchingFiles);
	}

	
	private int countFilesMatching(Path rootPath, String pathPattern) throws IOException {
		
		int count = 0;
		Pattern pattern = Pattern.compile(pathPattern);
		int rootPathEndIndex = rootPath.toFile().toString().length();

		count = (int) Files.walk(rootPath)
			// select only the files
			.filter( path -> path.toFile().isFile() )
			// select the files that fully matches the regex
			.filter( file -> {
				// firstly: get relative path substring 
				File relativePath = new File(file.toString().substring(rootPathEndIndex));
				return pattern.matcher(relativePath.toString()).matches(); 
			})
			// count the results
			.count();
		
		return count;
	}

}