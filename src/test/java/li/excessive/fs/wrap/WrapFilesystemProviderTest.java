package li.excessive.fs.wrap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import li.excessive.fs.Env;



public class WrapFilesystemProviderTest {

	@Rule
	public Env env = new Env();

    
	private static Logger log = LoggerFactory.getLogger(WrapFilesystemProviderTest.class);
	
	@Test
	public void testGetPath() throws URISyntaxException, IOException {
		Path f = env.resource("IXUS40/IMG_3197.JPG");
		
		Path p = Paths.get(new URI("wrap:" + f.toString()));
		
		log.info(p.toString());
		assertNotNull(p);
		assertTrue(p instanceof WrapPath);
	}
	
	@Test
	public void testGetPathZip() throws URISyntaxException, IOException {
        URI uri = new URI("wrap:jar:" + env.resource("EOS70.zip").toUri());
		try(FileSystem dscZipFs = FileSystems.newFileSystem(uri, new HashMap<String, String>())) {
			// access through file system fs.getPath(...)			
			Path p = dscZipFs.getPath("EOS70/IMG_1257.JPG");			
			log.info(p.toString());
			assertTrue(p instanceof WrapPath);
			
			// access through Paths.get(dsc:jar:...)
	        uri = URI.create("wrap:jar:" + env.resource("EOS70.zip!/EOS70/IMG_1257.JPG").toUri());
	        p = Paths.get(uri);
			log.info(p.toString());
			assertTrue(p instanceof WrapPath);
		}
	}
	
	@Test
	public void testCopyFile() throws URISyntaxException, IOException, InterruptedException {
        URI uri = new URI("wrap:jar:" + env.resource("EOS70.zip").toUri());
		try(FileSystem dscZipFs = FileSystems.newFileSystem(uri, new HashMap<String, String>())) {
			// access through file system fs.getPath(...)			
			Path p = dscZipFs.getPath("EOS70/IMG_1257.JPG");			
			log.info(p.toString());
			assertTrue(p instanceof WrapPath);
			
//			Path target = env.output(p.getFileName().toString());
//			Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
//			assertTrue(Files.exists(target));			
		}
	}


}
