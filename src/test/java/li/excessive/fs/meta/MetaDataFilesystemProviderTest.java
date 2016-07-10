package li.excessive.fs.meta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import li.excessive.fs.Env;



public class MetaDataFilesystemProviderTest {

	@Rule
	public Env env = new Env();
	
	private static Logger log = LoggerFactory.getLogger(MetaDataFilesystemProviderTest.class);
	
	@Test
	public void testGetPath() throws URISyntaxException, IOException {
		URI uri = new URI("meta:" + env.resource("IXUS40/IMG_3197.JPG"));		
		Path p = Paths.get(uri);
		log.info(p.toString());
		assertNotNull(p);
		assertTrue(p instanceof MetaDataPath);
	}
	
	@Test
	public void testGetPathZip() throws URISyntaxException, IOException {
        URI uri = new URI("meta:jar:" + env.resource("EOS70.zip").toUri());
        
		try(FileSystem dscZipFs = FileSystems.newFileSystem(uri, new HashMap<String, String>())) {
			// access through file system fs.getPath(...)
			Path p = dscZipFs.getPath("EOS70/IMG_1257.JPG");
			log.info(p.toString());
			assertTrue(p instanceof MetaDataPath);
			
			// access through Paths.get(dsc:jar:...)
	        uri = URI.create("meta:jar:" + env.resource("EOS70.zip!/EOS70/IMG_1257.JPG").toUri());
	        p = Paths.get(uri);
			log.info(p.toString());
			assertTrue(p instanceof MetaDataPath);
		}
	}

	@Test
	public void testReadableMetaDataAttributeViewJpg() throws URISyntaxException, IOException {
		URI dscUri = new URI("meta:" + env.resource("IXUS40/IMG_3197.JPG"));
		Path p = Paths.get(dscUri);
		MetaDataView v = Files.getFileAttributeView(p, MetaDataView.class);
		assertNotNull(v);
		assertEquals("6a4363a17fb60dbed894f107a460bfd5", v.getMd5());
		assertEquals("79e7650c497eb5ab556ecb4ca6dfa920806a1b0b", v.getSha1());
		log.info("meta data\n" + v);
	}
	
	@Test
	public void testReadableMetaDataAttributeViewJpg2() throws URISyntaxException, IOException {
		URI dscUri = new URI("meta:" + env.resource("EOS70/IMG_1257.JPG"));
		Path p = Paths.get(dscUri);
		MetaDataView v = Files.getFileAttributeView(p, MetaDataView.class);
		assertNotNull(v);
		assertEquals("2014-04-26T17:32:18.780", v.getDateTimeCreated().toString());
	}
	
	@Test
	public void testReadableMetaDataAttributeViewThm() throws URISyntaxException, IOException {
		URI dscUri = new URI("meta:" + env.resource("IXUS40/MVI_3198.THM"));
		Path p = Paths.get(dscUri);
		MetaDataView v = Files.getFileAttributeView(p, MetaDataView.class);
		assertNotNull(v);
		assertEquals("image/jpeg", v.getMimeType());
	}
	
	@Test
	public void testReadableMetaDataAttributeViewAvi() throws URISyntaxException, IOException {
		URI dscUri = new URI("meta:" + env.resource("IXUS40/MVI_3198.AVI"));
		Path p = Paths.get(dscUri);
		MetaDataView v = Files.getFileAttributeView(p, MetaDataView.class);
		assertNotNull(v);
		log.info("meta data\n" + v);
		assertEquals("video/x-msvideo", v.getMimeType());
		assertEquals("2011-08-14T09:35:52", v.getDateTimeCreated().toString());
	}
	


}
